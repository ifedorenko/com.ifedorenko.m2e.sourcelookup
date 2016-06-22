/*******************************************************************************
 * Copyright (c) 2012 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/
package com.ifedorenko.m2e.sourcelookup.internal.jdt;

import static org.eclipse.jdt.core.IJavaElementDelta.F_CLASSPATH_CHANGED;
import static org.eclipse.jdt.core.IJavaElementDelta.F_CLOSED;
import static org.eclipse.jdt.core.IJavaElementDelta.F_OPENED;
import static org.eclipse.jdt.core.IJavaElementDelta.F_RESOLVED_CLASSPATH_CHANGED;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.sourcelookup.containers.PackageFragmentRootSourceContainer;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.ifedorenko.m2e.sourcelookup.internal.jdt.AbstractProjectSourceDescriber.IJavaProjectSourceDescription;
import com.ifedorenko.m2e.sourcelookup.internal.jdt.AbstractProjectSourceDescriber.ISourceContainerFactory;

public class WorkspaceProjects {
  private final IElementChangedListener changeListener = new IElementChangedListener() {
    @Override
    public void elementChanged(ElementChangedEvent event) {
      try {
        final Set<IJavaProject> remove = new HashSet<IJavaProject>();
        final Set<IJavaProject> add = new HashSet<IJavaProject>();

        processDelta(event.getDelta(), remove, add);

        for (IJavaProject project : remove) {
          removeJavaProject(project);
        }
        List<AbstractProjectSourceDescriber> describers = getJavaProjectDescribers();
        for (IJavaProject project : add) {
          addJavaProject(project, describers);
        }
      } catch (CoreException e) {
        // maybe do something about it?
      }
    }

    private void processDelta(final IJavaElementDelta delta, Set<IJavaProject> remove, Set<IJavaProject> add)
        throws CoreException {
      // TODO review, this looks too complicated to add/remove java projects

      final IJavaElement element = delta.getElement();
      final int kind = delta.getKind();
      switch (element.getElementType()) {
        case IJavaElement.JAVA_MODEL:
          processChangedChildren(delta, remove, add);
          break;
        case IJavaElement.JAVA_PROJECT:
          switch (kind) {
            case IJavaElementDelta.REMOVED:
              remove.add((IJavaProject) element);
              break;
            case IJavaElementDelta.ADDED:
              add.add((IJavaProject) element);
              break;
            case IJavaElementDelta.CHANGED:
              if ((delta.getFlags() & F_CLOSED) != 0) {
                remove.add((IJavaProject) element);
              } else if ((delta.getFlags() & F_OPENED) != 0) {
                add.add((IJavaProject) element);
              } else if ((delta.getFlags() & (F_CLASSPATH_CHANGED | F_RESOLVED_CLASSPATH_CHANGED)) != 0) {
                remove.add((IJavaProject) element);
                add.add((IJavaProject) element);
              }
              break;
          }
          processChangedChildren(delta, remove, add);
          break;
        case IJavaElement.PACKAGE_FRAGMENT_ROOT:
          remove.add(element.getJavaProject());
          add.add(element.getJavaProject());
          break;
      }
    }

    private void processChangedChildren(IJavaElementDelta delta, Set<IJavaProject> remove, Set<IJavaProject> add)
        throws CoreException {
      for (IJavaElementDelta childDelta : delta.getAffectedChildren()) {
        processDelta(childDelta, remove, add);
      }
    }
  };

  private static class JavaProjectDescription implements IJavaProjectSourceDescription {
    final Set<File> locations;

    final Set<Object> hashes;

    final List<ISourceContainerFactory> factories;

    final Map<File, IPackageFragmentRoot> dependencyLocations;

    final Map<Object, IPackageFragmentRoot> dependencyHashes;

    public JavaProjectDescription() {
      locations = new HashSet<>();
      hashes = new HashSet<>();
      factories = new ArrayList<>();
      dependencyLocations = new HashMap<>();
      dependencyHashes = new HashMap<>();
    }

    private JavaProjectDescription(JavaProjectDescription original) {
      locations = ImmutableSet.copyOf(original.locations);
      hashes = ImmutableSet.copyOf(original.hashes);
      factories = ImmutableList.copyOf(original.factories);
      dependencyLocations = ImmutableMap.copyOf(original.dependencyLocations);
      dependencyHashes = ImmutableMap.copyOf(original.dependencyHashes);
    }

    @Override
    public void addLocation(File location) {
      this.locations.add(location);

      Object hash = Locations.hash(location);
      if (hash != null) {
        this.hashes.add(hash);
      }
    }

    @Override
    public void addLocations(Collection<File> locations) {
      for (File location : locations) {
        addLocation(location);
      }
    }

    @Override
    public void addSourceContainerFactory(ISourceContainerFactory factory) {
      this.factories.add(factory);
    }

    @Override
    public void addDependencies(Map<File, IPackageFragmentRoot> dependencies) {
      this.dependencyLocations.putAll(dependencies);
      this.dependencyHashes.putAll(Locations.hash(dependencies));
    }

    public JavaProjectDescription immutableCopy() {
      return new JavaProjectDescription(this);
    }
  }

  /**
   * Guards concurrent access to #locations and #projects. Necessary because java model change events can be delivered
   * while #locations and #projects are being initialized.
   */
  private final Object lock = new Object() {};

  // TODO hash-based lookup, useful when workspace and runtime use different copies of the same dependency
  // happens when run-as-maven-build and workspace use different local repositories, for example

  /**
   * Maps project output location to project info.
   */
  private final Map<File, JavaProjectDescription> locations = new HashMap<>();

  private final Multimap<Object, JavaProjectDescription> hashes = HashMultimap.create();

  /**
   * Maps java project to project output locations.
   */
  private final Map<IJavaProject, JavaProjectDescription> projects = new HashMap<>();

  private static final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

  public ISourceContainer getProjectContainer(File projectLocation) {
    JavaProjectDescription description = getProjectByLocation(projectLocation);

    if (description == null) {
      Collection<JavaProjectDescription> desciptions = getProjectsByHash(projectLocation);
      if (!desciptions.isEmpty()) {
        // it is totally possible to have multiple binary projects for the same jar
        description = desciptions.iterator().next();
      }
    }

    if (description == null) {
      return null;
    }

    List<ISourceContainer> containers = new ArrayList<>();
    for (ISourceContainerFactory factory : description.factories) {
      containers.add(factory.createContainer());
    }

    return CompositeSourceContainer.compose(containers);
  }

  private JavaProjectDescription getProjectByLocation(File projectLocation) {
    synchronized (lock) {
      return locations.get(projectLocation);
    }
  }

  private Collection<JavaProjectDescription> getProjectsByHash(File projectLocation) {
    synchronized (lock) {
      return ImmutableList.copyOf(hashes.get(Locations.hash(projectLocation)));
    }
  }

  public ISourceContainer getClasspathEntryContainer(File projectLocation, File entryLocation) {
    JavaProjectDescription projectByLocation = getProjectByLocation(projectLocation);

    IPackageFragmentRoot dependency = getProjectDependency(projectByLocation, entryLocation);

    if (dependency == null && projectByLocation == null) {
      for (JavaProjectDescription projectByHash : getProjectsByHash(projectLocation)) {
        dependency = getProjectDependency(projectByHash, entryLocation);
        if (dependency != null) {
          break;
        }
      }
    }

    if (dependency == null) {
      return null;
    }

    return new PackageFragmentRootSourceContainer(dependency);
  }

  protected IPackageFragmentRoot getProjectDependency(JavaProjectDescription project, File entryLocation) {
    if (project == null) {
      return null;
    }

    IPackageFragmentRoot dependency = project.dependencyLocations.get(entryLocation);

    if (dependency == null) {
      dependency = project.dependencyHashes.get(Locations.hash(entryLocation));
    }
    return dependency;
  }

  public void initialize(IProgressMonitor monitor) throws CoreException {
    JavaCore.addElementChangedListener(changeListener);

    final IJavaModel javaModel = JavaCore.create(root);
    final IJavaProject[] javaProjects = javaModel.getJavaProjects();

    // TODO possible race when java model change events are delivered while initialization is in progress

    List<AbstractProjectSourceDescriber> describers = getJavaProjectDescribers();
    for (IJavaProject project : javaProjects) {
      addJavaProject(project, describers);
    }
  }

  public void close() {
    JavaCore.removeElementChangedListener(changeListener);
  }

  private void addJavaProject(IJavaProject project, List<AbstractProjectSourceDescriber> describers)
      throws CoreException {
    if (project == null) {
      return;
    }

    JavaProjectDescription info = new JavaProjectDescription();

    for (AbstractProjectSourceDescriber describer : describers) {
      describer.describeProject(project, info);
    }

    info = info.immutableCopy();

    synchronized (lock) {
      for (File location : info.locations) {
        locations.put(location, info);
      }
      for (Object hash : info.hashes) {
        hashes.put(hash, info);
      }
      projects.put(project, info);
    }
  }

  protected List<AbstractProjectSourceDescriber> getJavaProjectDescribers() {
    List<AbstractProjectSourceDescriber> result = new ArrayList<>();

    IExtensionRegistry registry = Platform.getExtensionRegistry();

    IConfigurationElement[] elements =
        registry.getConfigurationElementsFor("com.ifedorenko.m2e.sourcelookup.projectSourceDescribers");

    for (IConfigurationElement element : elements) {
      if ("describer".equals(element.getName())) {
        try {
          result.add((AbstractProjectSourceDescriber) element.createExecutableExtension("class"));
        } catch (CoreException e) {}
      }
    }

    result.add(new DefaultProjectDescriber());

    return result;
  }

  private void removeJavaProject(IJavaProject project) {
    if (project != null) {
      synchronized (lock) {
        JavaProjectDescription description = projects.remove(project);
        if (description != null) {
          for (File location : description.locations) {
            locations.remove(location);
          }
          for (Object hash : description.hashes) {
            hashes.remove(hash, description);
          }
        }
      }
    }
  }

}

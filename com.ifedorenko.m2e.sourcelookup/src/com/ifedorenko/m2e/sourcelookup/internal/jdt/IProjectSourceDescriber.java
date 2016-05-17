/*******************************************************************************
 * Copyright (c) 2011-2016 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/
package com.ifedorenko.m2e.sourcelookup.internal.jdt;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

public abstract class IProjectSourceDescriber {
  // need three pieces of information about a java project
  // 1. what filesystem classes directories or jar files identify the project
  // 2. factory of source container(s) for the project itself
  // 3. project dependencies and their corresponding source container(s) factories

  // #1 are classes directories for plain java runtimes and bundle installation directory for Equinox
  // this means the same project will have both plain java and equinox locations
  // for example, tycho build loads some OSGi bundles as plain java projects

  // likewise, dependencies can be identified either by classes directories or bundle installation locations
  // each dependency must have corresponding IPackageFragmentRoot, which provides "java project context"

  public static interface ISourceContainerFactory {
    public Collection<ISourceContainer> getContainers();
  }

  public static interface IJavaProjectSourceDescription {
    public void addLocation(File location);

    public void addLocations(Collection<File> locations);

    public void addSourceContainerFactory(ISourceContainerFactory factory);

    public void addDependencies(Map<File, IPackageFragmentRoot> dependencies);
  }

  public abstract void describeProject(IJavaProject project, IJavaProjectSourceDescription description)
      throws CoreException;

  protected static final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

  protected static Map<File, IPackageFragmentRoot> getClasspath(IJavaProject project) throws JavaModelException {
    final Map<File, IPackageFragmentRoot> classpath = new LinkedHashMap<>();
    for (IPackageFragmentRoot fragment : project.getPackageFragmentRoots()) {
      if (fragment.getKind() == IPackageFragmentRoot.K_BINARY) {
        File classpathLocation;
        if (fragment.isExternal()) {
          classpathLocation = fragment.getPath().toFile();
        } else {
          classpathLocation = fragment.getResource().getLocation().toFile();
        }
        if (classpathLocation != null) {
          classpath.put(classpathLocation, fragment);
        }
      }
    }
    return classpath;
  }

  protected static Set<File> getOutputDirectories(IJavaProject project) throws JavaModelException {
    final Set<File> locations = new HashSet<>();
    addWorkspaceLocation(locations, project.getOutputLocation());
    for (IClasspathEntry cpe : project.getRawClasspath()) {
      if (cpe.getEntryKind() == IClasspathEntry.CPE_SOURCE && cpe.getOutputLocation() != null) {
        addWorkspaceLocation(locations, cpe.getOutputLocation());
      }
    }
    return locations;
  }

  private static void addWorkspaceLocation(Collection<File> locations, IPath workspacePath) {
    IResource resource = root.findMember(workspacePath);
    if (resource != null) {
      locations.add(resource.getLocation().toFile());
    }
  }

}

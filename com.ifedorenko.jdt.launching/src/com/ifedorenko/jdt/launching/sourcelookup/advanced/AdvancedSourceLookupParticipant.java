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
package com.ifedorenko.jdt.launching.sourcelookup.advanced;

import static com.ifedorenko.jdt.internal.launching.sourcelookup.advanced.AdvancedSourceLookupSupport.getContextMonitor;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;

import com.ifedorenko.jdt.internal.launching.sourcelookup.advanced.AdvancedSourceLookupSupport;
import com.ifedorenko.jdt.internal.launching.sourcelookup.advanced.CompositeSourceContainer;
import com.ifedorenko.jdt.internal.launching.sourcelookup.advanced.IJDIHelpers;
import com.ifedorenko.jdt.internal.launching.sourcelookup.advanced.WorkspaceProjectSourceContainers;
import com.ifedorenko.jdt.launching.internal.AdvancedSourceLookupActivator;

/**
 * @since 3.9
 * @provisional This is part of work in progress and can be changed, moved or removed without notice
 */
public class AdvancedSourceLookupParticipant implements ISourceLookupParticipant {

  private final IJDIHelpers jdi;

  private ISourceLookupDirector director;

  private final Map<File, ISourceContainer> containers = new HashMap<>();

  public AdvancedSourceLookupParticipant() {
    this(IJDIHelpers.INSTANCE);
  }

  /**
   * @noreference this constructor is visible for test purposes only
   */
  public AdvancedSourceLookupParticipant(IJDIHelpers jdi) {
    this.jdi = jdi;
  }

  @Override
  public void init(ISourceLookupDirector director) {
    this.director = director;
  }

  @Override
  public Object[] findSourceElements(Object element) throws CoreException {
    ISourceContainer container = getSourceContainer(element, false /* don't refresh cache */, null /* async */ );

    if (container == null) {
      return null;
    }

    String sourcePath = jdi.getSourcePath(element);
    if (sourcePath == null) {
      // can't really happen
      return null;
    }

    return container.findSourceElements(sourcePath);
  }

  public ISourceContainer getSourceContainer(Object element, boolean refresh, IProgressMonitor monitor)
      throws CoreException {
    File location = jdi.getClassesLocation(element);

    if (location == null) {
      return null;
    }

    synchronized (containers) {
      if (!refresh && containers.containsKey(location)) {
        return containers.get(location);
      }
    }

    monitor = getContextMonitor(monitor);

    WorkspaceProjectSourceContainers projectLocator = AdvancedSourceLookupSupport.getWorkspaceJavaProjects(monitor);
    if (monitor == null && projectLocator == null) {
      // reschedule to initialize and resolve sources in background
      AdvancedSourceLookupSupport.schedule((m) -> getSourceContainer(element, refresh, m));
      return null;
    }

    if (projectLocator == null) {
      // we get here when projectLocator initialization has been cancelled by the user
      return null;
    }

    //
    // lookup strategies that provide java project context necessary for debug expression evaluation
    //

    // workspace project identified by their runtime classes location is the preferred sources container
    ISourceContainer projectContainer = projectLocator.createProjectContainer(location);
    if (projectContainer != null) {
      return cacheContainer(element, location, projectContainer);
    }

    // dependency of one of workspace projects on the call stack also provides java project context
    for (File frameLocation : jdi.getStackFramesClassesLocations(element)) {
      ISourceContainer entryContainer = projectLocator.createClasspathEntryContainer(frameLocation, location);
      if (entryContainer != null) {
        return cacheContainer(element, location, entryContainer);
      }
    }

    if (monitor == null) {
      // reschedule to resolve sources in background
      AdvancedSourceLookupSupport.schedule((m) -> getSourceContainer(element, refresh, m));
      return null;
    }

    //
    // strategies that allow source code lookup but do not provide java project context
    //

    // checksum-based lookup in various sources (central, nexus, pde target platform, p2 repositories)
    for (ISourceContainerResolver repository : getSourceContainerResolvers()) {
      Collection<ISourceContainer> members = repository.resolveSourceContainers(location, monitor);
      if (members != null && !members.isEmpty()) {
        return cacheContainer(element, location, CompositeSourceContainer.compose(members));
      }
    }

    return null;
  }

  private ISourceContainer cacheContainer(Object element, File location, ISourceContainer container) {
    ISourceContainer oldContainer;
    synchronized (containers) {
      oldContainer = containers.put(location, container);
      if (oldContainer != null) {
        oldContainer.dispose();
      }
    }
    if (oldContainer != null || container != null) {
      updateDebugElement(element);
    }
    return container;
  }

  protected Collection<ISourceContainerResolver> getSourceContainerResolvers() {
    List<ISourceContainerResolver> result = new ArrayList<>();

    IExtensionRegistry registry = Platform.getExtensionRegistry();

    IConfigurationElement[] elements =
        registry.getConfigurationElementsFor(AdvancedSourceLookupActivator.ID_sourceContainerResolvers);

    for (IConfigurationElement element : elements) {
      if ("resolver".equals(element.getName())) { //$NON-NLS-1$
        try {
          result.add((ISourceContainerResolver) element.createExecutableExtension("class")); //$NON-NLS-1$
        } catch (CoreException e) {}
      }
    }

    return result;
  }

  @Override
  public String getSourceName(Object object) throws CoreException {
    return null; // default name->source mapping
  }

  @Override
  public void dispose() {
    disposeContainers();
  }

  @Override
  public void sourceContainersChanged(ISourceLookupDirector director) {
    disposeContainers();
  }

  protected void disposeContainers() {
    synchronized (containers) {
      for (ISourceContainer container : containers.values()) {
        if (container != null) // possible for non-maven jars
        {
          container.dispose();
        }
      }
      containers.clear();
    }
  }

  private void updateDebugElement(Object element) {
    director.clearSourceElements(element);
    if (element instanceof DebugElement) {
      // this is apparently needed to flush StackFrameSourceDisplayAdapter cache
      ((DebugElement) element).fireChangeEvent(DebugEvent.CONTENT);
    }
  }

  public static AdvancedSourceLookupParticipant getSourceLookup(Object element) {
    ISourceLocator sourceLocator = null;
    if (element instanceof IDebugElement) {
      sourceLocator = ((IDebugElement) element).getLaunch().getSourceLocator();
    }

    AdvancedSourceLookupParticipant sourceLookup = null;
    if (sourceLocator instanceof ISourceLookupDirector) {
      for (ISourceLookupParticipant participant : ((ISourceLookupDirector) sourceLocator).getParticipants()) {
        if (participant instanceof AdvancedSourceLookupParticipant) {
          sourceLookup = (AdvancedSourceLookupParticipant) participant;
          break;
        }
      }
    }
    return sourceLookup;
  }

}

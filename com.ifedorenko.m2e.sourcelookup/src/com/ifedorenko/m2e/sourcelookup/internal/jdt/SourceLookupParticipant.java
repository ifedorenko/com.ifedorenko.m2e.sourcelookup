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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;
import org.eclipse.debug.ui.DebugUITools;

import com.ifedorenko.m2e.sourcelookup.internal.SourceLookupActivator;
import com.ifedorenko.m2e.sourcelookup.internal.jdi.JDIHelpers;
import com.ifedorenko.m2e.sourcelookup.internal.launch.MavenSourceContainerResolver;

public class SourceLookupParticipant implements ISourceLookupParticipant {

  private ISourceLookupDirector director;

  private final Map<File, ISourceContainer> containers = new HashMap<>();

  @Override
  public void init(ISourceLookupDirector director) {
    this.director = director;
  }

  @Override
  public Object[] findSourceElements(Object fElement) throws CoreException {
    ISourceContainer container = getSourceContainer(fElement, false /* don't refresh cache */, null /* async */ );

    if (container == null) {
      return null;
    }

    String sourcePath = JDIHelpers.getSourcePath(fElement);
    if (sourcePath == null) {
      // can't really happen
      return null;
    }

    return container.findSourceElements(sourcePath);
  }

  public ISourceContainer getSourceContainer(Object fElement, boolean refresh, IProgressMonitor monitor)
      throws CoreException {
    File location = JDIHelpers.getLocation(fElement);

    if (location == null) {
      return null;
    }

    synchronized (containers) {
      if (!refresh && containers.containsKey(location)) {
        return containers.get(location);
      }
    }

    WorkspaceProjects projectLocator = SourceLookupActivator.getWorkspaceJavaProjects(monitor);
    if (monitor == null && projectLocator == null) {
      // reschedule to initialize and resolve sources in background
      SourceLookupActivator.schedule((m) -> getSourceContainer(fElement, refresh, m));
      return null;
    }

    if (projectLocator == null) {
      throw new IllegalStateException();
    }

    //
    // lookup strategies that provide java project context necessary for debug expression evaluation
    //

    // workspace project output folder is the preferred sources container
    ISourceContainer projectContainer = projectLocator.getProjectContainer(location);
    if (projectContainer != null) {
      return cacheContainer(fElement, location, projectContainer);
    }

    // dependency of one of workspace projects on the call also provides java project context
    // TODO in case of PDE, dependency may still need sources bundle discovery and download
    IStackFrame[] stackFrames = getStackFrames(fElement);
    if (stackFrames != null) {
      for (IStackFrame frame : stackFrames) {
        File frameLocation = JDIHelpers.getLocation(frame);
        if (frameLocation == null) {
          continue;
        }

        ISourceContainer entryContainer = projectLocator.getClasspathEntryContainer(frameLocation, location);
        if (entryContainer != null) {
          return cacheContainer(fElement, location, entryContainer);
        }
      }
    }

    // TODO hash-based lookup among dependencies of projects on the stack
    // but need a scenario when this is useful first, can't think of one right now

    if (monitor == null) {
      // reschedule to resolve sources in background
      SourceLookupActivator.schedule((m) -> getSourceContainer(fElement, refresh, m));
      return null;
    }

    //
    // strategies that allow source code lookup but do not provide java project context
    //

    // checksum-based lookup in various sources (central, nexus, pde target platform, p2 repositories)
    for (ISourceContainerResolver repository : getSourceContainerResolvers()) {
      Collection<ISourceContainer> members = repository.resolveSourceContainers(location, monitor);
      if (members != null && !members.isEmpty()) {
        return cacheContainer(fElement, location, CompositeSourceContainer.create(members));
      }
    }

    return null;
  }

  private ISourceContainer cacheContainer(Object fElement, File location, ISourceContainer container) {
    ISourceContainer oldContainer;
    synchronized (containers) {
      oldContainer = containers.put(location, container);
      if (oldContainer != null) {
        oldContainer.dispose();
      }
    }
    if (oldContainer != null || container != null) {
      updateDebugElement(fElement);
    }
    return container;
  }

  protected Collection<ISourceContainerResolver> getSourceContainerResolvers() {
    // TODO proper extension point
    return Collections.<ISourceContainerResolver>singleton(new MavenSourceContainerResolver());
  }

  private IStackFrame[] getStackFrames(Object element) throws DebugException {
    IStackFrame frame = null;
    if (element instanceof IStackFrame) {
      frame = (IStackFrame) element;
    }
    if (frame == null) {
      // not sure how useful this is
      // it makes variable type lookup more precise when the same dependency is referenced from multiple projects
      // the same dependency will be found by scanning all workspace projects, albeit in that case the same
      // "correct" dependency may come from "wrong" project.
      // this logic also introduces dependency on debug.ui, the only ui dependency of this bundle.
      // ui dependency is not a big deal, but the gain is not great either.
      // ... and this is really long comment to justify so little code with so little gain :-)
      frame = (IStackFrame) DebugUITools.getDebugContext().getAdapter(IStackFrame.class);
    }
    if (frame != null) {
      IStackFrame[] frames = frame.getThread().getStackFrames();
      for (int i = 0; i < frames.length - 1; i++) {
        if (frames[i] == frame) {
          IStackFrame[] stack = new IStackFrame[frames.length - i - 1];
          System.arraycopy(frames, i + 1, stack, 0, frames.length - i - 1);
          return stack;
        }
      }
    }
    return null;
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

  private void updateDebugElement(Object debugElement) {
    director.clearSourceElements(debugElement);
    if (debugElement instanceof DebugElement) {
      // this is apparently needed to flush StackFrameSourceDisplayAdapter cache
      ((DebugElement) debugElement).fireChangeEvent(DebugEvent.CONTENT);
    }
  }

  public static SourceLookupParticipant getSourceLookup(Object debugElement) {
    ISourceLocator sourceLocator = null;
    if (debugElement instanceof IDebugElement) {
      sourceLocator = ((IDebugElement) debugElement).getLaunch().getSourceLocator();
    }

    SourceLookupParticipant sourceLookup = null;
    if (sourceLocator instanceof ISourceLookupDirector) {
      for (ISourceLookupParticipant participant : ((ISourceLookupDirector) sourceLocator).getParticipants()) {
        if (participant instanceof SourceLookupParticipant) {
          sourceLookup = (SourceLookupParticipant) participant;
          break;
        }
      }
    }
    return sourceLookup;
  }

}

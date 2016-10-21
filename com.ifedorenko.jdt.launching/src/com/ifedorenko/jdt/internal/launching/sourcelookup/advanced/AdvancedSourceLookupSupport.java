/*******************************************************************************
 * Copyright (c) 2015-2016 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/
package com.ifedorenko.jdt.internal.launching.sourcelookup.advanced;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.IPersistableSourceLocator;
import org.eclipse.debug.core.sourcelookup.IPersistableSourceLocator2;

import com.ifedorenko.jdt.launching.internal.AdvancedSourceLookupActivator;
import com.ifedorenko.jdt.launching.sourcelookup.advanced.AdvancedSourceLookupDirector;

public class AdvancedSourceLookupSupport {
  private static BackgroundProcessingJob backgroundJob;

  private static volatile WorkspaceProjectSourceContainers workspaceProjects;
  private static final Lock workspaceProjectsLock = new ReentrantLock();

  private AdvancedSourceLookupSupport() {}

  public static void start() {
    backgroundJob = new BackgroundProcessingJob();
  }

  public static void stop() {
    backgroundJob.cancel();
    backgroundJob = null;

    workspaceProjectsLock.lock();
    try {
      if (workspaceProjects != null) {
        workspaceProjects.close();
        workspaceProjects = null;
      }
    } finally {
      workspaceProjectsLock.unlock();
    }
  }

  public static void schedule(IRunnableWithProgress task) {
    backgroundJob.schedule(task);
  }

  public static WorkspaceProjectSourceContainers getWorkspaceJavaProjects(IProgressMonitor monitor)
      throws CoreException {
    return getWorkspaceJavaProjects0(monitor);
  }

  private static WorkspaceProjectSourceContainers getWorkspaceJavaProjects0(IProgressMonitor monitor)
      throws CoreException {
    // this is convoluted, but I could not think of a simpler implementation

    // when monitor==null, we are most likely on UI thread and must not block, hence immediate return
    if (monitor == null || workspaceProjects != null) {
      return workspaceProjects;
    }

    // when monitor!=null, try to get the lock but check for cancellation periodically
    try {
      while (!workspaceProjectsLock.tryLock(500, TimeUnit.MILLISECONDS)) {
        if (monitor.isCanceled()) {
          return workspaceProjects;
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt(); // restore interrupted status
      return workspaceProjects;
    }

    // got the lock, do the initialization if another thread didn't do it already
    // note that double-check locking is okay on java 5+ with volatile fields
    try {
      if (workspaceProjects == null) {
        WorkspaceProjectSourceContainers _workspaceProjects = new WorkspaceProjectSourceContainers();
        _workspaceProjects.initialize(monitor);

        // assign only fully initialized instance, otherwise monitor==null branch above may misbehave
        workspaceProjects = _workspaceProjects;
      }
    } finally {
      // release the lock in finally{} block
      workspaceProjectsLock.unlock();
    }

    return workspaceProjects;
  }

  public static String getJavaagentString() {
    return "-javaagent:" + getJavaagentLocation(); //$NON-NLS-1$
  }

  public static String getJavaagentLocation() {
    return AdvancedSourceLookupActivator.getFileInPlugin(new Path("lib/javaagent-shaded.jar")).getAbsolutePath(); //$NON-NLS-1$
  }

  /**
   * Workaround a deficiency of ISourceLookupParticipant API, which does not provide access to a progress monitor.
   * 
   * <p>
   * This method can be called in three different cases:
   * <ol>
   * <li>from UI thread, in which case {@code null} is return. this tells the caller to only perform fast operations
   * (i.e. cache lookups) on this thread and submit any long-running operations as background jobs
   * <li>from background job with existing IProgressMonitor, in which case the monitor is returned
   * <li>from background job without IProgressMonitor, in which case {@link NullProgressMonitor} is returned.
   * </ol>
   */
  public static IProgressMonitor getContextMonitor(IProgressMonitor monitor) {
    if (monitor == null) {
      Job job = Job.getJobManager().currentJob();
      if (job != null) {
        // current implementation can perform workspace project cache initialization on system job without any user
        // feedback
        // although eclipse ui remains responsive, source lookup will appear to do nothing until initialization is
        // complete
        // a fix requires changes to ISourceLookupParticipant#findSourceElements API to accept user-visible progress
        // monitor
        monitor = new NullProgressMonitor();
      }
    }
    return monitor;
  }

  /**
   * Returns a launch object to use when launching the given launch configuration in the given mode. The returned launch
   * object is preconfigured to use {@link AdvancedSourceLookupDirector} as the source locator.
   */
  public static ILaunch createAdvancedLaunch(ILaunchConfiguration configuration, String mode) throws CoreException {
    return new Launch(configuration, mode, createSourceLocator(AdvancedSourceLookupDirector.ID, configuration));
  }

  /**
   * Creates and returns new {@link IPersistableSourceLocator} of the specified type and with the provided
   * configuration.
   */
  public static IPersistableSourceLocator createSourceLocator(String type, ILaunchConfiguration configuration)
      throws CoreException {
    ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();

    IPersistableSourceLocator locator = launchManager.newSourceLocator(type);
    String memento = configuration.getAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_MEMENTO, (String) null);
    if (memento == null) {
      locator.initializeDefaults(configuration);
    } else {
      if (locator instanceof IPersistableSourceLocator2) {
        ((IPersistableSourceLocator2) locator).initializeFromMemento(memento, configuration);
      } else {
        locator.initializeFromMemento(memento);
      }
    }

    return locator;
  }

  public static boolean isAdvancedSourcelookupEnabled() {
    // return Platform.getPreferencesService().getBoolean(JDIDebugPlugin.getUniqueIdentifier(),
    // JDIDebugPlugin.PREF_ENABLE_ADVANCED_SOURCELOOKUP, true, null);
    return true;
  }
}

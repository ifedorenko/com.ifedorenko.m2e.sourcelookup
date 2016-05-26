/*******************************************************************************
 * Copyright (c) 2011 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/
package com.ifedorenko.m2e.sourcelookup.internal;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;

import com.ifedorenko.m2e.sourcelookup.internal.jdt.WorkspaceProjects;

public class SourceLookupActivator extends Plugin {

  public static final String PLUGIN_ID = "com.ifedorenko.m2e.sourcelookup";

  private static SourceLookupActivator plugin;

  private BackgroundProcessingJob backgroundJob;

  private volatile WorkspaceProjects workspaceProjects;
  private final Lock workspaceProjectsLock = new ReentrantLock();

  public SourceLookupActivator() {}

  public void start(BundleContext context) throws Exception {
    super.start(context);

    plugin = this;

    backgroundJob = new BackgroundProcessingJob();
  }

  public void stop(BundleContext context) throws Exception {
    backgroundJob.cancel();
    backgroundJob = null;

    workspaceProjectsLock.lock();
    try {
      workspaceProjects.close();
      workspaceProjects = null;
    } finally {
      workspaceProjectsLock.unlock();
    }

    plugin = null;

    super.stop(context);
  }

  public static SourceLookupActivator getDefault() {
    return plugin;
  }

  public static void schedule(IRunnableWithProgress task) {
    getDefault().backgroundJob.schedule(task);
  }

  public static WorkspaceProjects getWorkspaceJavaProjects(IProgressMonitor monitor) throws CoreException {
    return getDefault().getWorkspaceJavaProjects0(monitor);
  }

  private WorkspaceProjects getWorkspaceJavaProjects0(IProgressMonitor monitor) throws CoreException {
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
        WorkspaceProjects _workspaceProjects = new WorkspaceProjects();
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

  public String getJavaagentString() throws CoreException {
    return "-javaagent:" + getJavaagentLocation();
  }

  public String getJavaagentLocation() throws CoreException {
    return toLocalFile(getBundle().getEntry("com.ifedorenko.m2e.sourcelookup.javaagent.jar"));
  }

  // TODO move to m2e Bundles
  public static String toLocalFile(URL entry) throws CoreException {
    try {
      return new File(FileLocator.toFileURL(entry).toURI()).getCanonicalPath();
    } catch (IOException | URISyntaxException e) {
      throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, e.getMessage(), e));
    }
  }
}

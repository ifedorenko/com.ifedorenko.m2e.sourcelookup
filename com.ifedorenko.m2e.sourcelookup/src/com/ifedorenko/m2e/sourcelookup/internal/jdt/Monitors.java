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

import java.lang.reflect.Method;

import org.eclipse.core.internal.jobs.Worker;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Workaround deficiency of ISourceLookupParticipant API, which does not provide access to a progress monitor. Uses
 * reflection to retrieve a monitor from the current thread.
 */
@SuppressWarnings("restriction")
class Monitors {

  private static final Method method_getProgressMonitor;

  static {
    Method method;
    try {
      method = Job.class.getSuperclass().getDeclaredMethod("getProgressMonitor");
      method.setAccessible(true);
    } catch (NoSuchMethodException | SecurityException e) {
      method = null;
    }
    method_getProgressMonitor = method;
  }

  public static IProgressMonitor getMonitor(IProgressMonitor monitor) {
    if (monitor == null) {
      Job job = currentJob();
      if (job != null) {
        monitor = getMonitor(job);
      }
    }
    return monitor;
  }

  private static Job currentJob() {
    Thread thread = Thread.currentThread();
    if (thread instanceof Worker) {
      return ((Worker) thread).currentJob();
    }
    return null;
  }

  private static IProgressMonitor getMonitor(Job job) {
    if (method_getProgressMonitor == null) {
      return null;
    }
    try {
      return (IProgressMonitor) method_getProgressMonitor.invoke(job);
    } catch (ReflectiveOperationException | IllegalArgumentException e) {
      return null;
    }
  }
}

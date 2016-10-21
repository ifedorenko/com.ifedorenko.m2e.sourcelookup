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
package com.ifedorenko.jdt.internal.launching.sourcelookup.advanced;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;

import com.ifedorenko.jdt.launching.internal.AdvancedSourceLookupActivator;

/**
 * Simple background request processing queue implemented using {@link Job} API. Requests are executed in the order they
 * arrive. Request execution delayed by {@value #EXECUTION_DELAY} milliseconds and all requests submitted during this
 * period will be processed together.
 */
public class BackgroundProcessingJob extends Job {
  private static final long EXECUTION_DELAY = 1000L;

  private final ArrayList<IRunnableWithProgress> queue = new ArrayList<>();

  public BackgroundProcessingJob() {
    super(Messages.BackgroundProcessingJob_name);
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    ArrayList<IRunnableWithProgress> tasks;
    synchronized (this.queue) {
      tasks = new ArrayList<>(this.queue);
      this.queue.clear();
    }

    SubMonitor progress = SubMonitor.convert(monitor, tasks.size());

    List<IStatus> errors = new ArrayList<>();

    for (IRunnableWithProgress task : tasks) {
      try {
        task.run(progress.split(1));
      } catch (CoreException e) {
        errors.add(e.getStatus());
      }
    }

    if (errors.isEmpty()) {
      return Status.OK_STATUS;
    }

    if (errors.size() == 1) {
      return errors.get(0);
    }

    return new MultiStatus(AdvancedSourceLookupActivator.ID_PLUGIN, IStatus.ERROR,
        errors.toArray(new IStatus[errors.size()]), Messages.BackgroundProcessingJob_failed, null);
  }

  public void schedule(IRunnableWithProgress task) {
    synchronized (queue) {
      queue.add(task);
      schedule(EXECUTION_DELAY);
    }
  }
}

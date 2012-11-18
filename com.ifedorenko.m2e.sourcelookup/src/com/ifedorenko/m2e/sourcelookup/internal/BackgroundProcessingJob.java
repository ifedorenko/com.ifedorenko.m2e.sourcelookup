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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

class BackgroundProcessingJob
    extends Job
{
    private final ArrayList<IRunnableWithProgress> queue = new ArrayList<IRunnableWithProgress>();

    public BackgroundProcessingJob()
    {
        super( "Background source download job" );
    }

    @Override
    protected IStatus run( IProgressMonitor monitor )
    {
        ArrayList<IRunnableWithProgress> tasks;
        synchronized ( this.queue )
        {
            tasks = new ArrayList<IRunnableWithProgress>( this.queue );
            this.queue.clear();
        }

        List<IStatus> errors = new ArrayList<IStatus>();

        for ( IRunnableWithProgress task : tasks )
        {
            try
            {
                task.run( monitor );
            }
            catch ( CoreException e )
            {
                errors.add( e.getStatus() );
            }
        }

        if ( errors.isEmpty() )
        {
            return Status.OK_STATUS;
        }

        if ( errors.size() == 1 )
        {
            return errors.get( 0 );
        }

        return new MultiStatus( SourceLookupActivator.PLUGIN_ID, IStatus.ERROR,
                                errors.toArray( new IStatus[errors.size()] ), "Background task failed", null );
    }

    public void schedule( IRunnableWithProgress task )
    {
        synchronized ( queue )
        {
            queue.add( task );
            schedule( 1000L );
        }
    }
}

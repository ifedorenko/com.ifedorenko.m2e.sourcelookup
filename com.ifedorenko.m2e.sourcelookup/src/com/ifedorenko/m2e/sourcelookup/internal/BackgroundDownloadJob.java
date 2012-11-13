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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.IMaven;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BackgroundDownloadJob
    extends Job
{
    private static final Logger log = LoggerFactory.getLogger( BackgroundDownloadJob.class );

    private static class QueueItem
    {
        public final ArtifactKey artifact;

        public final Runnable callback;

        public QueueItem( ArtifactKey artifact, Runnable callback )
        {
            this.artifact = artifact;
            this.callback = callback;
        }
    }

    private final ArrayList<QueueItem> queue = new ArrayList<QueueItem>();

    private final IMaven maven;

    public BackgroundDownloadJob()
    {
        super( "Background source download job" );
        this.maven = MavenPlugin.getMaven();
    }

    @Override
    protected IStatus run( IProgressMonitor monitor )
    {
        ArrayList<QueueItem> requests;
        synchronized ( this.queue )
        {
            requests = new ArrayList<QueueItem>( this.queue );
            this.queue.clear();
        }

        for ( QueueItem queueItem : requests )
        {
            ArtifactKey artifact = queueItem.artifact;
            try
            {
                maven.resolve( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), "jar",
                               artifact.getClassifier(), null, monitor );
                queueItem.callback.run();
            }
            catch ( CoreException e )
            {
                log.debug( "Could not download sources artifact {}", artifact, e );
            }
        }

        return Status.OK_STATUS;
    }

    public void schedule( ArtifactKey key, Runnable callback )
    {
        synchronized ( queue )
        {
            queue.add( new QueueItem( key, callback ) );
            schedule( 1000L );
        }
    }
}

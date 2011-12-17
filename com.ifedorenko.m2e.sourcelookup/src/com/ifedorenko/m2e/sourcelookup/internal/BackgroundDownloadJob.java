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

public class BackgroundDownloadJob
    extends Job
{
    private static final Logger log = LoggerFactory.getLogger( BackgroundDownloadJob.class );

    private final ArrayList<ArtifactKey> queue = new ArrayList<ArtifactKey>();

    private final IMaven maven;

    public BackgroundDownloadJob()
    {
        super( "Background source download job" );
        this.maven = MavenPlugin.getMaven();
    }

    @Override
    protected IStatus run( IProgressMonitor monitor )
    {
        ArrayList<ArtifactKey> requests;
        synchronized ( this.queue )
        {
            requests = new ArrayList<ArtifactKey>( this.queue );
            this.queue.clear();
        }

        for ( ArtifactKey key : requests )
        {
            try
            {
                maven.resolve( key.getGroupId(), key.getArtifactId(), key.getVersion(), "jar", key.getClassifier(),
                               null, monitor );
            }
            catch ( CoreException e )
            {
                log.debug( "Could not download sources artifact", e );
            }
        }

        return Status.OK_STATUS;
    }

    public void schedule( ArtifactKey key )
    {
        synchronized ( queue )
        {
            queue.add( key );
            schedule( 1000L );
        }
    }
}

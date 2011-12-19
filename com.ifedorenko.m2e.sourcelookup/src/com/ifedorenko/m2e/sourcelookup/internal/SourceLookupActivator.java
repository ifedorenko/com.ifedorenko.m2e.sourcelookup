package com.ifedorenko.m2e.sourcelookup.internal;

import org.eclipse.core.runtime.Plugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.osgi.framework.BundleContext;

public class SourceLookupActivator
    extends Plugin
{

    public static final String PLUGIN_ID = "com.ifedorenko.m2e.sourcelookup";

    private static SourceLookupActivator plugin;

    private BackgroundDownloadJob downloadJob;

    public SourceLookupActivator()
    {
    }

    public void start( BundleContext context )
        throws Exception
    {
        super.start( context );

        plugin = this;

        downloadJob = new BackgroundDownloadJob();
    }

    public void stop( BundleContext context )
        throws Exception
    {
        downloadJob.cancel();
        downloadJob = null;

        plugin = null;

        super.stop( context );
    }

    public static SourceLookupActivator getDefault()
    {
        return plugin;
    }

    public static void scheduleDownload( ArtifactKey artifactKey )
    {
        getDefault().downloadJob.schedule( artifactKey );
    }

}

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

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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

public class SourceLookupActivator
    extends Plugin
{

    public static final String PLUGIN_ID = "com.ifedorenko.m2e.sourcelookup";

    private static SourceLookupActivator plugin;

    private BackgroundProcessingJob backgroundJob;

    private JavaProjectSources javaProjectSources;

    public SourceLookupActivator()
    {
    }

    public void start( BundleContext context )
        throws Exception
    {
        super.start( context );

        plugin = this;

        backgroundJob = new BackgroundProcessingJob();
    }

    public void stop( BundleContext context )
        throws Exception
    {
        backgroundJob.cancel();
        backgroundJob = null;

        javaProjectSources.close();
        javaProjectSources = null;

        plugin = null;

        super.stop( context );
    }

    public static SourceLookupActivator getDefault()
    {
        return plugin;
    }

    public static void schedule( IRunnableWithProgress task )
    {
        getDefault().backgroundJob.schedule( task );
    }

    public static JavaProjectSources getWorkspaceSources()
        throws CoreException
    {
        return getDefault().getWorkspaceSources0();
    }

    private synchronized JavaProjectSources getWorkspaceSources0()
        throws CoreException
    {
        if ( javaProjectSources == null )
        {
            javaProjectSources = new JavaProjectSources();
            javaProjectSources.initialize();
        }
        return javaProjectSources;
    }

    @SuppressWarnings( "restriction" )
    public String getJavaagentString()
        throws CoreException
    {
        return "-javaagent:"
            + org.eclipse.m2e.internal.launch.MavenLaunchUtils.getBundleEntry( getBundle(),
                                                                               "com.ifedorenko.m2e.sourcelookup.javaagent.jar" );
    }
}

package com.ifedorenko.m2e.sourcelookup.internal;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class SourceLookupActivator
    extends AbstractUIPlugin
{

    public static final String PLUGIN_ID = "com.ifedorenko.m2e.sourcelookup";

    private static SourceLookupActivator plugin;

    public SourceLookupActivator()
    {
    }

    public void start( BundleContext context )
        throws Exception
    {
        super.start( context );
        plugin = this;
    }

    public void stop( BundleContext context )
        throws Exception
    {
        plugin = null;
        super.stop( context );
    }

    public static SourceLookupActivator getDefault()
    {
        return plugin;
    }

}

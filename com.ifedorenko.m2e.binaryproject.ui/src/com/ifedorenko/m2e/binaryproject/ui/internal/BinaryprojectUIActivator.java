package com.ifedorenko.m2e.binaryproject.ui.internal;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class BinaryprojectUIActivator
    extends AbstractUIPlugin
{

    // The plug-in ID
    public static final String PLUGIN_ID = "com.ifedorenko.m2e.binaryproject.ui"; //$NON-NLS-1$

    // The shared instance
    private static BinaryprojectUIActivator plugin;

    /**
     * The constructor
     */
    public BinaryprojectUIActivator()
    {
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
     */
    public void start( BundleContext context )
        throws Exception
    {
        super.start( context );
        plugin = this;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    public void stop( BundleContext context )
        throws Exception
    {
        plugin = null;
        super.stop( context );
    }

    /**
     * Returns the shared instance
     * 
     * @return the shared instance
     */
    public static BinaryprojectUIActivator getDefault()
    {
        return plugin;
    }

}

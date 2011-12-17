package com.ifedorenko.m2e.sourcelookup.internal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.m2e.internal.launch.IMavenLaunchParticipant;
import org.eclipse.m2e.internal.launch.MavenLaunchUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings( "restriction" )
public class SourceLookupMavenLaunchParticipant
    implements IMavenLaunchParticipant
{
    public final static String ATTR_SOURCELOOKUP_JAVAAGENT = "com.ifedorenko.m2e.sourcelookup.JAVAAGENT";

    private static final Logger log = LoggerFactory.getLogger( SourceLookupMavenLaunchParticipant.class );

    @Override
    public String getProgramArguments( ILaunchConfiguration configuration, ILaunch launch, IProgressMonitor monitor )
    {
        return null;
    }

    @Override
    public String getVMArguments( ILaunchConfiguration configuration, ILaunch launch, IProgressMonitor monitor )
    {
        try
        {
            if ( shouldEnableSourcelookupJavaagent( configuration, launch.getLaunchMode() ) )
            {
                String javaagent =
                    MavenLaunchUtils.getBundleEntry( SourceLookupActivator.getDefault().getBundle(),
                                                     "com.ifedorenko.m2e.sourcelookup.javaagent.jar" );
                return "-javaagent:" + javaagent;
            }
        }
        catch ( CoreException e )
        {
            log.debug( "Could not read launch configuration", e );
        }

        return null;
    }

    private boolean shouldEnableSourcelookupJavaagent( ILaunchConfiguration configuration, String mode )
        throws CoreException
    {
        return ILaunchManager.DEBUG_MODE.equals( mode )
            && configuration.getAttribute( ATTR_SOURCELOOKUP_JAVAAGENT, false );
    }
}

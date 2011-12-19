package com.ifedorenko.m2e.sourcelookup.internal;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;
import org.eclipse.m2e.internal.launch.IMavenLaunchParticipant;
import org.eclipse.m2e.internal.launch.MavenLaunchUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings( "restriction" )
public class SourceLookupMavenLaunchParticipant
    implements IMavenLaunchParticipant
{
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
            String javaagent =
                MavenLaunchUtils.getBundleEntry( SourceLookupActivator.getDefault().getBundle(),
                                                 "com.ifedorenko.m2e.sourcelookup.javaagent.jar" );
            return "-javaagent:" + javaagent;
        }
        catch ( CoreException e )
        {
            log.error( "Could not locate required resource", e );
        }

        return null;
    }

    @Override
    public List<ISourceLookupParticipant> getSourceLookupParticipants( ILaunchConfiguration configuration,
                                                                       ILaunch launch, IProgressMonitor monitor )
    {
        return Collections.<ISourceLookupParticipant> singletonList( new SourceLookupParticipant() );
    }
}

package com.ifedorenko.m2e.sourcelookup.launch.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;
import org.eclipse.jdt.internal.launching.JavaRemoteApplicationLaunchConfigurationDelegate;
import org.eclipse.jdt.internal.launching.JavaSourceLookupDirector;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaSourceLookupParticipant;

import com.ifedorenko.m2e.sourcelookup.internal.SourceLookupMavenLaunchParticipant;

@SuppressWarnings( "restriction" )
public class ExternalJVMConnectionLaunchDelegate
    extends JavaRemoteApplicationLaunchConfigurationDelegate
{
    private static final SourceLookupMavenLaunchParticipant sourcelookup = new SourceLookupMavenLaunchParticipant();

    @Override
    public void launch( final ILaunchConfiguration configuration, String mode, final ILaunch launch,
                        final IProgressMonitor monitor )
        throws CoreException
    {
        final List<ISourceLookupParticipant> participants = new ArrayList<ISourceLookupParticipant>();
        participants.addAll( sourcelookup.getSourceLookupParticipants( configuration, launch, monitor ) );
        participants.add( new JavaSourceLookupParticipant() );
        JavaSourceLookupDirector sourceLocator = new JavaSourceLookupDirector()
        {
            @Override
            public void initializeParticipants()
            {
                addParticipants( participants.toArray( new ISourceLookupParticipant[participants.size()] ) );
            }
        };
        sourceLocator.initializeParticipants();

        launch.setSourceLocator( sourceLocator );

        super.launch( configuration, mode, launch, monitor );
    }
}

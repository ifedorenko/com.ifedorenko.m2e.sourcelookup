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
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaSourceLookupParticipant;
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
        List<ISourceLookupParticipant> participants = new ArrayList<ISourceLookupParticipant>();

        ServiceLoader<ISourceLookupParticipant> serviceLoader =
            ServiceLoader.load( ISourceLookupParticipant.class,
                                SourceLookupMavenLaunchParticipant.class.getClassLoader() );

        Iterator<ISourceLookupParticipant> participantIterator = serviceLoader.iterator();
        while ( participantIterator.hasNext() )
        {
            participants.add( participantIterator.next() );
        }

        // as a workaround for 368212, javaagent mangles standard java jsr45 strata, which breaks custom source
        // code lookup configuration. To make this work, contribute m2e-aware JavaSourceLookupParticipant, which
        // uses source path information from m2e strata
        participants.add( new JavaSourceLookupParticipant()
        {
            @Override
            public String getSourceName( Object object )
                throws CoreException
            {
                String sourcePath = JDIHelpers.getSourcePath( object );
                return sourcePath != null ? sourcePath : super.getSourceName( object );
            }
        } );
        return participants;
    }
}

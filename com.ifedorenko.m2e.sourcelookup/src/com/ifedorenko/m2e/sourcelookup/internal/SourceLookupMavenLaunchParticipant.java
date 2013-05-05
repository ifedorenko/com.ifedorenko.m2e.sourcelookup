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
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;
import org.eclipse.jdt.internal.launching.JavaSourceLookupDirector;
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
        return getVMArguments();
    }

    public static String getVMArguments()
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
        return getSourceLookupParticipants();
    }

    private static List<ISourceLookupParticipant> getSourceLookupParticipants()
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

        return participants;
    }

    /**
     * Returns fully initialised ISourceLocator instance.
     */
    public static JavaSourceLookupDirector newSourceLocator( String mode )
    {
        final List<ISourceLookupParticipant> participants = new ArrayList<ISourceLookupParticipant>();
        if ( ILaunchManager.DEBUG_MODE.equals( mode ) )
        {
            participants.addAll( getSourceLookupParticipants() );
        }
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
        return sourceLocator;
    }
}

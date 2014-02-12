/*******************************************************************************
 * Copyright (c) 2014 Igor Fedorenko
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

import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;
import org.eclipse.jdt.internal.launching.JavaSourceLookupDirector;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaSourceLookupParticipant;

/**
 * Standalone source code locator. Can be used with standard JDT "Java Application" and "JUnit" launch configuration
 * types. To enable, add the following snippet to .launch file.
 * 
 * <pre>
 * {@code
 *     <stringAttribute 
 *          key="org.eclipse.debug.core.source_locator_id" 
 *          value="com.ifedorenko.m2e.sourcelookupDirector"/>
 * }
 * </pre>
 */
@SuppressWarnings( "restriction" )
public class SourceLookupDirector
    extends JavaSourceLookupDirector
{

    private final String mode;

    public SourceLookupDirector()
    {
        this( null );
    }

    public SourceLookupDirector( String mode )
    {
        this.mode = mode;
    }

    @Override
    public void initializeParticipants()
    {
        final List<ISourceLookupParticipant> participants = new ArrayList<ISourceLookupParticipant>();
        if ( mode == null || ILaunchManager.DEBUG_MODE.equals( mode ) )
        {
            participants.addAll( getSourceLookupParticipants() );
        }
        participants.add( new JavaSourceLookupParticipant() );

        addParticipants( participants.toArray( new ISourceLookupParticipant[participants.size()] ) );
    }

    static List<ISourceLookupParticipant> getSourceLookupParticipants()
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
}

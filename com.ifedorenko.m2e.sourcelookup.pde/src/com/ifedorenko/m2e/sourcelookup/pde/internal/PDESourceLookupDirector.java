package com.ifedorenko.m2e.sourcelookup.pde.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;

import com.ifedorenko.m2e.sourcelookup.internal.jdt.ISourceContainerResolver;
import com.ifedorenko.m2e.sourcelookup.internal.jdt.SourceLookupDirector;
import com.ifedorenko.m2e.sourcelookup.internal.jdt.SourceLookupParticipant;
import com.ifedorenko.m2e.sourcelookup.internal.launch.MavenSourceContainerResolver;

public class PDESourceLookupDirector
    extends SourceLookupDirector
{
    public static final String ID = "com.ifedorenko.pde.sourcelookupDirector";

    @Override
    protected Collection<ISourceLookupParticipant> getSourceLookupParticipants()
    {
        return Collections.<ISourceLookupParticipant>singleton( new SourceLookupParticipant()
        {
            @Override
            protected Collection<ISourceContainerResolver> getSourceContainerResolvers()
            {
                return Arrays.<ISourceContainerResolver>asList( new PDESourceContainerResolver(),
                                                                new MavenSourceContainerResolver() );
            }
        } );
    }
}

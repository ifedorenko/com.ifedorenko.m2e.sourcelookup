package com.ifedorenko.m2e.sourcelookup.internal;

import java.util.Collection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;

class CompositeSourceContainer
    extends org.eclipse.debug.core.sourcelookup.containers.CompositeSourceContainer
{

    private final ISourceContainer[] members;

    public CompositeSourceContainer( Collection<ISourceContainer> members )
    {
        this.members = members.toArray( new ISourceContainer[members.size()] );
    }

    @Override
    public ISourceContainer[] getSourceContainers()
        throws CoreException
    {
        return members;
    }

    @Override
    public String getName()
    {
        return null;
    }

    @Override
    public ISourceContainerType getType()
    {
        return null;
    }

    @Override
    protected ISourceContainer[] createSourceContainers()
        throws CoreException
    {
        return null;
    }

}

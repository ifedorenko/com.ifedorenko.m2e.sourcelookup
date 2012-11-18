package com.ifedorenko.m2e.sourcelookup.internal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

interface IRunnableWithProgress
{
    public void run( IProgressMonitor monitor )
        throws CoreException;
}

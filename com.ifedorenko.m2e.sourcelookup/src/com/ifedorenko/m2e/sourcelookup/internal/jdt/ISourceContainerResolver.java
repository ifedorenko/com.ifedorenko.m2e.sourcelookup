package com.ifedorenko.m2e.sourcelookup.internal.jdt;

import java.io.File;
import java.util.Collection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;

public interface ISourceContainerResolver {

  Collection<ISourceContainer> resolveSourceContainers(File classesLocation, IProgressMonitor monitor)
      throws CoreException;

}

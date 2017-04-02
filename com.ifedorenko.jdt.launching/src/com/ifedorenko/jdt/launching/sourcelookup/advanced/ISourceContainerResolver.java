/*******************************************************************************
 * Copyright (c) 2011-2016 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/
package com.ifedorenko.jdt.launching.sourcelookup.advanced;

import java.io.File;
import java.util.Collection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;

/**
 * Implementations of this interface identify runtime classes locations and download (or "resolve") corresponding
 * sources from external sources repositories like Maven or P2 artifact repositories and PDE target platform.
 * Implementations are registered with the advanced source lookup framework using
 * {@code org.eclipse.jdt.launching.sourceContainerResolvers} extension point.
 * 
 * @since 3.9
 * @provisional This is part of work in progress and can be changed, moved or removed without notice
 */
public interface ISourceContainerResolver {

  /**
   * Creates and returns a collection of source containers that correspond to the given filesystem classes location.
   * Returns {@code null} or an empty collection if requested sources cannot be located.
   */
  public Collection<ISourceContainer> resolveSourceContainers(File classesLocation, IProgressMonitor monitor)
      throws CoreException;

}

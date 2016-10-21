/*******************************************************************************
 * Copyright (c) 2012-2016 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/
package com.ifedorenko.jdt.internal.launching.sourcelookup.advanced;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Runnable with progress interface. Almost exact copy of similar interfaces defined in jface and p2.
 */
@FunctionalInterface
public interface IRunnableWithProgress {
  public void run(IProgressMonitor monitor) throws CoreException;
}

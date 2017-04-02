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
package com.ifedorenko.jdt.internal.launching.sourcelookup.advanced;

import java.io.File;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IStackFrame;

/**
 * Helpers to extract source lookup location information from advanced source lookup JSR-45 strata.
 */
public interface IJDIHelpers {

  public static final IJDIHelpers INSTANCE = new JDIHelpers();

  /**
   * Return classes location the given element was loaded from or {@code null} if the location cannot be determined.
   */
  public File getClassesLocation(Object element) throws DebugException;

  /**
   * Returns source path of the given element or {@code null} if the source path cannot be determined. The returned path
   * is relative to a sources container.
   */
  public String getSourcePath(Object element) throws DebugException;

  /**
   * If the given element is a {@link IStackFrame}, returns classes locations of the stack frames "beneath" the given
   * element. The returned iterable does not include {@code null} elements.
   * 
   * Returns empty iterable if the given element is not a {@link IStackFrame}.
   */
  public Iterable<File> getStackFramesClassesLocations(Object element) throws DebugException;

}

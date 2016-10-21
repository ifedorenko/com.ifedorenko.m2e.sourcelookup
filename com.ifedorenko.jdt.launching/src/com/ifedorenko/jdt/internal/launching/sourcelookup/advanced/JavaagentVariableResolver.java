/*******************************************************************************
 * Copyright (c) 2015-2016 Igor Fedorenko
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
import org.eclipse.core.variables.IDynamicVariable;
import org.eclipse.core.variables.IDynamicVariableResolver;

/**
 * {@code sourcelookup_javaagent} dynamic variable resolver.
 */
public class JavaagentVariableResolver implements IDynamicVariableResolver {
  @Override
  public String resolveValue(IDynamicVariable variable, String argument) throws CoreException {
    return AdvancedSourceLookupSupport.getJavaagentLocation();
  }
}

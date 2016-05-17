/*******************************************************************************
 * Copyright (c) 2015 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/
package com.ifedorenko.m2e.sourcelookup.internal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.IDynamicVariable;
import org.eclipse.core.variables.IDynamicVariableResolver;

public class AgentPathVariableResolver implements IDynamicVariableResolver {
  @Override
  public String resolveValue(IDynamicVariable variable, String argument) throws CoreException {
    return SourceLookupActivator.getDefault().getJavaagentLocation();
  }
}

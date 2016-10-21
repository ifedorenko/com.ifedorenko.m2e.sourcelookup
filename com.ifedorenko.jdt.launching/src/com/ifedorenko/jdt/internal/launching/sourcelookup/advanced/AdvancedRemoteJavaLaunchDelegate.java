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

import static com.ifedorenko.jdt.internal.launching.sourcelookup.advanced.AdvancedSourceLookupSupport.createAdvancedLaunch;
import static com.ifedorenko.jdt.internal.launching.sourcelookup.advanced.AdvancedSourceLookupSupport.isAdvancedSourcelookupEnabled;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.internal.launching.JavaRemoteApplicationLaunchConfigurationDelegate;

@SuppressWarnings("restriction")
public class AdvancedRemoteJavaLaunchDelegate extends JavaRemoteApplicationLaunchConfigurationDelegate {

  @Override
  public ILaunch getLaunch(ILaunchConfiguration configuration, String mode) throws CoreException {
    if (!isAdvancedSourcelookupEnabled()) {
      return super.getLaunch(configuration, mode);
    }
    return createAdvancedLaunch(configuration, mode);
  }

}

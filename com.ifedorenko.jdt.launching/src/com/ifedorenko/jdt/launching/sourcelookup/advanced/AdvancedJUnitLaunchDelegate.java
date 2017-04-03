/*******************************************************************************
 * Copyright (c) 2017 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/
package com.ifedorenko.jdt.launching.sourcelookup.advanced;

import static com.ifedorenko.jdt.internal.launching.sourcelookup.advanced.AdvancedSourceLookupSupport.createAdvancedLaunch;
import static com.ifedorenko.jdt.internal.launching.sourcelookup.advanced.AdvancedSourceLookupSupport.getJavaagentString;
import static com.ifedorenko.jdt.internal.launching.sourcelookup.advanced.AdvancedSourceLookupSupport.isAdvancedSourcelookupEnabled;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationDelegate;

// TODO move to org.eclipse.jdt.junit.core bundle
public class AdvancedJUnitLaunchDelegate extends JUnitLaunchConfigurationDelegate {
  @Override
  public String getVMArguments(ILaunchConfiguration configuration) throws CoreException {
    if (!isAdvancedSourcelookupEnabled()) {
      return super.getVMArguments(configuration);
    }
    // TODO wish we had API similar to zt-exec or at least commons-exec
    return getJavaagentString() + " " + super.getVMArguments(configuration); //$NON-NLS-1$
  }

  @Override
  public ILaunch getLaunch(ILaunchConfiguration configuration, String mode) throws CoreException {
    if (!isAdvancedSourcelookupEnabled()) {
      return super.getLaunch(configuration, mode);
    }
    return createAdvancedLaunch(configuration, mode);
  }
}

/*******************************************************************************
 * Copyright (c) 2016 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/
package com.ifedorenko.m2e.sourcelookup.pde.internal;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.pde.launching.JUnitLaunchConfigurationDelegate;

public class JUnitPluginTestLauncher extends JUnitLaunchConfigurationDelegate {

  @SuppressWarnings({"rawtypes", "unchecked"}) // needed to compile with mars
  @Override
  protected void collectExecutionArguments(ILaunchConfiguration configuration, List/* <String> */ vmArguments,
      List/* <String> */ programArgs) throws CoreException {
    super.collectExecutionArguments(configuration, vmArguments, programArgs);
    LaunchDelegateImpl.injectFrameworkExtension(getConfigurationDirectory(configuration));
    LaunchDelegateImpl.appendJavaagentString(vmArguments);
  }

  @Override
  public ILaunch getLaunch(ILaunchConfiguration configuration, String mode) throws CoreException {
    return LaunchDelegateImpl.getLaunch(configuration, mode);
  }

}

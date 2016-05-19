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
package com.ifedorenko.m2e.sourcelookup.internal.jdt.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.IPersistableSourceLocator;
import org.eclipse.debug.core.sourcelookup.IPersistableSourceLocator2;
import org.eclipse.jdt.internal.launching.JavaRemoteApplicationLaunchConfigurationDelegate;

import com.ifedorenko.m2e.sourcelookup.internal.jdt.SourceLookupDirector;


@SuppressWarnings("restriction")
public class RemoteJavaApplicationLauncher extends JavaRemoteApplicationLaunchConfigurationDelegate {

  @Override
  public ILaunch getLaunch(ILaunchConfiguration configuration, String mode) throws CoreException {
    // TODO reconcile with LaunchDelegateImpl

    Launch launch = new Launch(configuration, mode, null);

    IPersistableSourceLocator locator = getLaunchManager().newSourceLocator(SourceLookupDirector.ID);
    String memento = configuration.getAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_MEMENTO, (String) null);
    if (memento == null) {
      locator.initializeDefaults(configuration);
    } else {
      if (locator instanceof IPersistableSourceLocator2) {
        ((IPersistableSourceLocator2) locator).initializeFromMemento(memento, configuration);
      } else {
        locator.initializeFromMemento(memento);
      }
    }
    launch.setSourceLocator(locator);

    return launch;
  }

}

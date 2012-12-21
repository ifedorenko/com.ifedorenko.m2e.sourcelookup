/*******************************************************************************
 * Copyright (c) 2012 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/
package com.ifedorenko.m2e.sourcelookup.launch.internal;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaConnectTab;

public class ExternalJVMConnectionLaunchConfigurationTabGroup
    extends AbstractLaunchConfigurationTabGroup
{
    @Override
    public void createTabs( ILaunchConfigurationDialog dialog, String mode )
    {
        ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] { new JavaConnectTab(), //
            new DynamicSourceLookupTab(), //
            new SourceLookupTab(), //
            new CommonTab(), //
        };
        setTabs( tabs );
    }

}

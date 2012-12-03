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

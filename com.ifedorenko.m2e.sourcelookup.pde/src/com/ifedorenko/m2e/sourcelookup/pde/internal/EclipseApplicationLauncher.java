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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.pde.launching.EclipseApplicationLaunchConfiguration;

public class EclipseApplicationLauncher
    extends EclipseApplicationLaunchConfiguration
{

    @Override
    public String[] getProgramArguments( ILaunchConfiguration configuration )
        throws CoreException
    {
        String[] programArguments = super.getProgramArguments( configuration );
        LaunchDelegateImpl.injectFrameworkExtension( getConfigDir( configuration ) );
        return programArguments;
    }

    @Override
    public String[] getVMArguments( ILaunchConfiguration configuration )
        throws CoreException
    {
        return LaunchDelegateImpl.appendJavaagentString( super.getVMArguments( configuration ) );
    }

    @Override
    public ILaunch getLaunch( ILaunchConfiguration configuration, String mode )
        throws CoreException
    {
        return LaunchDelegateImpl.getLaunch( configuration, mode );
    }

}

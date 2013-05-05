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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.internal.launching.JavaRemoteApplicationLaunchConfigurationDelegate;

import com.ifedorenko.m2e.sourcelookup.internal.SourceLookupMavenLaunchParticipant;

@SuppressWarnings( "restriction" )
public class ExternalJVMConnectionLaunchDelegate
    extends JavaRemoteApplicationLaunchConfigurationDelegate
{
    @Override
    public void launch( final ILaunchConfiguration configuration, String mode, final ILaunch launch,
                        final IProgressMonitor monitor )
        throws CoreException
    {
        launch.setSourceLocator( SourceLookupMavenLaunchParticipant.newSourceLocator( mode ) );

        super.launch( configuration, mode, launch, monitor );
    }
}

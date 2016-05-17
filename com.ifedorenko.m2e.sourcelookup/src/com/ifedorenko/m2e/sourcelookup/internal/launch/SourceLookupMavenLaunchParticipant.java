/*******************************************************************************
 * Copyright (c) 2011 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/
package com.ifedorenko.m2e.sourcelookup.internal.launch;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;
import org.eclipse.m2e.internal.launch.IMavenLaunchParticipant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ifedorenko.m2e.sourcelookup.internal.SourceLookupActivator;

@SuppressWarnings("restriction")
public class SourceLookupMavenLaunchParticipant implements IMavenLaunchParticipant {
  private static final Logger log = LoggerFactory.getLogger(SourceLookupMavenLaunchParticipant.class);

  @Override
  public String getProgramArguments(ILaunchConfiguration configuration, ILaunch launch, IProgressMonitor monitor) {
    return null;
  }

  @Override
  public String getVMArguments(ILaunchConfiguration configuration, ILaunch launch, IProgressMonitor monitor) {
    return getVMArguments();
  }

  public static String getVMArguments() {
    try {
      return SourceLookupActivator.getDefault().getJavaagentString();
    } catch (CoreException e) {
      log.error("Could not locate required resource", e);
    }

    return null;
  }

  @Override
  public List<ISourceLookupParticipant> getSourceLookupParticipants(ILaunchConfiguration configuration, ILaunch launch,
      IProgressMonitor monitor) {
    return Collections.<ISourceLookupParticipant>singletonList(new MavenSourceLookupParticipant());
  }
}

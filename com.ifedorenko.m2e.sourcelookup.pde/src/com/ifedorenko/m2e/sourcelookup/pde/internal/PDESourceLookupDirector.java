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
package com.ifedorenko.m2e.sourcelookup.pde.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;
import org.eclipse.jdt.launching.sourcelookup.advanced.ISourceContainerResolver;
import org.eclipse.jdt.launching.sourcelookup.advanced.JavaAdvancedSourceLookupDirector;
import org.eclipse.jdt.launching.sourcelookup.advanced.SourceLookupParticipant;

import com.ifedorenko.m2e.sourcelookup.internal.launch.MavenSourceContainerResolver;

public class PDESourceLookupDirector extends JavaAdvancedSourceLookupDirector {
  public static final String ID = "com.ifedorenko.pde.sourcelookupDirector";

  @Override
  protected Collection<ISourceLookupParticipant> getSourceLookupParticipants() {
    return Collections.singleton(new SourceLookupParticipant() {
      @Override
      protected Collection<ISourceContainerResolver> getSourceContainerResolvers() {
        return Arrays.asList(new PDESourceContainerResolver(), new MavenSourceContainerResolver());
      }
    });
  }
}

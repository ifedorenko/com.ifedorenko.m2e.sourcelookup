/*******************************************************************************
 * Copyright (c) 2014-2016 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/
package com.ifedorenko.jdt.launching.sourcelookup.advanced;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;
import org.eclipse.jdt.internal.launching.JavaSourceLookupDirector;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaSourceLookupParticipant;

/**
 * Advanced java source lookup director.
 * 
 * Can be used with standard JDT "Java Application" and "JUnit" launch configuration types. To enable, add the following
 * snippet to .launch file.
 * 
 * <pre>
 * {@code
 *     <stringAttribute 
 *          key="org.eclipse.debug.core.source_locator_id" 
 *          value="org.eclipse.jdt.launching.sourceLocator.JavaAdvancedSourceLookupDirector"/>
 * }
 * </pre>
 * 
 * Note to self: JavaSourceLookupDirector parent class is useful because it allows custom source lookup path in the
 * launch configuration.
 * 
 * @provisional This is part of work in progress and can be changed, moved or removed without notice
 */
@SuppressWarnings("restriction")
public class AdvancedSourceLookupDirector extends JavaSourceLookupDirector {
  public static final String ID = "com.ifedorenko.jdt.launching.sourceLocator"; //$NON-NLS-1$

  private final String mode;

  public AdvancedSourceLookupDirector() {
    this(null);
  }

  public AdvancedSourceLookupDirector(String mode) {
    this.mode = mode;
  }

  @Override
  public void initializeParticipants() {
    final List<ISourceLookupParticipant> participants = new ArrayList<>();
    if (mode == null || ILaunchManager.DEBUG_MODE.equals(mode)) {
      participants.addAll(getSourceLookupParticipants());
    }

    // fall-back to default JDT behaviour if we can't find matching sources
    // in most cases this means scanning workspace for any source or binary with matching name
    participants.add(new JavaSourceLookupParticipant());

    addParticipants(participants.toArray(new ISourceLookupParticipant[participants.size()]));
  }

  protected Collection<ISourceLookupParticipant> getSourceLookupParticipants() {
    return Collections.singleton(new AdvancedSourceLookupParticipant());
  }
}

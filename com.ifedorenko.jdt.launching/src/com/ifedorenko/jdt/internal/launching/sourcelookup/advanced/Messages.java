/*******************************************************************************
 * Copyright (c) 2015-2016 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/
package com.ifedorenko.jdt.internal.launching.sourcelookup.advanced;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
  private static final String BUNDLE_NAME = "org.eclipse.jdt.internal.launching.sourcelookup.advanced.messages"; //$NON-NLS-1$
  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {}

  public static String BackgroundProcessingJob_name;
  public static String BackgroundProcessingJob_failed;

}

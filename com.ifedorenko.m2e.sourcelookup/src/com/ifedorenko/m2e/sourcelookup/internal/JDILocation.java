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
package com.ifedorenko.m2e.sourcelookup.internal;

import java.io.File;

public class JDILocation
{
    private final Object debugElement;

    private final File location;

    JDILocation( Object debugElement, File location )
    {
        this.debugElement = debugElement;
        this.location = location;
    }

    public File getLocation()
    {
        return location;
    }

    public Object getDebugElement()
    {
        return debugElement;
    }
}

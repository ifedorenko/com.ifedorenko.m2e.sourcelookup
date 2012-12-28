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
package com.ifedorenko.m2e.sourcelookup.ui.internal.bug396796;

import java.io.File;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

public class DebugElementWrapper
{
    private final Object debugElement;

    private final File location;

    DebugElementWrapper( Object debugElement, File location )
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

    public static Object getDebugElement( ISelection selection )
    {
        Object element = null;
        if ( selection instanceof IStructuredSelection && !selection.isEmpty() )
        {
            element = ( (IStructuredSelection) selection ).getFirstElement();
            if ( element instanceof DebugElementWrapper )
            {
                element = ( (DebugElementWrapper) element ).getDebugElement();
            }
        }
        return element;
    }
}

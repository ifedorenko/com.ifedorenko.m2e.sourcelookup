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

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugElement;

@SuppressWarnings( "rawtypes" )
public class JDILocationAdapterFactory
    implements IAdapterFactory
{
    private static final Class[] ADAPTER_LIST = new Class[] { JDILocation.class, };

    @Override
    public Object getAdapter( Object adaptableObject, Class adapterType )
    {
        if ( !JDILocation.class.equals( adapterType ) || !( adaptableObject instanceof IDebugElement ) )
        {
            return null;
        }
        try
        {
            File location = JDIHelpers.getLocation( adaptableObject );
            if ( location != null )
            {
                return new JDILocation( adaptableObject, location );
            }
        }
        catch ( DebugException e )
        {
            // too bad
        }
        return null;
    }

    @Override
    public Class[] getAdapterList()
    {
        return ADAPTER_LIST;
    }

}

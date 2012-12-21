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

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.debug.core.DebugException;

public class JDIStratumTester
    extends PropertyTester
{

    @Override
    public boolean test( Object receiver, String property, Object[] args, Object expectedValue )
    {
        boolean result;
        try
        {
            result = JDIHelpers.getLocation( receiver ) != null;
        }
        catch ( DebugException e )
        {
            result = false;
        }
        return result;
    }

}

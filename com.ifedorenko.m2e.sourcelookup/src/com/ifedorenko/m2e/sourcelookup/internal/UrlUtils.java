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
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class UrlUtils
{
    public static File toFile( String urlStr )
    {
        try
        {
            URL url = new URL( urlStr );
            if ( "file".equals( url.getProtocol() ) )
            {
                return new File( url.getPath() );
            }
        }
        catch ( MalformedURLException e )
        {
            // fall through
        }

        return null;
    }

    public static IPath toPath( String urlStr )
    {
        File file = toFile( urlStr );
        return file != null ? Path.fromOSString( file.getAbsolutePath() ) : null;
    }
}

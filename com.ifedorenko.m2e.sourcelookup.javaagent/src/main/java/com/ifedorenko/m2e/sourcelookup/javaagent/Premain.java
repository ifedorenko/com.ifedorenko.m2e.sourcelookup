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
package com.ifedorenko.m2e.sourcelookup.javaagent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;

public class Premain
{
    static final ClassfileTransformer transformer = new ClassfileTransformer();

    public static void premain( String agentArgs, Instrumentation inst )
    {
        System.err.println( "Dynamic source lookup support loaded." );

        inst.addTransformer( new ClassFileTransformer()
        {
            public byte[] transform( ClassLoader loader, final String className, Class<?> classBeingRedefined,
                                     ProtectionDomain protectionDomain, byte[] classfileBuffer )
                throws IllegalClassFormatException
            {
                try
                {
                    if ( protectionDomain == null )
                    {
                        return null;
                    }

                    final CodeSource codeSource = protectionDomain.getCodeSource();
                    if ( codeSource == null )
                    {
                        return null;
                    }

                    final URL locationUrl = codeSource.getLocation();
                    if ( locationUrl == null )
                    {
                        return null;
                    }

                    final String location = locationUrl.toExternalForm();

                    return transformer.transform( classfileBuffer, location );
                }
                catch ( Exception e )
                {
                    System.err.print( "Could not instrument class " + className + ": " );
                    e.printStackTrace( System.err );
                }
                return null;
            }
        } );
    }
}

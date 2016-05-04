/*******************************************************************************
 * Copyright (c) 2014 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/
package com.ifedorenko.m2e.sourcelookup.weaving;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public class ClassfileTransformer
{

    public byte[] transform( byte[] classfileBuffer, final String location )
    {

        final ClassReader r = new ClassReader( classfileBuffer, 0, classfileBuffer.length );
        final ClassWriter w = new ClassWriter( 0 );

        r.accept( new ClassVisitor( Opcodes.ASM5, w )
        {
            public void visitSource( String source, String debug )
            {
                String javaSource = source;
                // TODO merge SMAP if present (always present when used together with Equinox weaver)
                if ( debug == null )
                {
                    StringBuilder smap = new StringBuilder();
                    smap.append( "SMAP\n" );
                    smap.append( javaSource ).append( "\n" );
                    smap.append( "Java\n" ); // default strata name
                    smap.append( "*S m2e\n" );
                    smap.append( "*F\n" );
                    smap.append( "1 " ).append( source ).append( "\n" );
                    smap.append( "2 " ).append( location ).append( "\n" );
                    // JSR-045, StratumSection
                    // "One FileSection and one LineSection (in either order) must follow the StratumSection"
                    smap.append( "*L\n" );
                    smap.append( "*E\n" );
                    debug = smap.toString();
                }

                super.visitSource( javaSource, debug );
            };

        }, 0 );

        return w.toByteArray();
    }
}

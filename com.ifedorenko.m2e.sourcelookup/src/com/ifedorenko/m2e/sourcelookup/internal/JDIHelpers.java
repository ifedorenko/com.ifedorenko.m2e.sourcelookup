/*******************************************************************************
 * Copyright (c) 2011-2012 Igor Fedorenko
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

import org.eclipse.debug.core.DebugException;
import org.eclipse.jdt.debug.core.IJavaObject;
import org.eclipse.jdt.debug.core.IJavaReferenceType;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaType;

final class JDIHelpers
{
    private JDIHelpers()
    {
    }

    // jdt debug boilerplate and other ideas are "borrowed" from
    // org.eclipse.pde.internal.launching.sourcelookup.PDESourceLookupQuery.run()

    public static String getLocation( Object fElement )
        throws DebugException
    {
        IJavaReferenceType declaringType = null;
        if ( fElement instanceof IJavaStackFrame )
        {
            IJavaStackFrame stackFrame = (IJavaStackFrame) fElement;
            declaringType = stackFrame.getReferenceType();
        }
        else if ( fElement instanceof IJavaObject )
        {
            IJavaType javaType = ( (IJavaObject) fElement ).getJavaType();
            if ( javaType instanceof IJavaReferenceType )
            {
                declaringType = (IJavaReferenceType) javaType;
            }
        }
        else if ( fElement instanceof IJavaReferenceType )
        {
            declaringType = (IJavaReferenceType) fElement;
        }

        if ( declaringType != null )
        {
            String[] locations = declaringType.getSourceNames( "m2e" );

            if ( locations == null || locations.length < 1 )
            {
                return null;
            }

            return locations[0];
        }

        return null;
    }

    public static String getSourcePath( Object fElement )
        throws DebugException
    {
        IJavaReferenceType declaringType = null;
        if ( fElement instanceof IJavaStackFrame )
        {
            IJavaStackFrame stackFrame = (IJavaStackFrame) fElement;
            // under JSR 45 source path from the stack frame is more precise than anything derived from the type:
            String sourcePath = stackFrame.getSourcePath();
            if ( sourcePath != null )
            {
                return sourcePath;
            }

            declaringType = stackFrame.getReferenceType();
        }
        else if ( fElement instanceof IJavaObject )
        {
            IJavaType javaType = ( (IJavaObject) fElement ).getJavaType();
            if ( javaType instanceof IJavaReferenceType )
            {
                declaringType = (IJavaReferenceType) javaType;
            }
        }
        else if ( fElement instanceof IJavaReferenceType )
        {
            declaringType = (IJavaReferenceType) fElement;
        }

        if ( declaringType != null )
        {
            String declaringTypeName = declaringType.getName();
            String[] sourcePaths = declaringType.getSourcePaths( null );
            return sourcePaths != null ? sourcePaths[0] : generateSourceName( declaringTypeName );
        }

        return null;
    }

    // copy&paste from org.eclipse.pde.internal.launching.sourcelookup.PDESourceLookupQuery.generateSourceName(String)
    private static String generateSourceName( String qualifiedTypeName )
    {
        int index = qualifiedTypeName.indexOf( '$' );
        if ( index >= 0 )
            qualifiedTypeName = qualifiedTypeName.substring( 0, index );
        return qualifiedTypeName.replace( '.', File.separatorChar ) + ".java"; //$NON-NLS-1$
    }

}

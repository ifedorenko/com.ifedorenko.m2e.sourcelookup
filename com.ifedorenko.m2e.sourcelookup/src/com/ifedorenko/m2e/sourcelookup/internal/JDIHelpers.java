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
import org.eclipse.jdt.debug.core.IJavaVariable;

public final class JDIHelpers
{
    public static final String STRATA_M2E = "m2e";

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
        else if ( fElement instanceof IJavaVariable )
        {
            IJavaType javaType = ( (IJavaVariable) fElement ).getJavaType();
            if ( javaType instanceof IJavaReferenceType )
            {
                declaringType = (IJavaReferenceType) javaType;
            }
        }

        if ( declaringType != null )
        {
            String[] locations = declaringType.getSourceNames( STRATA_M2E );

            if ( locations == null || locations.length < 2 )
            {
                return null;
            }

            return locations[1];
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
            // under JSR 45 source path from the stack frame is more precise than anything derived from the type
            String sourcePath = stackFrame.getSourcePath( STRATA_M2E );
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
        else if ( fElement instanceof IJavaVariable )
        {
            IJavaType javaType = ( (IJavaVariable) fElement ).getJavaType();
            if ( javaType instanceof IJavaReferenceType )
            {
                declaringType = (IJavaReferenceType) javaType;
            }
        }

        if ( declaringType != null )
        {
            String[] sourcePaths = declaringType.getSourcePaths( STRATA_M2E );

            if ( sourcePaths != null && sourcePaths.length > 0 && sourcePaths[0] != null )
            {
                return sourcePaths[0];
            }

            return generateSourceName( declaringType.getName() );
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

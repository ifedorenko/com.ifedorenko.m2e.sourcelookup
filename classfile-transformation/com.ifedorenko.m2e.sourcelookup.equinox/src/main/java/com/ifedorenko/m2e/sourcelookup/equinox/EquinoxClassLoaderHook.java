/*******************************************************************************
 * Copyright (c) 2012-2016 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/
package com.ifedorenko.m2e.sourcelookup.equinox;

import org.eclipse.osgi.internal.hookregistry.ClassLoaderHook;
import org.eclipse.osgi.internal.hookregistry.HookConfigurator;
import org.eclipse.osgi.internal.hookregistry.HookRegistry;
import org.eclipse.osgi.internal.loader.classpath.ClasspathEntry;
import org.eclipse.osgi.internal.loader.classpath.ClasspathManager;
import org.eclipse.osgi.storage.bundlefile.BundleEntry;

import com.ifedorenko.m2e.sourcelookup.weaving.ClassfileTransformer;

public class EquinoxClassLoaderHook
    extends ClassLoaderHook
    implements HookConfigurator
{

    private static final ClassfileTransformer transformer = new ClassfileTransformer();

    @Override
    public byte[] processClass( String name, byte[] classbytes, ClasspathEntry classpathEntry, BundleEntry entry,
                                ClasspathManager manager )
    {
        final String location = classpathEntry.getBundleFile().getBaseFile().toURI().toASCIIString();

        return transformer.transform( classbytes, location );
    }

    @Override
    public void addHooks( HookRegistry hookRegistry )
    {
        hookRegistry.addClassLoaderHook( this );
    }

}

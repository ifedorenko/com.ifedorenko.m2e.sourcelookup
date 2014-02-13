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
package com.ifedorenko.m2e.sourcelookup.javaagent;

import java.security.ProtectionDomain;
import java.util.ArrayList;

import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.baseadaptor.HookConfigurator;
import org.eclipse.osgi.baseadaptor.HookRegistry;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.baseadaptor.hooks.ClassLoadingHook;
import org.eclipse.osgi.baseadaptor.loader.BaseClassLoader;
import org.eclipse.osgi.baseadaptor.loader.ClasspathEntry;
import org.eclipse.osgi.baseadaptor.loader.ClasspathManager;
import org.eclipse.osgi.framework.adaptor.BundleProtectionDomain;
import org.eclipse.osgi.framework.adaptor.ClassLoaderDelegate;

public class EquinoxClassloadingHook
    implements ClassLoadingHook, HookConfigurator
{

    private ClassfileTransformer transformer = new ClassfileTransformer();

    @Override
    public void addHooks( HookRegistry hookRegistry )
    {
        hookRegistry.addClassLoadingHook( this );
    }

    @Override
    public byte[] processClass( String name, byte[] classbytes, ClasspathEntry classpathEntry, BundleEntry entry,
                                ClasspathManager manager )
    {
        final String location;
        final BundleEntry rootEntry = classpathEntry.getBundleFile().getEntry( "/" );
        if ( rootEntry != null )
        {
            location = rootEntry.getFileURL().toExternalForm();
        }
        else
        {
            location = classpathEntry.getBundleFile().getBaseFile().toURI().toASCIIString();
        }

        return transformer.transform( classbytes, location );
    }

    @Override
    @SuppressWarnings( "rawtypes" )
    public boolean addClassPathEntry( ArrayList/*<ClasspathEntry>*/ cpEntries, String cp, ClasspathManager hostmanager,
                                      BaseData sourcedata, ProtectionDomain sourcedomain )
    {
        return false;
    }

    @Override
    public String findLibrary( BaseData data, String libName )
    {
        return null;
    }

    @Override
    public ClassLoader getBundleClassLoaderParent()
    {
        return null;
    }

    @Override
    public BaseClassLoader createClassLoader( ClassLoader parent, ClassLoaderDelegate delegate,
                                              BundleProtectionDomain domain, BaseData data, String[] bundleclasspath )
    {
        return null;
    }

    @Override
    public void initializedClassLoader( BaseClassLoader baseClassLoader, BaseData data )
    {
    }
}

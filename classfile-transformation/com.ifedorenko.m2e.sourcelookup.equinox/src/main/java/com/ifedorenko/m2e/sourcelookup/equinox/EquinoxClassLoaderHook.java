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

import java.lang.reflect.Field;

import org.eclipse.jdt.launching.internal.weaving.ClassfileTransformer;
import org.eclipse.osgi.internal.hookregistry.ClassLoaderHook;
import org.eclipse.osgi.internal.hookregistry.HookConfigurator;
import org.eclipse.osgi.internal.hookregistry.HookRegistry;
import org.eclipse.osgi.internal.loader.classpath.ClasspathEntry;
import org.eclipse.osgi.internal.loader.classpath.ClasspathManager;
import org.eclipse.osgi.storage.SystemBundleFile;
import org.eclipse.osgi.storage.bundlefile.BundleEntry;
import org.eclipse.osgi.storage.bundlefile.BundleFile;
import org.eclipse.osgi.storage.bundlefile.FileBundleEntry;
import org.eclipse.osgi.storage.bundlefile.NestedDirBundleFile;

public class EquinoxClassLoaderHook extends ClassLoaderHook implements HookConfigurator {

  private static final ClassfileTransformer transformer = new ClassfileTransformer();

  @Override
  public byte[] processClass(String name, byte[] classbytes, ClasspathEntry classpathEntry, BundleEntry entry,
      ClasspathManager manager) {
    final String location = getBundleLocation(classpathEntry);
    // final String location = getLocation( classpathEntry.getBundleFile() );
    if (location == null) {
      return null;
    }
    return transformer.transform(classbytes, location);
  }

  static String getLocation(BundleFile bundleFile) {
    if (bundleFile instanceof NestedDirBundleFile) {
      BundleEntry rootEntry = bundleFile.getEntry("/");
      if (!(rootEntry instanceof FileBundleEntry)) {
        return null; // not supported
      }
      return ((FileBundleEntry) rootEntry).getFileURL().toExternalForm();
    } else if (bundleFile instanceof SystemBundleFile) {
      return null;
    }
    return bundleFile.getBaseFile().toURI().toASCIIString();
  }

  private static final Field dataField;

  private static final Field filenameField;

  static {
    Field _dataField = null;
    Field _filenameField = null;
    try {
      _dataField = ClasspathEntry.class.getDeclaredField("data");
      _filenameField = _dataField.getType().getDeclaredField("fileName");

      _dataField.setAccessible(true);
      _filenameField.setAccessible(true);
    } catch (NoSuchFieldException ignored) {
      // incompatible equinox version, can't do anything about it
    } catch (SecurityException ignores) {
      // highly unlikely to happen, requires custom security manager
    }

    dataField = _dataField;
    filenameField = _filenameField;
  }

  static String getBundleLocation(ClasspathEntry entry) {
    // use PDE-specific state maintained by Equinox
    // this appears to be the only way to find fragments locations

    if (dataField == null || filenameField == null) {
      return null;
    }

    try {
      Object data = dataField.get(entry);
      return "file:" + filenameField.get(data);
    } catch (IllegalAccessException e) {
      return null;
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  @Override
  public void addHooks(HookRegistry hookRegistry) {
    hookRegistry.addClassLoaderHook(this);
  }

}

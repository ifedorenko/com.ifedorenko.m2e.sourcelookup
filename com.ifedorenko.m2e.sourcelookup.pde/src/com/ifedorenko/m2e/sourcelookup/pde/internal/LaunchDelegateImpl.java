/*******************************************************************************
 * Copyright (c) 2016 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/
package com.ifedorenko.m2e.sourcelookup.pde.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.IPersistableSourceLocator;
import org.eclipse.debug.core.sourcelookup.IPersistableSourceLocator2;

import com.ifedorenko.m2e.sourcelookup.internal.SourceLookupActivator;

class LaunchDelegateImpl {
  public static void injectFrameworkExtension(File configurationDirectory) throws CoreException {
    File configFile = new File(configurationDirectory, "config.ini");
    Properties props = new Properties();
    try (InputStream is = new FileInputStream(configFile)) {
      props.load(is);
    } catch (IOException e) {
      // create new file
    }

    String PROPS_FRAMEWORK_EXTENSIONS = "osgi.framework.extensions"; // TODO proper constant

    String extensions = props.getProperty(PROPS_FRAMEWORK_EXTENSIONS, "");
    if (extensions.length() > 0) {
      extensions += ",";
    }

    extensions += "reference:file:" + getBundleFile("/com.ifedorenko.m2e.sourcelookup.equinox.jar");

    props.put(PROPS_FRAMEWORK_EXTENSIONS, extensions);
    try (OutputStream os = new FileOutputStream(configFile)) {
      props.store(os, "Configuration File"); // useless comment from LaunchConfigurationHelper.save
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public static ILaunch getLaunch(ILaunchConfiguration configuration, String mode) throws CoreException {
    Launch launch = new Launch(configuration, mode, null);

    IPersistableSourceLocator locator = getLaunchManager().newSourceLocator(PDESourceLookupDirector.ID);
    String memento = configuration.getAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_MEMENTO, (String) null);
    if (memento == null) {
      locator.initializeDefaults(configuration);
    } else {
      if (locator instanceof IPersistableSourceLocator2) {
        ((IPersistableSourceLocator2) locator).initializeFromMemento(memento, configuration);
      } else {
        locator.initializeFromMemento(memento);
      }
    }
    launch.setSourceLocator(locator);

    return launch;
  }

  public static List<String> appendJavaagentString(List<String> jvmargs) throws CoreException {
    jvmargs.add(SourceLookupActivator.getDefault().getJavaagentString());
    return jvmargs;
  }

  public static String[] appendJavaagentString(String[] jvmargs) throws CoreException {
    String[] result = new String[jvmargs.length + 1];
    System.arraycopy(jvmargs, 0, result, 0, jvmargs.length);
    result[jvmargs.length] = SourceLookupActivator.getDefault().getJavaagentString();
    return result;
  }

  private static ILaunchManager getLaunchManager() {
    return DebugPlugin.getDefault().getLaunchManager();
  }

  private static String getBundleFile(String path) throws CoreException {
    ClassLoader cl = LaunchDelegateImpl.class.getClassLoader();
    return SourceLookupActivator.toLocalFile(cl.getResource(path));
  }
}

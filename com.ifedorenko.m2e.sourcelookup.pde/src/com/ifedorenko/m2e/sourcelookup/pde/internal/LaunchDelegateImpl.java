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

import static org.eclipse.jdt.launching.sourcelookup.advanced.AdvancedSourceLookup.createSourceLocator;
import static org.eclipse.jdt.launching.sourcelookup.advanced.AdvancedSourceLookup.getJavaagentString;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.Launch;


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
    return new Launch(configuration, mode, createSourceLocator(PDESourceLookupDirector.ID, configuration));
  }

  public static List<String> appendJavaagentString(List<String> jvmargs) throws CoreException {
    jvmargs.add(getJavaagentString());
    return jvmargs;
  }

  public static String[] appendJavaagentString(String[] jvmargs) throws CoreException {
    String[] result = new String[jvmargs.length + 1];
    System.arraycopy(jvmargs, 0, result, 0, jvmargs.length);
    result[jvmargs.length] = getJavaagentString();
    return result;
  }

  private static String getBundleFile(String path) throws CoreException {
    ClassLoader cl = LaunchDelegateImpl.class.getClassLoader();
    URL resource = cl.getResource(path);
    try {
      return new File(FileLocator.toFileURL(resource).toURI()).getCanonicalPath();
    } catch (IOException | URISyntaxException e) {
      throw new CoreException(new Status(IStatus.ERROR, "com.ifedorenko.m2e.sourcelookup.pde", e.getMessage(), e));
    }
  }
}

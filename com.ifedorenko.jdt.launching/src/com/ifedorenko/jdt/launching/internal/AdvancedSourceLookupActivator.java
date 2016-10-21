package com.ifedorenko.jdt.launching.internal;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.ifedorenko.jdt.internal.launching.sourcelookup.advanced.AdvancedSourceLookupSupport;

public class AdvancedSourceLookupActivator implements BundleActivator {

  public static final String ID_PLUGIN = "com.ifedorenko.jdt.launching";
  public static final String ID_sourceContainerResolvers = ID_PLUGIN + ".sourceContainerResolvers";
  public static final String ID_workspaceProjectDescribers = ID_PLUGIN + ".workspaceProjectDescribers";

  private static Bundle bundle;

  @Override
  public void start(BundleContext context) throws Exception {
    bundle = context.getBundle();
    AdvancedSourceLookupSupport.start();
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    AdvancedSourceLookupSupport.stop();
    bundle = null;
  }

  public static File getFileInPlugin(IPath path) {
    // copy&paste from org.eclipse.jdt.internal.launching.LaunchingPlugin.getFileInPlugin(IPath)
    try {
      URL installURL = new URL(bundle.getEntry("/"), path.toString()); //$NON-NLS-1$
      URL localURL = FileLocator.toFileURL(installURL);
      return new File(localURL.getFile());
    } catch (IOException ioe) {
      return null;
    }
  }
}

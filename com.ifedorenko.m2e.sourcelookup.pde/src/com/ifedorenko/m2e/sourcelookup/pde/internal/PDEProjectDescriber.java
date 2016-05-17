package com.ifedorenko.m2e.sourcelookup.pde.internal;

import java.io.File;
import java.util.Collections;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaProjectSourceContainer;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;

import com.ifedorenko.m2e.sourcelookup.internal.jdt.IProjectSourceDescriber;

public class PDEProjectDescriber extends IProjectSourceDescriber {

  @Override
  public void describeProject(IJavaProject project, IJavaProjectSourceDescription description) throws CoreException {
    IPluginModelBase bundle = PluginRegistry.findModel(project.getProject());

    if (bundle == null) {
      return;
    }

    description.addLocation(new File(bundle.getInstallLocation()));
    description.addSourceContainerFactory(() -> Collections.singleton(new JavaProjectSourceContainer(project)));

    // TODO dependencies
  }

}

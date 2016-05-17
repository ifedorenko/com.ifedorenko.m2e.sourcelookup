package com.ifedorenko.m2e.binaryproject.sourcelookup;

import java.io.File;
import java.util.Collections;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.launching.sourcelookup.containers.PackageFragmentRootSourceContainer;

import com.ifedorenko.m2e.binaryproject.BinaryProjectPlugin;
import com.ifedorenko.m2e.sourcelookup.internal.jdt.IProjectSourceDescriber;

public class BinaryProjectDescriber extends IProjectSourceDescriber {

  private static File getBinaryLocation(IJavaProject project) throws CoreException {
    final String binaryLocation = project.getProject().getPersistentProperty(BinaryProjectPlugin.QNAME_JAR);
    if (binaryLocation == null) {
      return null;
    }
    return new File(binaryLocation);
  }

  @Override
  public void describeProject(IJavaProject project, IJavaProjectSourceDescription description) throws CoreException {
    final File binaryLocation = getBinaryLocation(project);
    if (binaryLocation == null) {
      return;
    }

    Map<File, IPackageFragmentRoot> classpath = getClasspath(project);
    IPackageFragmentRoot binary = classpath.remove(binaryLocation);

    if (binary == null) {
      return; // this is a bug somewhere in my code
    }

    description.addDependencies(classpath);
    description.addLocation(binaryLocation);
    description.addSourceContainerFactory(() -> Collections.singleton(new PackageFragmentRootSourceContainer(binary)));
  }

}

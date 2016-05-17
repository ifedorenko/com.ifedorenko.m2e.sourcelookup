package com.ifedorenko.m2e.sourcelookup.internal.jdt;

import java.util.Collections;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaProjectSourceContainer;

public class ProjectDescriber extends IProjectSourceDescriber {

  @Override
  public void describeProject(IJavaProject project, IJavaProjectSourceDescription description) throws CoreException {
    description.addDependencies(getClasspath(project));
    description.addLocations(getOutputDirectories(project));
    description.addSourceContainerFactory(() -> Collections.singleton(new JavaProjectSourceContainer(project)));
  }

}

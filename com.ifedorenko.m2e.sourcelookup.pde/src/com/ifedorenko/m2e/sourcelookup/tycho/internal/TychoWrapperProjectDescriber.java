/*******************************************************************************
 * Copyright (c) 2011-2016 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/
package com.ifedorenko.m2e.sourcelookup.tycho.internal;

import java.util.Map;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaProjectSourceContainer;
import org.eclipse.jdt.launching.sourcelookup.containers.PackageFragmentRootSourceContainer;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.jdt.IClasspathEntryDescriptor;
import org.eclipse.m2e.jdt.IClasspathManager;
import org.eclipse.m2e.jdt.internal.ClasspathEntryDescriptor;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.sonatype.tycho.m2e.internal.EmbeddedArtifacts;

import com.ifedorenko.jdt.launching.sourcelookup.advanced.IWorkspaceProjectDescriber;


// TODO consider moving to a dedicated bundle or to m2e/tycho directly
// don't forget to remove this bundle dependency on m2e and m2e/tycho if you decide to move

@SuppressWarnings("restriction")
public class TychoWrapperProjectDescriber implements IWorkspaceProjectDescriber {

  private static final IWorkspaceRoot workspace = ResourcesPlugin.getWorkspace().getRoot();

  @Override
  public void describeProject(IJavaProject project, IJavaProjectSourceDescription description) throws CoreException {
    if (PluginRegistry.findModel(project.getProject()) == null) {
      return;
    }

    Map<ArtifactKey, String> artifacts = EmbeddedArtifacts.getEmbeddedArtifacts(project.getProject());
    if (artifacts.isEmpty()) {
      return;
    }

    for (IPackageFragmentRoot fragment : project.getPackageFragmentRoots()) {
      IClasspathEntryDescriptor entry = new ClasspathEntryDescriptor(fragment.getResolvedClasspathEntry());
      Map<String, String> attributes = entry.getClasspathAttributes();

      String g = attributes.get(IClasspathManager.GROUP_ID_ATTRIBUTE);
      String a = attributes.get(IClasspathManager.ARTIFACT_ID_ATTRIBUTE);
      String v = attributes.get(IClasspathManager.VERSION_ATTRIBUTE);
      String c = attributes.get(IClasspathManager.CLASSIFIER_ATTRIBUTE);
      if (artifacts.containsKey(new ArtifactKey(g, a, v, c))) {
        description.addSourceContainerFactory(() -> new PackageFragmentRootSourceContainer(fragment));
      }
    }

    for (String otherName : project.getRequiredProjectNames()) {
      IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getProject(workspace.getProject(otherName));
      if (facade != null && artifacts.containsKey(facade.getArtifactKey())) {
        description.addSourceContainerFactory(
            () -> new JavaProjectSourceContainer(JavaCore.create(workspace.getProject(otherName))));
      }
    }
  }
}

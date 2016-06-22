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

import java.util.Collections;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.launching.sourcelookup.containers.PackageFragmentRootSourceContainer;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.jdt.IClasspathEntryDescriptor;
import org.eclipse.m2e.jdt.IClasspathManager;
import org.eclipse.m2e.jdt.internal.ClasspathEntryDescriptor;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.sonatype.tycho.m2e.internal.EmbeddedArtifacts;

import com.ifedorenko.m2e.sourcelookup.internal.jdt.AbstractProjectSourceDescriber;

// TODO consider moving to a dedicated bundle or to m2e/tycho directly
// don't forget to remove this bundle dependency on m2e and m2e/tycho if you decide to move

@SuppressWarnings("restriction")
public class TychoWrapperProjectDescriber extends AbstractProjectSourceDescriber {

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
        description
            .addSourceContainerFactory(() -> Collections.singleton(new PackageFragmentRootSourceContainer(fragment)));
      }
    }
  }
}

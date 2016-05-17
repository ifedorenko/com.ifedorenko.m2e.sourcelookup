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
package com.ifedorenko.m2e.sourcelookup.pde.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.ArchiveSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.ExternalArchiveSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.FolderSourceContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.PDEClasspathContainer;

import com.ifedorenko.m2e.sourcelookup.internal.jdt.ISourceContainerResolver;

@SuppressWarnings("restriction")
public class PDESourceContainerResolver implements ISourceContainerResolver {
  private static final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

  @Override
  public Collection<ISourceContainer> resolveSourceContainers(File bundleLocation, IProgressMonitor monitor)
      throws CoreException {
    // PluginRegistry location-based, checksum-based lookup
    // P2 checksum-based lookup
    // bundle-symbolicName:version extracted from manifest

    Map<File, IPluginModelBase> projects = toLocations(PluginRegistry.getWorkspaceModels());

    IPluginModelBase project = projects.get(bundleLocation);

    Map<File, IPluginModelBase> externals = toLocations(PluginRegistry.getExternalModels());

    IPluginModelBase external = externals.get(bundleLocation);

    if (external == null) {
      return null;
    }

    List<ISourceContainer> containers = new ArrayList<>();

    // TODO probably want to reimplement, don't trust PDE to do exactly what I need
    // TODO does not work for org.eclipse.osgi bundle (messed up PDE metadata again?)
    for (IClasspathEntry cpe : PDEClasspathContainer.getExternalEntries(external)) {
      IPath sourcePath = cpe.getSourceAttachmentPath();
      if (sourcePath == null) {
        continue;
      }
      IResource resource = root.findMember(sourcePath);
      if (resource instanceof IFile) {
        containers.add(new ArchiveSourceContainer((IFile) resource, true));
      } else if (resource instanceof IFolder) {
        containers.add(new FolderSourceContainer((IFolder) resource, true));
      } else {
        File file = sourcePath.toFile();
        if (file.isFile()) {
          containers.add(new ExternalArchiveSourceContainer(file.getAbsolutePath(), true));
        }
      }
    }

    return containers;
  }

  private static Map<File, IPluginModelBase> toLocations(IPluginModelBase[] bundles) {
    Map<File, IPluginModelBase> locations = new HashMap<>();
    for (IPluginModelBase bundle : bundles) {
      locations.put(new File(bundle.getInstallLocation()), bundle);
    }
    return locations;
  }
}

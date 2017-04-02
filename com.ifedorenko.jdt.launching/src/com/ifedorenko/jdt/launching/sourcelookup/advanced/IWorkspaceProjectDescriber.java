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
package com.ifedorenko.jdt.launching.sourcelookup.advanced;

import java.io.File;
import java.util.Map;
import java.util.function.Supplier;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;

/**
 * Implementations of this interface describe workspace projects for the purpose of source lookup. Implementations are
 * registered with the advanced source lookup framework using
 * {@code org.eclipse.jdt.launching.workspaceProjectDescribers} extension point.
 * 
 * Workspace project runtime classes location are used to identify projects when performing source code lookup.
 * Depending on project type and application classloading mechanism used by the runtime, classes location can be project
 * output folders, one of project classpath entries (for PDE Binary Plug-In projects, for example) or some other
 * runtime-specific location, like OSGi bundle installation location.
 * 
 * The same workspace project can have different classes location if it is used with different runtime technologies. For
 * example, PDE Plug-In project classes will have bundle installation location when used by Equinox framework and
 * project output folder when used by standard java application. Note that different runtime technologies can coexist
 * within the same running JVM, like it is the case with Tycho build, where Equinox, Maven and standard Java APIs are
 * used side-by-side. For this reason multiple project describers can provide information about the same project and all
 * projects descriptions will be considered when performing source lookup.
 * 
 * @since 3.9
 * @provisional This is part of work in progress and can be changed, moved or removed without notice
 */
public interface IWorkspaceProjectDescriber {

  public static interface IJavaProjectSourceDescription {
    /**
     * Adds filesystem classes directories or jar files as reported by the runtime for project classes.
     * 
     * Some common examples:
     * <ul>
     * <li>for standard java projects this is project output locations
     * <li>for M2E Binary Maven projects, this is one of project claspath entries
     * <li>for PDE Plug-In projects used by Equinox, this is project bundle installation location
     * <li>for PDE Plug-In projects used plain java, this is still project output locations
     * </ul>
     * 
     */
    public void addLocation(File location);

    /**
     * Adds factory of source container(s) for the project itself, typically:
     * <ul>
     * <li>JavaProjectSourceContainer for normal projects with sources folders
     * <li>PackageFragmentRootSourceContainer for PDE and M2E binary projects
     * </ul>
     * 
     * In some cases one project will have multiple associated source container. For example, Binary Maven project that
     * represents an "uber" jar will have a source container factory for each jar included in the uber jar.
     */
    public void addSourceContainerFactory(Supplier<ISourceContainer> factory);

    /**
     * Adds runtime classes location of project dependencies and their corresponding package fragment roots, typically
     * <ul>
     * <li>for standard java application, this is dependency jar or classes directory
     * <li>for equinox, this is dependency bundle location
     * </ul>
     */
    public void addDependencies(Map<File, IPackageFragmentRoot> dependencies);
  }

  /**
   * Populate the given description with the given project's description.
   */
  public void describeProject(IJavaProject project, IJavaProjectSourceDescription description) throws CoreException;

}

/*******************************************************************************
 * Copyright (c) 2012 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/
package com.ifedorenko.m2e.sourcelookup.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaProjectSourceContainer;
import org.eclipse.jdt.launching.sourcelookup.containers.PackageFragmentRootSourceContainer;

import com.ifedorenko.m2e.binaryproject.BinaryProjectPlugin;

public class JavaProjectSources
    implements IElementChangedListener
{
    private static class JavaProjectInfo
    {
        private final IJavaProject project;

        private final Map<File, IPackageFragmentRoot> classpath;

        public JavaProjectInfo( IJavaProject project, Map<File, IPackageFragmentRoot> classpath )
        {
            this.project = project;
            this.classpath = Collections.unmodifiableMap( classpath );
        }

        public IJavaProject getJavaProject()
        {
            return project;
        }

        public IPackageFragmentRoot getPackageFragmentRoot( File location )
        {
            return classpath.get( location );
        }
    }

    private final Object lock = new Object()
    {
    };

    /**
     * Maps project output location to project info.
     */
    private final Map<File, JavaProjectInfo> locations = new HashMap<File, JavaProjectInfo>();

    private Map<IJavaProject, Set<File>> projects = new HashMap<IJavaProject, Set<File>>();

    private final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

    public ISourceContainer getProjectSourceContainer( File location )
    {
        JavaProjectInfo info = getJavaProject( location );

        if ( info != null )
        {
            return getProjectSourceContainer( info.getJavaProject() );
        }

        return null;
    }

    public ISourceContainer getContextSourceContainer( File location, IStackFrame[] stack )
        throws DebugException
    {
        for ( IStackFrame frame : stack )
        {
            File frameLocation = JDIHelpers.getLocation( frame );
            if ( frameLocation == null )
            {
                continue;
            }

            JavaProjectInfo info = getJavaProject( frameLocation );

            if ( info == null )
            {
                continue;
            }

            final IPackageFragmentRoot fragment = info.getPackageFragmentRoot( location );
            if ( fragment != null )
            {
                return new PackageFragmentRootSourceContainer( fragment );
            }
        }
        return null;
    }

    public void initialize()
        throws CoreException
    {
        JavaCore.addElementChangedListener( this );

        final IJavaModel javaModel = JavaCore.create( root );
        final IJavaProject[] javaProjects = javaModel.getJavaProjects();
        synchronized ( lock )
        {
            // not sure if synchronized is a good idea here
            // on one hand, this is the easiest way to guarantee async change events won't corrupt cache
            // but it can result in deadlocks if java model and change event delivery is synchronized internally.
            // note to self: if deadlocks, change events should be processed by background thread
            for ( IJavaProject project : javaProjects )
            {
                addJavaProject( project );
            }
        }
    }

    public void close()
    {
        JavaCore.removeElementChangedListener( this );
    }

    private void addJavaProject( IJavaProject project )
        throws CoreException
    {
        if ( project != null )
        {
            final Map<File, IPackageFragmentRoot> classpath = new LinkedHashMap<File, IPackageFragmentRoot>();
            for ( IPackageFragmentRoot fragment : project.getPackageFragmentRoots() )
            {
                if ( fragment.getKind() == IPackageFragmentRoot.K_BINARY && fragment.getSourceAttachmentPath() != null )
                {
                    File classpathLocation;
                    if ( fragment.isExternal() )
                    {
                        classpathLocation = fragment.getPath().toFile();
                    }
                    else
                    {
                        classpathLocation = toFile( fragment.getPath() );
                    }
                    if ( classpathLocation != null )
                    {
                        classpath.put( classpathLocation, fragment );
                    }
                }
            }

            final JavaProjectInfo projectInfo = new JavaProjectInfo( project, classpath );

            final Set<File> projectLocations = new HashSet<File>();

            final String jarLocation = project.getProject().getPersistentProperty( BinaryProjectPlugin.QNAME_JAR );
            if ( jarLocation != null )
            {
                // maven binary project
                projectLocations.add( new File( jarLocation ) );
            }
            else
            {
                // regular project
                projectLocations.add( toFile( project.getOutputLocation() ) );
                for ( IClasspathEntry cpe : project.getRawClasspath() )
                {
                    if ( cpe.getEntryKind() == IClasspathEntry.CPE_SOURCE )
                    {
                        projectLocations.add( toFile( cpe.getOutputLocation() ) );
                    }
                }
            }

            synchronized ( lock )
            {
                projects.put( project, projectLocations );
                for ( File projectLocation : projectLocations )
                {
                    locations.put( projectLocation, projectInfo );
                }
            }
        }
    }

    private void removeJavaProject( IJavaProject project )
    {
        if ( project != null )
        {
            synchronized ( lock )
            {
                Set<File> projectLocations = projects.remove( project );
                if ( projectLocations != null )
                {
                    for ( File projectLocation : projectLocations )
                    {
                        locations.remove( projectLocation );
                    }
                }
            }
        }
    }

    private JavaProjectInfo getJavaProject( File location )
    {
        synchronized ( lock )
        {
            return locations.get( location );
        }
    }

    private File toFile( IPath workspacePath )
    {
        IResource resource = root.findMember( workspacePath );
        if ( resource != null )
        {
            return resource.getLocation().toFile();
        }
        return null;
    }

    static ISourceContainer getProjectSourceContainer( IJavaProject javaProject )
    {
        List<ISourceContainer> containers = new ArrayList<ISourceContainer>();

        boolean hasSources = false;

        try
        {
            for ( IClasspathEntry cpe : javaProject.getRawClasspath() )
            {
                switch ( cpe.getEntryKind() )
                {
                    case IClasspathEntry.CPE_SOURCE:
                        hasSources = true;
                        break;
                    case IClasspathEntry.CPE_LIBRARY:
                        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
                        IResource lib = workspaceRoot.findMember( cpe.getPath() );
                        IPackageFragmentRoot fragmentRoot;
                        if ( lib != null )
                        {
                            fragmentRoot = javaProject.getPackageFragmentRoot( lib );
                        }
                        else
                        {
                            fragmentRoot = javaProject.getPackageFragmentRoot( cpe.getPath().toOSString() );
                        }
                        containers.add( new PackageFragmentRootSourceContainer( fragmentRoot ) );
                        break;
                }
            }
        }
        catch ( JavaModelException e )
        {
            // ignore... maybe log
        }

        if ( hasSources )
        {
            containers.add( 0, new JavaProjectSourceContainer( javaProject ) );
        }

        if ( containers.isEmpty() )
        {
            return null;
        }

        if ( containers.size() == 1 )
        {
            return containers.get( 0 );
        }

        return new CompositeSourceContainer( containers );
    }

    @Override
    public void elementChanged( ElementChangedEvent event )
    {
        try
        {
            final Set<IJavaProject> remove = new HashSet<IJavaProject>();
            final Set<IJavaProject> add = new HashSet<IJavaProject>();

            processDelta( event.getDelta(), remove, add );

            for ( IJavaProject project : remove )
            {
                removeJavaProject( project );
            }
            for ( IJavaProject project : add )
            {
                addJavaProject( project );
            }
        }
        catch ( CoreException e )
        {
            // maybe do something about it?
        }
    }

    private void processDelta( final IJavaElementDelta delta, Set<IJavaProject> remove, Set<IJavaProject> add )
        throws CoreException
    {
        final IJavaElement element = delta.getElement();
        final int kind = delta.getKind();
        switch ( element.getElementType() )
        {
            case IJavaElement.JAVA_MODEL:
                processChangedChildren( delta, remove, add );
                break;
            case IJavaElement.JAVA_PROJECT:
                switch ( kind )
                {
                    case IJavaElementDelta.REMOVED:
                        remove.add( (IJavaProject) element );
                        break;
                    case IJavaElementDelta.ADDED:
                        add.add( (IJavaProject) element );
                        break;
                    case IJavaElementDelta.CHANGED:
                        switch ( delta.getFlags() )
                        {
                            case IJavaElementDelta.F_CLOSED:
                                remove.add( (IJavaProject) element );
                                break;
                            case IJavaElementDelta.F_OPENED:
                                add.add( (IJavaProject) element );
                                break;
                        }
                        break;
                }
                processChangedChildren( delta, remove, add );
                break;
            case IJavaElement.PACKAGE_FRAGMENT_ROOT:
                remove.add( element.getJavaProject() );
                add.add( element.getJavaProject() );
                break;
        }
    }

    private void processChangedChildren( IJavaElementDelta delta, Set<IJavaProject> remove, Set<IJavaProject> add )
        throws CoreException
    {
        for ( IJavaElementDelta childDelta : delta.getAffectedChildren() )
        {
            processDelta( childDelta, remove, add );
        }
    }

    public ISourceContainer getAnySourceContainer( File location )
    {
        synchronized ( lock )
        {
            for ( JavaProjectInfo project : locations.values() )
            {
                IPackageFragmentRoot fragmentRoot = project.getPackageFragmentRoot( location );
                if ( fragmentRoot != null )
                {
                    return new PackageFragmentRootSourceContainer( fragmentRoot );
                }
            }
        }
        return null;
    }
}

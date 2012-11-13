/*******************************************************************************
 * Copyright (c) 2011-2012 Igor Fedorenko
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;
import org.eclipse.debug.core.sourcelookup.containers.ExternalArchiveSourceContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaProjectSourceContainer;
import org.eclipse.jdt.launching.sourcelookup.containers.PackageFragmentRootSourceContainer;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.IMavenProjectChangedListener;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;

public class SourceLookupParticipant
    implements ISourceLookupParticipant, IMavenProjectChangedListener
{

    private ISourceLookupDirector director;

    private final Map<String, ISourceContainer> containers = new HashMap<String, ISourceContainer>();

    public void init( ISourceLookupDirector director )
    {
        MavenPlugin.getMavenProjectRegistry().addMavenProjectChangedListener( this );
        this.director = director;
    }

    public Object[] findSourceElements( Object fElement )
        throws CoreException
    {
        String location = JDIHelpers.getLocation( fElement );

        if ( location == null )
        {
            return null;
        }

        ISourceContainer container;
        synchronized ( this.containers )
        {
            if ( this.containers.containsKey( location ) )
            {
                container = this.containers.get( location );
            }
            else
            {
                container = createSourceContainer( fElement, location );

                this.containers.put( location, container );
            }
        }

        if ( container == null )
        {
            return null;
        }

        String sourcePath = JDIHelpers.getSourcePath( fElement );
        if ( sourcePath == null )
        {
            // can't really happen
            return null;
        }

        return container.findSourceElements( sourcePath );
    }

    protected ISourceContainer createSourceContainer( final Object fElement, final String location )
        throws CoreException
    {
        List<ISourceContainer> containers = null;

        // look among workspace projects first
        for ( IMavenProjectFacade facade : MavenPlugin.getMavenProjectRegistry().getProjects() )
        {
            if ( isLocationEquals( facade.getOutputLocation(), location )
                || isLocationEquals( facade.getTestOutputLocation(), location ) )
            {
                return new JavaProjectSourceContainer( JavaCore.create( facade.getProject() ) );
            }
        }

        containers = new PomPropertiesScanner<ISourceContainer>()
        {
            @Override
            protected ISourceContainer visitArtifact( ArtifactKey artifact )
                throws CoreException
            {
                String groupId = artifact.getGroupId();
                String artifactId = artifact.getArtifactId();
                String version = artifact.getVersion();

                IMaven maven = MavenPlugin.getMaven();

                // check in local repository first
                ArtifactRepository localRepository = maven.getLocalRepository();
                String relPath =
                    maven.getArtifactPath( localRepository, groupId, artifactId, version, "jar", "sources" );
                File file = new File( localRepository.getBasedir(), relPath );
                if ( file.isFile() && file.canRead() )
                {
                    return new ExternalArchiveSourceContainer( file.getAbsolutePath(), true );
                }
                else
                {
                    List<ArtifactRepository> repositories = new ArrayList<ArtifactRepository>();
                    repositories.addAll( maven.getArtifactRepositories() );
                    repositories.addAll( maven.getPluginArtifactRepositories() );
                    if ( !maven.isUnavailable( groupId, artifactId, version, "jar", "sources", repositories ) )
                    {
                        Runnable callback = new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                refreshContainer( location );
                                director.clearSourceElements( fElement );
                                if ( fElement instanceof DebugElement )
                                {
                                    // this is apparently needed to flush StackFrameSourceDisplayAdapter cache
                                    ( (DebugElement) fElement ).fireChangeEvent( DebugEvent.CONTENT );
                                }
                            }
                        };
                        SourceLookupActivator.scheduleDownload( new ArtifactKey( groupId, artifactId, version,
                                                                                 "sources" ), callback );
                    }
                    return null;
                }
            }

            @Override
            protected ISourceContainer visitMavenProject( IMavenProjectFacade mavenProject )
            {
                IJavaProject javaProject = JavaCore.create( mavenProject.getProject() );

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
            protected ISourceContainer visitProject( IProject project )
            {
                return new JavaProjectSourceContainer( JavaCore.create( project ) );
            }
        }.scan( location );

        if ( containers.isEmpty() )
        {
            return null;
        }

        for ( ISourceContainer child : containers )
        {
            child.init( director );
        }

        if ( containers.size() == 1 )
        {
            return containers.get( 0 );
        }

        return new CompositeSourceContainer( containers );
    }

    private boolean isLocationEquals( IPath workspaceLocation, String url )
    {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IFolder folder = root.getFolder( workspaceLocation );
        if ( folder == null )
        {
            return false;
        }
        IPath location = folder.getLocation();
        return location != null && location.equals( UrlUtils.toPath( url ) );
    }

    public String getSourceName( Object object )
        throws CoreException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void dispose()
    {
        disposeContainers();
        MavenPlugin.getMavenProjectRegistry().removeMavenProjectChangedListener( this );
    }

    protected void disposeContainers()
    {
        synchronized ( containers )
        {
            for ( ISourceContainer container : containers.values() )
            {
                if ( container != null ) // possible for non-maven jars
                {
                    container.dispose();
                }
            }
            containers.clear();
        }
    }

    public void sourceContainersChanged( ISourceLookupDirector director )
    {
        disposeContainers();
    }

    @Override
    public void mavenProjectChanged( MavenProjectChangedEvent[] events, IProgressMonitor monitor )
    {
        disposeContainers();
    }

    protected void refreshContainer( String location )
    {
        synchronized ( containers )
        {
            ISourceContainer container = containers.remove( location );
            if ( container != null )
            {
                container.dispose();
            }
        }
    }

}

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
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;
import org.eclipse.debug.core.sourcelookup.containers.ExternalArchiveSourceContainer;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaProjectSourceContainer;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.IMavenProjectFacade;

class SourceLookupParticipant
    implements ISourceLookupParticipant
{

    private ISourceLookupDirector director;

    private Map<String, ISourceContainer> containers = new HashMap<String, ISourceContainer>();

    public void init( ISourceLookupDirector director )
    {
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
        if ( this.containers.containsKey( location ) )
        {
            container = this.containers.get( location );
        }
        else
        {
            container = createSourceContainer( location );

            this.containers.put( location, container );
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

    protected ISourceContainer createSourceContainer( String location )
        throws CoreException
    {
        List<ISourceContainer> containers = new PomPropertiesScanner<ISourceContainer>()
        {
            @Override
            protected ISourceContainer visitGAV( String groupId, String artifactId, String version )
                throws CoreException
            {
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
                        SourceLookupActivator.scheduleDownload( new ArtifactKey( groupId, artifactId, version,
                                                                                 "sources" ) );
                    }
                    return null;
                }
            }

            @Override
            protected ISourceContainer visitMavenProject( IMavenProjectFacade mavenProject )
            {
                return new JavaProjectSourceContainer( JavaCore.create( mavenProject.getProject() ) );
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

    public String getSourceName( Object object )
        throws CoreException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void dispose()
    {
        for ( ISourceContainer container : containers.values() )
        {
            container.dispose();
        }
        containers.clear();
    }

    public void sourceContainersChanged( ISourceLookupDirector director )
    {
        // TODO Auto-generated method stub

    }

}

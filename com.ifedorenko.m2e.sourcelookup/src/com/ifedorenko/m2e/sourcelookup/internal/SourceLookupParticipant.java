/*******************************************************************************
 * Copyright (c) 2011 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/
package com.ifedorenko.m2e.sourcelookup.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;
import org.eclipse.debug.core.sourcelookup.containers.ExternalArchiveSourceContainer;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.debug.core.IJavaObject;
import org.eclipse.jdt.debug.core.IJavaReferenceType;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaType;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaProjectSourceContainer;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;

public class SourceLookupParticipant
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
        // jdt debug boilerplate borrowed from
        // org.eclipse.pde.internal.launching.sourcelookup.PDESourceLookupQuery.run()

        try
        {
            IJavaReferenceType declaringType = null;
            String sourcePath = null;
            if ( fElement instanceof IJavaStackFrame )
            {
                IJavaStackFrame stackFrame = (IJavaStackFrame) fElement;
                declaringType = stackFrame.getReferenceType();
                // under JSR 45 source path from the stack frame is more precise than anything derived from the type:
                sourcePath = stackFrame.getSourcePath();
            }
            else if ( fElement instanceof IJavaObject )
            {
                IJavaType javaType = ( (IJavaObject) fElement ).getJavaType();
                if ( javaType instanceof IJavaReferenceType )
                {
                    declaringType = (IJavaReferenceType) javaType;
                }
            }
            else if ( fElement instanceof IJavaReferenceType )
            {
                declaringType = (IJavaReferenceType) fElement;
            }
            if ( declaringType != null )
            {
                String[] locations = declaringType.getSourceNames( "m2e" );

                if ( locations == null || locations.length < 1 )
                {
                    return null;
                }

                String location = locations[0];

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

                if ( container != null )
                {
                    if ( sourcePath == null )
                    {
                        String declaringTypeName = declaringType.getName();
                        String[] sourcePaths = declaringType.getSourcePaths( null );
                        if ( sourcePaths != null )
                        {
                            sourcePath = sourcePaths[0];
                        }
                        if ( sourcePath == null )
                        {
                            sourcePath = generateSourceName( declaringTypeName );
                        }
                    }

                    return container.findSourceElements( sourcePath );
                }

            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        return null;
    }

    protected ISourceContainer createSourceContainer( String location )
        throws CoreException
    {
        List<ISourceContainer> containers = new ArrayList<ISourceContainer>();
        for ( Properties pomProperties : loadPomProperties( location ) )
        {
            String projectName = pomProperties.getProperty( "m2e.projectName" );
            File projectLocation = getFile( pomProperties, "m2e.projectLocation" );
            IProject project =
                projectName != null ? ResourcesPlugin.getWorkspace().getRoot().getProject( projectName ) : null;
            if ( project != null && project.getLocation().toFile().equals( projectLocation ) )
            {
                containers.add( new JavaProjectSourceContainer( JavaCore.create( project ) ) );
            }
            else
            {
                String groupId = pomProperties.getProperty( "groupId" );
                String artifactId = pomProperties.getProperty( "artifactId" );
                String versionId = pomProperties.getProperty( "version" );
                IMavenProjectRegistry projectRegistry = MavenPlugin.getMavenProjectRegistry();
                IMavenProjectFacade mavenProject = projectRegistry.getMavenProject( groupId, artifactId, versionId );
                if ( mavenProject != null )
                {
                    containers.add( new JavaProjectSourceContainer( JavaCore.create( mavenProject.getProject() ) ) );
                }
                else
                {
                    IMaven maven = MavenPlugin.getMaven();

                    // check in local repository first
                    ArtifactRepository localRepository = maven.getLocalRepository();
                    String relPath =
                        maven.getArtifactPath( localRepository, groupId, artifactId, versionId, "jar", "sources" );
                    File file = new File( localRepository.getBasedir(), relPath );
                    if ( file.isFile() && file.canRead() )
                    {
                        containers.add( new ExternalArchiveSourceContainer( file.getAbsolutePath(), true ) );
                    }
                    else
                    {
                        List<ArtifactRepository> repositories = new ArrayList<ArtifactRepository>();
                        repositories.addAll( maven.getArtifactRepositories() );
                        repositories.addAll( maven.getPluginArtifactRepositories() );
                        if ( !maven.isUnavailable( groupId, artifactId, versionId, "jar", "sources", repositories ) )
                        {
                            SourceLookupActivator.scheduleDownload( new ArtifactKey( groupId, artifactId, versionId,
                                                                                     "sources" ) );
                        }
                    }
                }
            }
        }

        // TODO check with nexus index

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

    private List<Properties> loadPomProperties( String urlStr )
    {
        List<Properties> result = new ArrayList<Properties>();
        try
        {
            URL url = new URL( urlStr );

            if ( "file".equals( url.getProtocol() ) )
            {

                File file = new File( url.getPath() );
                if ( file.isDirectory() )
                {
                    getPomProperties( new File( file, "META-INF/maven" ), result );
                }
                else if ( file.isFile() )
                {
                    JarFile jar = new JarFile( file );
                    try
                    {
                        getPomProperties( jar, result );
                    }
                    finally
                    {
                        jar.close();
                    }
                }
            }
        }
        catch ( Exception ex )
        {
            // fall through
        }
        return result;
    }

    private void getPomProperties( JarFile jar, List<Properties> result )
        throws IOException
    {
        Enumeration<JarEntry> entries = jar.entries();
        while ( entries.hasMoreElements() )
        {
            JarEntry entry = entries.nextElement();
            if ( !entry.isDirectory() )
            {
                String name = entry.getName();
                if ( name.startsWith( "META-INF/maven" ) && name.endsWith( "pom.properties" ) )
                {
                    InputStream is = jar.getInputStream( entry );
                    try
                    {
                        Properties properties = new Properties();
                        properties.load( is );
                        // TODO validate properties and path match
                        result.add( properties );
                    }
                    finally
                    {
                        IOUtil.close( is );
                    }
                }
            }
        }
    }

    private File getFile( Properties properties, String name )
    {
        String value = properties.getProperty( name );
        return value != null ? new File( value ) : null;
    }

    private void getPomProperties( File dir, List<Properties> result )
    {
        File[] files = dir.listFiles();
        if ( files == null )
        {
            return;
        }
        for ( File file : files )
        {
            if ( file.isDirectory() )
            {
                getPomProperties( file, result );
            }
            else if ( file.isFile() && "pom.properties".equals( file.getName() ) )
            {
                try
                {
                    InputStream is = new BufferedInputStream( new FileInputStream( file ) );
                    try
                    {
                        Properties properties = new Properties();
                        properties.load( is );
                        // TODO validate properties and path match
                        result.add( properties );
                    }
                    finally
                    {
                        IOUtil.close( is );
                    }
                }
                catch ( IOException e )
                {
                    // ignore
                }
            }
        }
    }

    // copy&paste from org.eclipse.pde.internal.launching.sourcelookup.PDESourceLookupQuery.generateSourceName(String)
    private static String generateSourceName( String qualifiedTypeName )
    {
        int index = qualifiedTypeName.indexOf( '$' );
        if ( index >= 0 )
            qualifiedTypeName = qualifiedTypeName.substring( 0, index );
        return qualifiedTypeName.replace( '.', File.separatorChar ) + ".java"; //$NON-NLS-1$
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

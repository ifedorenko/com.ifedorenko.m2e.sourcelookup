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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;

/**
 * Scans given location for pom.properties and extracts IProject, IMavenProjectFacade or GAV.
 */
abstract class PomPropertiesScanner<T>
{
    public List<T> scan( String location )
        throws CoreException
    {
        IMavenProjectRegistry projectRegistry = MavenPlugin.getMavenProjectRegistry();

        List<T> result = new ArrayList<T>();
        for ( Properties pomProperties : loadPomProperties( location ) )
        {
            T t;

            String projectName = pomProperties.getProperty( "m2e.projectName" );
            File projectLocation = getFile( pomProperties, "m2e.projectLocation" );
            IProject project =
                projectName != null ? ResourcesPlugin.getWorkspace().getRoot().getProject( projectName ) : null;
            if ( project != null && project.getLocation().toFile().equals( projectLocation ) )
            {
                IMavenProjectFacade mavenProject = projectRegistry.getProject( project );

                if ( mavenProject != null )
                {
                    t = visitMavenProject( mavenProject );
                }
                else
                {
                    t = visitProject( project );
                }
            }
            else
            {
                String groupId = pomProperties.getProperty( "groupId" );
                String artifactId = pomProperties.getProperty( "artifactId" );
                String version = pomProperties.getProperty( "version" );
                IMavenProjectFacade mavenProject = projectRegistry.getMavenProject( groupId, artifactId, version );
                if ( mavenProject != null )
                {
                    t = visitMavenProject( mavenProject );
                }
                else
                {
                    t = visitGAV( groupId, artifactId, version );
                }
            }

            if ( t != null )
            {
                result.add( t );
            }
        }

        // TODO check with nexus index

        return result;
    }

    protected abstract T visitGAV( String groupId, String artifactId, String version )
        throws CoreException;

    protected abstract T visitMavenProject( IMavenProjectFacade mavenProject );

    protected abstract T visitProject( IProject project );

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

    private File getFile( Properties properties, String name )
    {
        String value = properties.getProperty( name );
        return value != null ? new File( value ) : null;
    }
}

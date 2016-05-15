package com.ifedorenko.m2e.sourcelookup.internal.launch;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.ExternalArchiveSourceContainer;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaProjectSourceContainer;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.internal.index.IIndex;
import org.eclipse.m2e.core.internal.index.IndexedArtifactFile;
import org.eclipse.m2e.core.internal.index.nexus.CompositeIndex;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ifedorenko.m2e.sourcelookup.internal.jdt.ISourceContainerResolver;

@SuppressWarnings( "restriction" )
public class MavenSourceContainerResolver
    implements ISourceContainerResolver
{

    @Override
    public Collection<ISourceContainer> resolveSourceContainers( File classesLocation, IProgressMonitor monitor )
    {
        // checksum-based lookup in nexus index
        // checksum-based lookup in central
        // GAV extracted from pom.properties

        Collection<ArtifactKey> classesArtifacts = identifyNexusIndexer( classesLocation );
        if ( classesArtifacts == null )
        {
            classesArtifacts = identifyCentralSearch( classesLocation );
        }
        if ( classesArtifacts == null )
        {
            classesArtifacts = scanPomProperties( classesLocation );
        }
        if ( classesArtifacts == null )
        {
            return null;
        }

        List<ISourceContainer> result = new ArrayList<>();
        for ( ArtifactKey classesArtifact : classesArtifacts )
        {
            ISourceContainer container = resovleSourceContainer( classesArtifact, monitor );
            if ( container != null )
            {
                result.add( container );
            }
        }
        return result;
    }

    private Collection<ArtifactKey> scanPomProperties( File classesLocation )
    {
        // TODO Auto-generated method stub
        return null;
    }

    protected Collection<ArtifactKey> identifyNexusIndexer( File file )
    {
        try
        {
            IIndex index = MavenPlugin.getIndexManager().getAllIndexes();

            List<IndexedArtifactFile> identified;
            if ( index instanceof CompositeIndex )
            {
                identified = ( (CompositeIndex) index ).identifyAll( file );
            }
            else
            {
                IndexedArtifactFile indexed = index.identify( file );
                if ( indexed != null )
                {
                    identified = Collections.singletonList( indexed );
                }
                else
                {
                    identified = Collections.emptyList();
                }
            }

            for ( IndexedArtifactFile indexed : identified )
            {
                if ( indexed.sourcesExists == IIndex.PRESENT )
                {
                    return Collections.singleton( indexed.getArtifactKey() );
                }
            }
        }
        catch ( CoreException e )
        {
            // TODO maybe log, but ignore otherwise
        }

        return null;
    }

    protected Collection<ArtifactKey> identifyCentralSearch( File file )
    {
        try
        {
            String sha1 = Files.hash( file, Hashing.sha1() ).toString(); // TODO use Locations for caching
            URL url = new URL( "https://search.maven.org/solrsearch/select?q=1:" + sha1 );
            try (InputStreamReader reader = new InputStreamReader( url.openStream(), Charsets.UTF_8 ))
            {
                Collection<ArtifactKey> result = new ArrayList<>();
                JsonObject container = new Gson().fromJson( reader, JsonObject.class );
                JsonArray docs = container.get( "response" ).getAsJsonObject().get( "docs" ).getAsJsonArray();
                for ( int i = 0; i < docs.size(); i++ )
                {
                    JsonObject doc = docs.get( i ).getAsJsonObject();
                    String g = doc.get( "g" ).getAsString();
                    String a = doc.get( "a" ).getAsString();
                    String v = doc.get( "v" ).getAsString();
                    result.add( new ArtifactKey( g, a, v, null ) );
                }
                return !result.isEmpty() ? result : null;
            }
        }
        catch ( IOException e )
        {
            // TODO maybe log, ignore otherwise
        }
        return null;
    }

    private ISourceContainer resovleSourceContainer( ArtifactKey artifact, IProgressMonitor monitor )
    {
        String groupId = artifact.getGroupId();
        String artifactId = artifact.getArtifactId();
        String version = artifact.getVersion();

        IMaven maven = MavenPlugin.getMaven();
        IMavenProjectRegistry projectRegistry = MavenPlugin.getMavenProjectRegistry();

        IMavenProjectFacade mavenProject = projectRegistry.getMavenProject( groupId, artifactId, version );
        if ( mavenProject != null )
        {
            return new JavaProjectSourceContainer( JavaCore.create( mavenProject.getProject() ) );
        }

        try
        {
            List<ArtifactRepository> repositories = new ArrayList<ArtifactRepository>();
            repositories.addAll( maven.getArtifactRepositories() );
            repositories.addAll( maven.getPluginArtifactRepositories() );

            if ( !maven.isUnavailable( groupId, artifactId, version, "jar", "sources", repositories ) )
            {
                Artifact resolve = maven.resolve( groupId, artifactId, version, "jar", "sources", null, monitor );

                return new ExternalArchiveSourceContainer( resolve.getFile().getAbsolutePath(), true );
            }
        }
        catch ( CoreException e )
        {
            // TODO maybe log, ignore otherwise
        }

        return null;
    }
}

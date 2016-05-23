package com.ifedorenko.m2e.sourcelookup.internal.launch;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
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
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;

import com.ifedorenko.m2e.sourcelookup.internal.jdt.ISourceContainerResolver;

public class MavenSourceContainerResolver implements ISourceContainerResolver {

  private static final MavenArtifactIdentifierer INDENTIFIERER = new MavenArtifactIdentifierer();

  @Override
  public Collection<ISourceContainer> resolveSourceContainers(File classesLocation, IProgressMonitor monitor) {
    Collection<ArtifactKey> classesArtifacts = INDENTIFIERER.identify(classesLocation, monitor);

    if (classesArtifacts == null) {
      return null;
    }

    List<ISourceContainer> result = new ArrayList<>();
    for (ArtifactKey classesArtifact : classesArtifacts) {
      ISourceContainer container = resovleSourceContainer(classesArtifact, monitor);
      if (container != null) {
        result.add(container);
      }
    }
    return result;
  }

  private ISourceContainer resovleSourceContainer(ArtifactKey artifact, IProgressMonitor monitor) {
    String groupId = artifact.getGroupId();
    String artifactId = artifact.getArtifactId();
    String version = artifact.getVersion();

    IMaven maven = MavenPlugin.getMaven();
    IMavenProjectRegistry projectRegistry = MavenPlugin.getMavenProjectRegistry();

    IMavenProjectFacade mavenProject = projectRegistry.getMavenProject(groupId, artifactId, version);
    if (mavenProject != null) {
      return new JavaProjectSourceContainer(JavaCore.create(mavenProject.getProject()));
    }

    try {
      List<ArtifactRepository> repositories = new ArrayList<ArtifactRepository>();
      repositories.addAll(maven.getArtifactRepositories());
      repositories.addAll(maven.getPluginArtifactRepositories());

      if (!maven.isUnavailable(groupId, artifactId, version, "jar", "sources", repositories)) {
        Artifact resolve = maven.resolve(groupId, artifactId, version, "jar", "sources", null, monitor);

        return new ExternalArchiveSourceContainer(resolve.getFile().getAbsolutePath(), true);
      }
    } catch (CoreException e) {
      // TODO maybe log, ignore otherwise
    }

    return null;
  }
}

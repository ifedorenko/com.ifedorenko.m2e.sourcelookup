
package com.ifedorenko.m2e.sourcelookup.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
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
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;


public class SourceLookupParticipant implements ISourceLookupParticipant {

  private ISourceLookupDirector director;

  private Map<String, ISourceContainer> containers = new HashMap<String, ISourceContainer>();

  public void init(ISourceLookupDirector director) {
    this.director = director;
  }

  public Object[] findSourceElements(Object fElement) throws CoreException {
    // jdt debug boilerplate borrowed from org.eclipse.pde.internal.launching.sourcelookup.PDESourceLookupQuery.run()

    try {

      IJavaReferenceType declaringType = null;
      String sourcePath = null;
      if(fElement instanceof IJavaStackFrame) {
        IJavaStackFrame stackFrame = (IJavaStackFrame) fElement;
        declaringType = stackFrame.getReferenceType();
        // under JSR 45 source path from the stack frame is more precise than anything derived from the type: 
        sourcePath = stackFrame.getSourcePath();
      } else if(fElement instanceof IJavaObject) {
        IJavaType javaType = ((IJavaObject) fElement).getJavaType();
        if(javaType instanceof IJavaReferenceType) {
          declaringType = (IJavaReferenceType) javaType;
        }
      } else if(fElement instanceof IJavaReferenceType) {
        declaringType = (IJavaReferenceType) fElement;
      }
      if(declaringType != null) {
        String[] locations = declaringType.getSourceNames("m2e");

        if(locations == null || locations.length < 1) {
          return null;
        }

        String location = locations[0];

        ISourceContainer container = null;

        if(containers.containsKey(location)) {
          container = containers.get(location);
        } else {
          Properties pomProperties = loadPomProperties(location);
          if(pomProperties != null) {
            String projectName = pomProperties.getProperty("m2e.projectName");
            File projectLocation = getFile(pomProperties, "m2e.projectLocation");
            IProject project = projectName != null ? ResourcesPlugin.getWorkspace().getRoot().getProject(projectName)
                : null;
            if(project != null && project.getLocation().toFile().equals(projectLocation)) {
              container = new JavaProjectSourceContainer(JavaCore.create(project));
            } else {
              String groupId = pomProperties.getProperty("groupId");
              String artifactId = pomProperties.getProperty("artifactId");
              String versionId = pomProperties.getProperty("version");
              IMavenProjectRegistry projectRegistry = MavenPlugin.getMavenProjectRegistry();
              IMavenProjectFacade mavenProject = projectRegistry.getMavenProject(groupId, artifactId, versionId);
              if(mavenProject != null) {
                container = new JavaProjectSourceContainer(JavaCore.create(mavenProject.getProject()));
              } else {
                IMaven maven = MavenPlugin.getMaven();
                List<ArtifactRepository> repositories = maven.getArtifactRepositories();
                try {
                  Artifact artifact = maven.resolve(groupId, artifactId, versionId, "jar", "sources", repositories,
                      new NullProgressMonitor());
                  container = new ExternalArchiveSourceContainer(artifact.getFile().getAbsolutePath(), true);
                } catch(CoreException e) {
                  // fall through
                }
              }
            }
          } else {
            // TODO check with nexus index
          }
          containers.put(location, container);

          if(container != null) {
            container.init(director);
          }
        }

        if(container != null) {
          if(sourcePath == null) {
            String declaringTypeName = declaringType.getName();
            String[] sourcePaths = declaringType.getSourcePaths(null);
            if(sourcePaths != null) {
              sourcePath = sourcePaths[0];
            }
            if(sourcePath == null) {
              sourcePath = generateSourceName(declaringTypeName);
            }
          }

          return container.findSourceElements(sourcePath);
        }

      }
    } catch(Exception e) {
      e.printStackTrace();
    }

    return null;
  }

  private Properties loadPomProperties(String urlStr) {
    try {
      URL url = new URL(urlStr);

      if("file".equals(url.getProtocol())) {
        File file = new File(url.getPath());
        if(file.isDirectory()) {
          return getPomProperties(new File(file, "META-INF/maven"));
        } else if(file.isFile()) {
          JarFile jar = new JarFile(file);
          try {
            return getPomProperties(jar);
          } finally {
            jar.close();
          }
        }
      }
    } catch(Exception ex) {
      // fall through
    }
    return null;
  }

  private Properties getPomProperties(JarFile jar) throws IOException {
    Enumeration<JarEntry> entries = jar.entries();
    while(entries.hasMoreElements()) {
      JarEntry entry = entries.nextElement();
      if(!entry.isDirectory()) {
        String name = entry.getName();
        if(name.startsWith("META-INF/maven") && name.endsWith("pom.properties")) {
          InputStream is = jar.getInputStream(entry);
          try {
            Properties properties = new Properties();
            properties.load(is);

            // TODO validate properties and path match
            return properties;
          } finally {
            is.close();
          }
        }
      }
    }
    return null;
  }

  private File getFile(Properties properties, String name) {
    String value = properties.getProperty(name);
    return value != null ? new File(value) : null;
  }

  private Properties getPomProperties(File dir) {
    File[] files = dir.listFiles();
    if(files == null) {
      return null;
    }
    for(File file : files) {
      if(file.isDirectory()) {
        Properties result = getPomProperties(file);
        if(result != null) {
          return result;
        }
      } else if(file.isFile() && "pom.properties".equals(file.getName())) {
        Properties result = new Properties();
        try {
          InputStream is = new BufferedInputStream(new FileInputStream(file));
          try {
            result.load(is);
          } finally {
            IOUtil.close(is);
          }
          // TODO validate properties and path match
          return result;
        } catch(IOException e) {
          // ignore
        }
      }
    }
    return null;
  }

  // copy&paste from org.eclipse.pde.internal.launching.sourcelookup.PDESourceLookupQuery.generateSourceName(String)
  private static String generateSourceName(String qualifiedTypeName) {
    int index = qualifiedTypeName.indexOf('$');
    if(index >= 0)
      qualifiedTypeName = qualifiedTypeName.substring(0, index);
    return qualifiedTypeName.replace('.', File.separatorChar) + ".java"; //$NON-NLS-1$
  }

  public String getSourceName(Object object) throws CoreException {
    // TODO Auto-generated method stub
    return null;
  }

  public void dispose() {
    for(ISourceContainer container : containers.values()) {
      container.dispose();
    }
    containers.clear();
  }

  public void sourceContainersChanged(ISourceLookupDirector director) {
    // TODO Auto-generated method stub

  }

}

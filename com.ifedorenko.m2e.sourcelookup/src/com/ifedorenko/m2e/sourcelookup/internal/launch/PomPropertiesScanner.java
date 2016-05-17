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
package com.ifedorenko.m2e.sourcelookup.internal.launch;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
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

@SuppressWarnings("restriction")
public abstract class PomPropertiesScanner<T> {
  IMavenProjectRegistry projectRegistry = MavenPlugin.getMavenProjectRegistry();

  private static final MetaInfMavenScanner<Properties> SCANNER = new MetaInfMavenScanner<Properties>() {

    @Override
    protected Properties visitFile(File file) throws IOException {
      try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
        Properties properties = new Properties();
        properties.load(is);
        // TODO validate properties and path match
        return properties;
      }
    }

    @Override
    protected Properties visitJarEntry(JarFile jar, JarEntry entry) throws IOException {
      try (InputStream is = jar.getInputStream(entry)) {
        Properties properties = new Properties();
        properties.load(is);
        // TODO validate properties and path match
        return properties;
      }
    }
  };

  public List<T> scan(File location) throws CoreException {
    Set<T> result = new LinkedHashSet<T>();
    for (Properties pomProperties : SCANNER.scan(location, "pom.properties")) {
      T t;

      String projectName = pomProperties.getProperty("m2e.projectName");
      File projectLocation = getFile(pomProperties, "m2e.projectLocation");
      IProject project = projectName != null ? ResourcesPlugin.getWorkspace().getRoot().getProject(projectName) : null;
      if (project != null && project.isAccessible() && project.getLocation().toFile().equals(projectLocation)) {
        IMavenProjectFacade mavenProject = projectRegistry.getProject(project);

        if (mavenProject != null) {
          t = visitMavenProject(mavenProject);
        } else {
          t = visitProject(project);
        }
      } else {
        String groupId = pomProperties.getProperty("groupId");
        String artifactId = pomProperties.getProperty("artifactId");
        String version = pomProperties.getProperty("version");
        t = visitGAVC(groupId, artifactId, version, null);
      }

      if (t != null) {
        result.add(t);
      }
    }

    if (location.isFile()) {
      IndexedArtifactFile indexed = identifyNexusIndexer(location);
      if (indexed != null) {
        ArtifactKey a = indexed.getArtifactKey();
        T t = visitGAVC(a.getGroupId(), a.getArtifactId(), a.getVersion(), a.getClassifier());
        if (t != null) {
          result.add(t);
        }
      }

      try {
        String sha1 = Files.hash(location, Hashing.sha1()).toString();
        URL url = new URL("https://search.maven.org/solrsearch/select?q=1:" + sha1);
        try (InputStreamReader reader = new InputStreamReader(url.openStream(), Charsets.UTF_8)) {
          JsonObject container = new Gson().fromJson(reader, JsonObject.class);
          JsonArray docs = container.get("response").getAsJsonObject().get("docs").getAsJsonArray();
          for (int i = 0; i < docs.size(); i++) {
            JsonObject doc = docs.get(i).getAsJsonObject();
            String g = doc.get("g").getAsString();
            String a = doc.get("a").getAsString();
            String v = doc.get("v").getAsString();
            T t = visitGAVC(g, a, v, null);
            if (t != null) {
              result.add(t);
            }
          }
        }
      } catch (IOException e) {
        // XXX
      }
    }

    return new ArrayList<T>(result);
  }

  protected IndexedArtifactFile identifyNexusIndexer(File file) throws CoreException {
    IIndex index = MavenPlugin.getIndexManager().getAllIndexes();

    List<IndexedArtifactFile> identified;
    if (index instanceof CompositeIndex) {
      identified = ((CompositeIndex) index).identifyAll(file);
    } else {
      IndexedArtifactFile indexed = index.identify(file);
      if (indexed != null) {
        identified = Collections.singletonList(indexed);
      } else {
        identified = Collections.emptyList();
      }
    }

    for (IndexedArtifactFile indexed : identified) {
      if (indexed.sourcesExists == IIndex.PRESENT) {
        return indexed;
      }
    }

    return null;
  }

  protected T visitGAVC(String groupId, String artifactId, String version, String classifier) throws CoreException {
    T t;
    IMavenProjectFacade mavenProject = projectRegistry.getMavenProject(groupId, artifactId, version);
    if (mavenProject != null) {
      t = visitMavenProject(mavenProject);
    } else {
      t = visitArtifact(new ArtifactKey(groupId, artifactId, version, classifier));
    }
    return t;
  }

  private File getFile(Properties properties, String name) {
    String value = properties.getProperty(name);
    return value != null ? new File(value) : null;
  }

  protected abstract T visitArtifact(ArtifactKey artifact) throws CoreException;

  protected abstract T visitMavenProject(IMavenProjectFacade mavenProject);

  protected abstract T visitProject(IProject project);

}

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
package com.ifedorenko.m2e.sourcelookup.equinox;

import static com.ifedorenko.m2e.sourcelookup.equinox.EquinoxClassLoaderHook.getLocation;

import java.io.File;

import org.eclipse.osgi.storage.SystemBundleFile;
import org.eclipse.osgi.storage.bundlefile.DirBundleFile;
import org.eclipse.osgi.storage.bundlefile.NestedDirBundleFile;
import org.eclipse.osgi.storage.bundlefile.ZipBundleFile;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class EquinoxClassLoaderHookTest {
  @Rule
  public final TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void testZipBundleFile() throws Exception {
    File file = new File("src/test/resources/dummy.jar").getCanonicalFile();
    ZipBundleFile bundleFile = new ZipBundleFile(file, null, null, null);
    Assert.assertEquals(file.getCanonicalFile().toURI().toASCIIString(), getLocation(bundleFile));
  }

  @Test
  public void testSystemBundleFile() {
    Assert.assertNull(getLocation(new SystemBundleFile()));
  }

  @Test
  public void testNestedDirBundleFile() throws Exception {
    File dir = temp.newFolder().getCanonicalFile();
    new File(dir, "target/classes/com").mkdirs();
    NestedDirBundleFile bundleFile = new NestedDirBundleFile(new DirBundleFile(dir, false), "target/classes");
    Assert.assertEquals(new File(dir, "target/classes").getCanonicalFile().toURI().toASCIIString(),
        getLocation(bundleFile));
  }
}

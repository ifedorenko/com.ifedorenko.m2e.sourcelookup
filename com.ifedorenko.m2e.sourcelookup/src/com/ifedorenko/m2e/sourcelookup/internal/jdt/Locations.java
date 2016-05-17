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
package com.ifedorenko.m2e.sourcelookup.internal.jdt;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

// TODO rename to something like FileHashes or FileChecksums
// TODO make package protected
class Locations {
  private static class CacheKey {
    public final File file;

    private final long length;

    private final long lastModified;

    public CacheKey(File file) throws IOException {
      this.file = file.getCanonicalFile();
      this.length = file.length();
      this.lastModified = file.lastModified();
    }

    @Override
    public int hashCode() {
      int hash = 17;
      hash = hash * 31 + file.hashCode();
      hash = hash * 31 + (int) length;
      hash = hash * 31 + (int) lastModified;
      return hash;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof CacheKey)) {
        return false;
      }
      CacheKey other = (CacheKey) obj;
      return file.equals(other.file) && length == other.length && lastModified == other.lastModified;
    }
  }

  private static final Cache<CacheKey, HashCode> CACHE = CacheBuilder.newBuilder().build();

  public static final Object hash(final File location) {
    if (location == null || !location.isFile()) {
      return null;
    }
    try {
      return CACHE.get(new CacheKey(location), new Callable<HashCode>() {
        @Override
        public HashCode call() throws Exception {
          return Files.hash(location, Hashing.sha1());
        }
      });
    } catch (ExecutionException | IOException e) {
      return null; // file does not exist or can't be read
    }
  }

  public static final <T> Map<Object, T> hash(Map<File, T> map) {
    Map<Object, T> hashed = new HashMap<>();
    for (Map.Entry<File, T> entry : map.entrySet()) {
      Object hash = hash(entry.getKey());
      if (hash != null) {
        hashed.put(hash, entry.getValue());
      }
    }
    return ImmutableMap.copyOf(hashed);
  }
}

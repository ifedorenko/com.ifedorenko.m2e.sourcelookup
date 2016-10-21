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
package com.ifedorenko.jdt.internal.launching.sourcelookup.advanced;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Helpers to compute file content digests. Provides long-lived hasher instance with bounded cache of most recently
 * requested files, which is useful to handle source lookup requests. Also provides factory of hasher instances with
 * unbounded caches, which is useful to perform bulk workspace indexing.
 */
public class FileHashing {

  public static interface Hasher {
    Object hash(File file);
  }

  // default hasher with bounded cache.
  // this is used when performing source lookup and number of unique files requested during the same debugging session
  // is likely to be small.
  private static final HasherImpl HASHER = new HasherImpl(100);

  /**
   * Returns default long-lived Hasher instance with bounded hash cache.
   */
  public static Hasher hasher() {
    return HASHER;
  }

  /**
   * Returns new Hasher instance with unbounded hash cache, useful for bulk hashing of projects and their dependencies.
   */
  public static Hasher newHasher() {
    return new HasherImpl(HASHER);
  }

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

  private static class HashCode {
    private final byte[] bytes;

    public HashCode(byte[] bytes) {
      this.bytes = bytes; // assumes this class "owns" the array from now on
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(bytes);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof HashCode)) {
        return false;
      }
      return Arrays.equals(bytes, ((HashCode) obj).bytes);
    }
  }

  private static class HasherImpl implements Hasher {

    private final Map<CacheKey, HashCode> cache;

    @SuppressWarnings("serial")
    public HasherImpl(int cacheSize) {
      this.cache = new LinkedHashMap<CacheKey, HashCode>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<CacheKey, HashCode> eldest) {
          return size() > cacheSize;
        }
      };
    }

    public HasherImpl(HasherImpl initial) {
      this.cache = new LinkedHashMap<>(initial.cache);
    }

    @Override
    public Object hash(File file) {
      if (file == null || !file.isFile()) {
        return null;
      }
      try {
        CacheKey cacheKey = new CacheKey(file);
        synchronized (cache) {
          HashCode hashCode = cache.get(cacheKey);
          if (hashCode != null) {
            return hashCode;
          }
        }
        // don't hold cache lock while hashing file
        HashCode hashCode = sha1(file);
        synchronized (cache) {
          cache.put(cacheKey, hashCode);
        }
        return hashCode;
      } catch (IOException e) {
        return null; // file does not exist or can't be read
      }
    }

  }

  private static HashCode sha1(File file) throws IOException {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA1"); //$NON-NLS-1$
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Unsupported JVM", e); //$NON-NLS-1$
    }
    byte[] buf = new byte[4096];
    try (InputStream is = new FileInputStream(file)) {
      int len;
      while ((len = is.read(buf)) > 0) {
        digest.update(buf, 0, len);
      }
    }
    return new HashCode(digest.digest());
  }

}

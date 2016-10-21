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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.jdt.debug.core.IJavaObject;
import org.eclipse.jdt.debug.core.IJavaReferenceType;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaType;
import org.eclipse.jdt.debug.core.IJavaValue;
import org.eclipse.jdt.debug.core.IJavaVariable;

public final class JDIHelpers implements IJDIHelpers {

  // must match ClassfileTransformer.STRATA_ID
  private static final String STRATA_ID = "jdt"; //$NON-NLS-1$

  JDIHelpers() {}

  // jdt debug boilerplate and other ideas were originally "borrowed" from
  // org.eclipse.pde.internal.launching.sourcelookup.PDESourceLookupQuery.run()

  @Override
  public File getClassesLocation(Object element) throws DebugException {
    IJavaReferenceType declaringType = null;
    if (element instanceof IJavaStackFrame) {
      IJavaStackFrame stackFrame = (IJavaStackFrame) element;
      declaringType = stackFrame.getReferenceType();
    } else if (element instanceof IJavaObject) {
      IJavaType javaType = ((IJavaObject) element).getJavaType();
      if (javaType instanceof IJavaReferenceType) {
        declaringType = (IJavaReferenceType) javaType;
      }
    } else if (element instanceof IJavaReferenceType) {
      declaringType = (IJavaReferenceType) element;
    } else if (element instanceof IJavaVariable) {
      IJavaVariable javaVariable = (IJavaVariable) element;
      IJavaType javaType = ((IJavaValue) javaVariable.getValue()).getJavaType();
      if (javaType instanceof IJavaReferenceType) {
        declaringType = (IJavaReferenceType) javaType;
      }
    }

    if (declaringType != null) {
      String[] locations = declaringType.getSourceNames(STRATA_ID);

      if (locations == null || locations.length < 2) {
        return null;
      }

      try {
        URL url = new URL(locations[1]);
        if ("file".equals(url.getProtocol())) { //$NON-NLS-1$
          return new File(url.getPath()).toPath().normalize().toFile();
        }
      } catch (MalformedURLException e) {
        // fall through
      }
    }

    return null;
  }

  @Override
  public String getSourcePath(Object element) throws DebugException {
    IJavaReferenceType declaringType = null;
    if (element instanceof IJavaStackFrame) {
      IJavaStackFrame stackFrame = (IJavaStackFrame) element;
      // under JSR 45 source path from the stack frame is more precise than anything derived from the type
      String sourcePath = stackFrame.getSourcePath(STRATA_ID);
      if (sourcePath != null) {
        return sourcePath;
      }

      declaringType = stackFrame.getReferenceType();
    } else if (element instanceof IJavaObject) {
      IJavaType javaType = ((IJavaObject) element).getJavaType();
      if (javaType instanceof IJavaReferenceType) {
        declaringType = (IJavaReferenceType) javaType;
      }
    } else if (element instanceof IJavaReferenceType) {
      declaringType = (IJavaReferenceType) element;
    } else if (element instanceof IJavaVariable) {
      IJavaType javaType = ((IJavaVariable) element).getJavaType();
      if (javaType instanceof IJavaReferenceType) {
        declaringType = (IJavaReferenceType) javaType;
      }
    }

    if (declaringType != null) {
      String[] sourcePaths = declaringType.getSourcePaths(STRATA_ID);

      if (sourcePaths != null && sourcePaths.length > 0 && sourcePaths[0] != null) {
        return sourcePaths[0];
      }

      return generateSourceName(declaringType.getName());
    }

    return null;
  }

  private static final IStackFrame[] EMPTY_STACK = new IStackFrame[0];

  private IStackFrame[] getStackFrames(Object element) throws DebugException {
    if (element instanceof IStackFrame) {
      IStackFrame[] frames = ((IStackFrame) element).getThread().getStackFrames();
      for (int i = 0; i < frames.length - 1; i++) {
        if (frames[i] == element) {
          return Arrays.copyOfRange(frames, i + 1, frames.length - 1);
        }
      }
    }
    return EMPTY_STACK;
  }

  @Override
  public Iterable<File> getStackFramesClassesLocations(Object element) throws DebugException {
    IStackFrame[] stack = getStackFrames(element);

    return new Iterable<File>() {
      @Override
      public Iterator<File> iterator() {
        return Arrays.stream(stack) //
            .map(frame -> getClassesLocation(frame)) //
            .filter(frameLocation -> frameLocation != null) //
            .iterator();
      }

      File getClassesLocation(IStackFrame frame) {
        // TODO consider ignoring DebugException for all IJDIHeloper methods
        try {
          return JDIHelpers.this.getClassesLocation(frame);
        } catch (DebugException e) {
          return null;
        }
      }
    };
  }

  // copy&paste from org.eclipse.pde.internal.launching.sourcelookup.PDESourceLookupQuery.generateSourceName(String)
  private static String generateSourceName(String qualifiedTypeName) {
    int index = qualifiedTypeName.indexOf('$');
    if (index >= 0) {
      qualifiedTypeName = qualifiedTypeName.substring(0, index);
    }
    return qualifiedTypeName.replace('.', File.separatorChar) + ".java"; //$NON-NLS-1$
  }
}

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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;
import org.eclipse.debug.core.sourcelookup.containers.ExternalArchiveSourceContainer;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaProjectSourceContainer;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.IMavenProjectChangedListener;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;

public class SourceLookupParticipant
    implements ISourceLookupParticipant, IMavenProjectChangedListener
{

    private ISourceLookupDirector director;

    private final Map<File, ISourceContainer> containers = new HashMap<File, ISourceContainer>();

    final IMaven maven = MavenPlugin.getMaven();

    private class CreateContainerRunnable
        implements IRunnableWithProgress
    {
        private final Object element;

        public CreateContainerRunnable( Object element )
        {
            this.element = element;
        }

        @Override
        public void run( IProgressMonitor monitor )
            throws CoreException
        {
            refreshSourceLookup( element, monitor );
        }
    }

    public void init( ISourceLookupDirector director )
    {
        MavenPlugin.getMavenProjectRegistry().addMavenProjectChangedListener( this );
        this.director = director;
    }

    public Object[] findSourceElements( Object fElement )
        throws CoreException
    {
        ISourceContainer container = getSourceContainer( fElement, null /* async */);

        if ( container == null )
        {
            return null;
        }

        String sourcePath = JDIHelpers.getSourcePath( fElement );
        if ( sourcePath == null )
        {
            // can't really happen
            return null;
        }

        return container.findSourceElements( sourcePath );
    }

    public ISourceContainer getSourceContainer( Object fElement, IProgressMonitor monitor )
        throws CoreException
    {
        File location = JDIHelpers.getLocation( fElement );

        if ( location == null )
        {
            return null;
        }

        ISourceContainer container;

        synchronized ( this.containers )
        {
            if ( this.containers.containsKey( location ) )
            {
                container = this.containers.get( location );
            }
            else
            {
                final JavaProjectSources workspaceSources = SourceLookupActivator.getWorkspaceSources();

                container = workspaceSources.getProjectSourceContainer( location );

                if ( container == null )
                {
                    IStackFrame[] stackFrames = getStackFrames( fElement );

                    if ( stackFrames != null )
                    {
                        container = workspaceSources.getContextSourceContainer( location, stackFrames );
                    }
                }

                if ( container == null )
                {
                    // this is still better than name-only lookup performed by default jdt source lookup participant
                    container = workspaceSources.getAnySourceContainer( location );
                }

                if ( container == null )
                {
                    if ( monitor != null )
                    {
                        container = createSourceContainer( location, monitor );
                    }
                    else
                    {
                        SourceLookupActivator.schedule( new CreateContainerRunnable( fElement ) );
                    }
                }

                if ( container != null )
                {
                    container.init( director );
                }

                this.containers.put( location, container );
            }
        }

        return container;
    }

    private IStackFrame[] getStackFrames( Object element )
        throws DebugException
    {
        IStackFrame frame = null;
        if ( element instanceof IStackFrame )
        {
            frame = (IStackFrame) element;
        }
        if ( frame == null )
        {
            // not sure how useful this is
            // it makes variable type lookup more precise when the same dependency is referenced from multiple projects
            // the same dependency will be found by scanning all workspace projects, albeit in that case the same
            // "correct" dependency may come from "wrong" project.
            // this logic also introduces dependency on debug.ui, the only ui dependency of this bundle.
            // ui dependency is not a big deal, but the gain is not great either.
            // ... and this is really long comment to justify so little code with so little gain :-)
            frame = (IStackFrame) DebugUITools.getDebugContext().getAdapter( IStackFrame.class );
        }
        if ( frame != null )
        {
            IStackFrame[] frames = frame.getThread().getStackFrames();
            for ( int i = 0; i < frames.length - 1; i++ )
            {
                if ( frames[i] == frame )
                {
                    IStackFrame[] stack = new IStackFrame[frames.length - i - 1];
                    System.arraycopy( frames, i + 1, stack, 0, frames.length - i - 1 );
                    return stack;
                }
            }
        }
        return null;
    }

    protected ISourceContainer createSourceContainer( final File location, final IProgressMonitor monitor )
        throws CoreException
    {
        List<ISourceContainer> containers = new PomPropertiesScanner<ISourceContainer>()
        {
            @Override
            protected ISourceContainer visitArtifact( ArtifactKey artifact )
                throws CoreException
            {
                String groupId = artifact.getGroupId();
                String artifactId = artifact.getArtifactId();
                String version = artifact.getVersion();

                IMaven maven = MavenPlugin.getMaven();

                List<ArtifactRepository> repositories = new ArrayList<ArtifactRepository>();
                repositories.addAll( maven.getArtifactRepositories() );
                repositories.addAll( maven.getPluginArtifactRepositories() );

                if ( !maven.isUnavailable( groupId, artifactId, version, "jar", "sources", repositories ) )
                {
                    Artifact resolve = maven.resolve( groupId, artifactId, version, "jar", "sources", null, monitor );

                    return new ExternalArchiveSourceContainer( resolve.getFile().getAbsolutePath(), true );
                }

                return null;
            }

            @Override
            protected ISourceContainer visitMavenProject( IMavenProjectFacade mavenProject )
            {
                return JavaProjectSources.getProjectSourceContainer( JavaCore.create( mavenProject.getProject() ) );
            }

            @Override
            protected ISourceContainer visitProject( IProject project )
            {
                return new JavaProjectSourceContainer( JavaCore.create( project ) );
            }
        }.scan( location );

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

    public String getSourceName( Object object )
        throws CoreException
    {
        return null;
    }

    public void dispose()
    {
        disposeContainers();
        MavenPlugin.getMavenProjectRegistry().removeMavenProjectChangedListener( this );
    }

    protected void disposeContainers()
    {
        synchronized ( containers )
        {
            for ( ISourceContainer container : containers.values() )
            {
                if ( container != null ) // possible for non-maven jars
                {
                    container.dispose();
                }
            }
            containers.clear();
        }
    }

    public void sourceContainersChanged( ISourceLookupDirector director )
    {
        disposeContainers();
    }

    @Override
    public void mavenProjectChanged( MavenProjectChangedEvent[] events, IProgressMonitor monitor )
    {
        disposeContainers();
    }

    public void refreshSourceLookup( Object element, final IProgressMonitor monitor )
        throws DebugException, CoreException
    {
        File location = JDIHelpers.getLocation( element );

        if ( location == null )
        {
            return;
        }

        ISourceContainer container = createSourceContainer( location, monitor );
        synchronized ( containers )
        {
            //
            ISourceContainer oldContainer = containers.put( location, container );
            if ( oldContainer != null )
            {
                oldContainer.dispose();
            }
        }
        if ( container != null )
        {
            director.clearSourceElements( element );
            if ( element instanceof DebugElement )
            {
                // this is apparently needed to flush StackFrameSourceDisplayAdapter cache
                ( (DebugElement) element ).fireChangeEvent( DebugEvent.CONTENT );
            }
        }
    }

    public static SourceLookupParticipant getSourceLookup( Object debugElement )
    {
        ISourceLocator sourceLocator = null;
        if ( debugElement instanceof IDebugElement )
        {
            sourceLocator = ( (IDebugElement) debugElement ).getLaunch().getSourceLocator();
        }

        SourceLookupParticipant sourceLookup = null;
        if ( sourceLocator instanceof ISourceLookupDirector )
        {
            for ( ISourceLookupParticipant participant : ( (ISourceLookupDirector) sourceLocator ).getParticipants() )
            {
                if ( participant instanceof SourceLookupParticipant )
                {
                    sourceLookup = (SourceLookupParticipant) participant;
                    break;
                }
            }
        }
        return sourceLookup;
    }
}

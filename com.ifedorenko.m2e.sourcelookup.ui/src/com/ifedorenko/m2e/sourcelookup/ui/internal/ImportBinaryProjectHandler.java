package com.ifedorenko.m2e.sourcelookup.ui.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.ui.handlers.HandlerUtil;

import com.ifedorenko.m2e.binaryproject.BinaryProjectPlugin;
import com.ifedorenko.m2e.sourcelookup.internal.JDIHelpers;
import com.ifedorenko.m2e.sourcelookup.internal.PomPropertiesScanner;

public class ImportBinaryProjectHandler
    extends AbstractHandler
{
    @Override
    public Object execute( ExecutionEvent event )
        throws ExecutionException
    {
        ISelection selection = HandlerUtil.getCurrentSelectionChecked( event );

        if ( !( selection instanceof IStructuredSelection ) || selection.isEmpty() )
        {
            return null;
        }
        try
        {
            final String location = JDIHelpers.getLocation( ( (IStructuredSelection) selection ).getFirstElement() );

            if ( location == null )
            {
                return null;
            }

            Job job = new Job( "Import binary projects" )
            {
                @Override
                protected IStatus run( IProgressMonitor monitor )
                {
                    List<ArtifactKey> artifacts;
                    try
                    {
                        artifacts = new PomPropertiesScanner<ArtifactKey>()
                        {
                            @Override
                            protected ArtifactKey visitProject( IProject project )
                            {
                                return null;
                            }

                            @Override
                            protected ArtifactKey visitMavenProject( IMavenProjectFacade mavenProject )
                            {
                                return null;
                            }

                            @Override
                            protected ArtifactKey visitArtifact( ArtifactKey artifact )
                                throws CoreException
                            {
                                return artifact;
                            }
                        }.scan( location );
                    }
                    catch ( CoreException e1 )
                    {
                        return e1.getStatus();
                    }

                    if ( !artifacts.isEmpty() )
                    {
                        List<IStatus> errors = new ArrayList<IStatus>();

                        for ( ArtifactKey artifact : artifacts )
                        {
                            try
                            {
                                BinaryProjectPlugin.getInstance().create( artifact.getGroupId(),
                                                                          artifact.getArtifactId(),
                                                                          artifact.getVersion(), null, monitor );
                            }
                            catch ( CoreException e )
                            {
                                errors.add( e.getStatus() );
                            }
                        }
                    }
                    return Status.OK_STATUS;
                }
            };
            job.schedule();
        }
        catch ( DebugException e )
        {
            throw new ExecutionException( "Could not import binary project", e );
        }

        return null;
    }
}

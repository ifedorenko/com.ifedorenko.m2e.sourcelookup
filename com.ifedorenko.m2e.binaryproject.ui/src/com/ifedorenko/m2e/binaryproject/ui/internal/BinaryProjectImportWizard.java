package com.ifedorenko.m2e.binaryproject.ui.internal;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.ui.internal.wizards.MavenDependenciesWizardPage;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

import com.ifedorenko.m2e.binaryproject.AbstractBinaryProjectsImportJob;

@SuppressWarnings( "restriction" )
public class BinaryProjectImportWizard
    extends Wizard
    implements IImportWizard
{

    private MavenDependenciesWizardPage artifactsPage;

    public BinaryProjectImportWizard()
    {
    }

    @Override
    public void init( IWorkbench workbench, IStructuredSelection selection )
    {
    }

    @Override
    public boolean performFinish()
    {
        final ArrayList<ArtifactKey> artifacts = new ArrayList<ArtifactKey>();

        for ( Dependency dependency : artifactsPage.getDependencies() )
        {
            artifacts.add( new ArtifactKey( dependency.getGroupId(), dependency.getArtifactId(),
                                            dependency.getVersion(), dependency.getClassifier() ) );
        }

        if ( artifacts.isEmpty() )
        {
            return false;
        }

        Job job = new AbstractBinaryProjectsImportJob()
        {

            @Override
            protected List<ArtifactKey> getArtifactKeys()
                throws CoreException
            {
                return artifacts;
            }
        };
        job.schedule();

        return true;
    }

    @Override
    public boolean canFinish()
    {
        return artifactsPage.getDependencies().length > 0;
    }

    @Override
    public void addPages()
    {
        artifactsPage =
            new MavenDependenciesWizardPage( new ProjectImportConfiguration(), "Artifacts",
                                             "Select artifacts to import" )
            {
                @Override
                protected void createAdvancedSettings( Composite composite, GridData gridData )
                {
                    // TODO profile can theoretically be usedful
                }
            };
        artifactsPage.setDependencies( new Dependency[0] );
        artifactsPage.addListener( new ISelectionChangedListener()
        {
            @Override
            public void selectionChanged( SelectionChangedEvent event )
            {
                getContainer().updateButtons();
            }
        } );
        addPage( artifactsPage );
    }
}

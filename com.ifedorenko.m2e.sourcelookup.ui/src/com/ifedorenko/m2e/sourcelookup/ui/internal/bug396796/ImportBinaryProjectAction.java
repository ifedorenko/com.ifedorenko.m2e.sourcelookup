package com.ifedorenko.m2e.sourcelookup.ui.internal.bug396796;

import org.eclipse.debug.core.DebugException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;

import com.ifedorenko.m2e.sourcelookup.ui.internal.ImportBinaryProjectHandler;

public class ImportBinaryProjectAction
    implements IObjectActionDelegate
{
    private ISelection selection;

    private IWorkbenchPart targetPart;

    @Override
    public void run( IAction action )
    {
        try
        {
            ImportBinaryProjectHandler.importBinaryProjects( DebugElementWrapper.getDebugElement( selection ) );
        }
        catch ( DebugException e )
        {
            ErrorDialog.openError( getShell(), "Could not import project", null, e.getStatus() );
        }
    }

    private Shell getShell()
    {
        IWorkbenchPartSite site = targetPart.getSite();
        return site != null ? site.getShell() : null;
    }

    @Override
    public void selectionChanged( IAction action, ISelection selection )
    {
        this.selection = selection;
    }

    @Override
    public void setActivePart( IAction action, IWorkbenchPart targetPart )
    {
        this.targetPart = targetPart;
    }

}

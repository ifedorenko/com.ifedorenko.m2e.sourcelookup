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
package com.ifedorenko.m2e.sourcelookup.ui.internal.bug396796;

import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.ifedorenko.m2e.sourcelookup.internal.JDILocation;
import com.ifedorenko.m2e.sourcelookup.internal.SourceLookupParticipant;
import com.ifedorenko.m2e.sourcelookup.ui.internal.SourceLookupInfoDialog;

public class OpenSourceLookupInfoDialogAction
    implements IObjectActionDelegate
{

    private ISelection selection;

    private IWorkbenchPart targetPart;

    @Override
    public void run( IAction action )
    {
        if ( selection instanceof IStructuredSelection )
        {
            JDILocation wrapper = (JDILocation) ( (IStructuredSelection) selection ).getFirstElement();

            Object debugElement = wrapper.getDebugElement();

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

            new SourceLookupInfoDialog( getShell(), debugElement, sourceLookup ).open();
        }
    }

    private Shell getShell()
    {
        return targetPart.getSite().getShell();
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

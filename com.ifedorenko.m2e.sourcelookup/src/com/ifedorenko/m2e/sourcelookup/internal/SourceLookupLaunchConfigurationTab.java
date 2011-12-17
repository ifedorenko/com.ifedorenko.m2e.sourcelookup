package com.ifedorenko.m2e.sourcelookup.internal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

public class SourceLookupLaunchConfigurationTab
    extends AbstractLaunchConfigurationTab
{
    private Button btnDynamicSourceCode;

    /**
     * @wbp.parser.entryPoint
     */
    public void createControl( Composite parent )
    {
        Composite composite = new Composite( parent, SWT.NONE );
        setControl( composite );
        composite.setLayout( new GridLayout( 1, false ) );

        btnDynamicSourceCode = new Button( composite, SWT.CHECK );
        btnDynamicSourceCode.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                setDirty( true );
                updateLaunchConfigurationDialog();
            }
        } );
        btnDynamicSourceCode.setToolTipText( "Uses javaagent to instrument classes with additional information required to locate their sources." );
        btnDynamicSourceCode.setText( "Dynamic source code lookup (experimental)" );
    }

    public void setDefaults( ILaunchConfigurationWorkingCopy configuration )
    {
        configuration.setAttribute( SourceLookupMavenLaunchParticipant.ATTR_SOURCELOOKUP_JAVAAGENT, true );
    }

    public void initializeFrom( ILaunchConfiguration configuration )
    {
        try
        {
            btnDynamicSourceCode.setSelection( configuration.getAttribute( SourceLookupMavenLaunchParticipant.ATTR_SOURCELOOKUP_JAVAAGENT,
                                                                           true ) );
        }
        catch ( CoreException e )
        {
            btnDynamicSourceCode.setSelection( true );
        }
    }

    public void performApply( ILaunchConfigurationWorkingCopy configuration )
    {
        configuration.setAttribute( SourceLookupMavenLaunchParticipant.ATTR_SOURCELOOKUP_JAVAAGENT,
                                    btnDynamicSourceCode.getSelection() );
    }

    public String getName()
    {
        return "Dynamic sources";
    }
}

package com.ifedorenko.m2e.sourcelookup.launch.internal;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.ifedorenko.m2e.sourcelookup.internal.SourceLookupMavenLaunchParticipant;

@SuppressWarnings( "restriction" )
public class DynamicSourceLookupTab
    extends AbstractLaunchConfigurationTab
{

    /**
     * @wbp.parser.entryPoint
     */
    @Override
    public void createControl( Composite parent )
    {
        Composite composite = new Composite( parent, SWT.NONE );

        setControl( composite );
        composite.setLayout( new GridLayout( 1, false ) );

        Label lblJvmStartupParameters = new Label( composite, SWT.NONE );
        lblJvmStartupParameters.setText( "JVM startup parameters" );

        Text text = new Text( composite, SWT.BORDER | SWT.WRAP | SWT.MULTI );
        text.setEditable( false );
        text.setText( SourceLookupMavenLaunchParticipant.getVMArguments() );
        GridData gd_text = new GridData( SWT.FILL, SWT.TOP, true, true, 1, 1 );
        gd_text.widthHint = 200;
        gd_text.minimumHeight = 100;
        gd_text.horizontalIndent = 10;
        text.setLayoutData( gd_text );
    }

    @Override
    public void setDefaults( ILaunchConfigurationWorkingCopy configuration )
    {
    }

    @Override
    public void initializeFrom( ILaunchConfiguration configuration )
    {
    }

    @Override
    public void performApply( ILaunchConfigurationWorkingCopy configuration )
    {
    }

    @Override
    public String getName()
    {
        return "Dynamic Source";
    }
}

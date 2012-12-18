package com.ifedorenko.m2e.sourcelookup.internal;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugElement;

@SuppressWarnings( "rawtypes" )
public class JDILocationAdapterFactory
    implements IAdapterFactory
{
    private static final Class[] ADAPTER_LIST = new Class[] { JDILocation.class, };

    @Override
    public Object getAdapter( Object adaptableObject, Class adapterType )
    {
        if ( !JDILocation.class.equals( adapterType ) || !( adaptableObject instanceof IDebugElement ) )
        {
            return null;
        }
        try
        {
            String location = JDIHelpers.getLocation( adaptableObject );
            if ( location != null )
            {
                return new JDILocation( location );
            }
        }
        catch ( DebugException e )
        {
            // too bad
        }
        return null;
    }

    @Override
    public Class[] getAdapterList()
    {
        return ADAPTER_LIST;
    }

}

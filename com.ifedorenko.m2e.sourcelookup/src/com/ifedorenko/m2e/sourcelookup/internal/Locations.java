package com.ifedorenko.m2e.sourcelookup.internal;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

class Locations
{
    public static final Object hash( File location )
    {
        try
        {
            return Files.hash( location, Hashing.sha1() );
        }
        catch ( IOException e )
        {
            return null;
        }
    }

    public static final <T> Map<Object, T> hash( Map<File, T> map )
    {
        Map<Object, T> hashed = new HashMap<>();
        for ( Map.Entry<File, T> entry : map.entrySet() )
        {
            Object hash = hash( entry.getKey() );
            if ( hash != null )
            {
                hashed.put( hash, entry.getValue() );
            }
        }
        return ImmutableMap.copyOf( hashed );
    }
}

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
package webapp;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lib.Lib;

public class WebappServlet
    extends HttpServlet
{
    private static final long serialVersionUID = -7189913424514222067L;

    protected void doGet( HttpServletRequest req, HttpServletResponse resp )
        throws ServletException, IOException
    {
        Lib lib = new Lib();
        List mylist = new MyList();
        mylist.add( "a" );
        mylist.add( "b" );
        mylist.add( lib.getMessage() );
        resp.getWriter().println( mylist.toString() );
    }
}

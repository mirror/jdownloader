/*
 *      HTML.Template:  A module for using HTML Templates with java
 *
 *      Copyright (c) 2002 Philip S Tellis (philip.tellis@iname.com)
 *
 *      This module is free software; you can redistribute it
 *      and/or modify it under the terms of either:
 *
 *      a) the GNU General Public License as published by the Free
 *      Software Foundation; either version 1, or (at your option)
 *      any later version, or
 *
 *      b) the "Artistic License" which comes with this module.
 *
 *      This program is distributed in the hope that it will be
 *      useful, but WITHOUT ANY WARRANTY; without even the implied
 *      warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *      PURPOSE.  See either the GNU General Public License or the
 *      Artistic License for more details.
 *
 *      You should have received a copy of the Artistic License
 *      with this module, in the file ARTISTIC.  If not, I'll be
 *      glad to provide one.
 *
 *      You should have received a copy of the GNU General Public
 *      License along with this program; if not, write to the Free
 *      Software Foundation, Inc., 59 Temple Place, Suite 330,
 *      Boston, MA 02111-1307 USA
 */

package org.jdownloader.extensions.webinterface.template.tmpls.element;

import java.util.Hashtable;
import java.util.NoSuchElementException;

public abstract class Element {
    protected String name = "";
    protected String type;

    public void add(Element node) {
    }

    public void add(String data) {
    }

    public boolean contains(String param) {
        try {
            return typeOfParam(param) != null ? true : false;
        } catch (NoSuchElementException nse) {
            return false;
        }
    }

    public final String Name() {
        return name;
    }

    public abstract String parse(Hashtable<?, ?> params);

    public final String Type() {
        return type;
    }

    public abstract String typeOfParam(String param) throws NoSuchElementException;
}
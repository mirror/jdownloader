/*
 * $Id: JXTaskPaneBeanInfo.java,v 1.3 2007/12/10 11:41:32 stolis Exp $
 *
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.jdesktop.swingx;

import java.beans.BeanDescriptor;

/**
 * BeanInfo class for JXTaskPane.
 * 
 * @author rbair, Jan Stola
 */
public class JXTaskPaneBeanInfo extends BeanInfoSupport {
    
    /** Constructor for the JXTaskPaneBeanInfo object */
    public JXTaskPaneBeanInfo() {
        super(JXTaskPane.class);
    }
    
    protected void initialize() {
        BeanDescriptor bd = getBeanDescriptor();
        
        // setup bean descriptor in constructor.
        bd.setName("JXTaskPane");
        bd.setShortDescription("JXTaskPane is a container for tasks and other arbitrary components.");
        bd.setValue("isContainer", Boolean.TRUE);
        bd.setValue("containerDelegate", "getContentPane");
        
        setPreferred(true, "title", "icon", "special");
        setPreferred(true, "animated", "scrollOnExpand", "expanded", "font");
        setBound(true, "title", "icon", "special", "scrollOnExpand", "expanded");
        setPreferred(false, "border");
    }
}
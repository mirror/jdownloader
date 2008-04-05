/*
 * $Id: JXCollapsiblePaneBeanInfo.java,v 1.4 2007/12/06 19:39:27 stolis Exp $
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
import org.jdesktop.swingx.editors.EnumPropertyEditor;

/**
 * BeanInfo class for JXCollapsiblePane.
 * 
 * @author Jan Stola
 */
public class JXCollapsiblePaneBeanInfo extends BeanInfoSupport {
    /** Constructor for the JXCollapsiblePaneBeanInfo object */
    public JXCollapsiblePaneBeanInfo() {
        super(JXCollapsiblePane.class);        
    }

    protected void initialize() {
        BeanDescriptor bd = getBeanDescriptor();
        bd.setName("JXCollapsiblePane");
        bd.setShortDescription("A pane which hides its content with an animation.");
        bd.setValue("isContainer", Boolean.TRUE);
        bd.setValue("containerDelegate", "getContentPane");
        
        setPreferred(true, "animated", "collapsed", "orientation");
        setBound(true, "animated", "collapsed", "orientation");
        setPropertyEditor(OrientationPropertyEditor.class, "orientation");
    }

    public static final class OrientationPropertyEditor extends EnumPropertyEditor<JXCollapsiblePane.Orientation> {
        public OrientationPropertyEditor() {
            super(JXCollapsiblePane.Orientation.class);
        }
    }

}

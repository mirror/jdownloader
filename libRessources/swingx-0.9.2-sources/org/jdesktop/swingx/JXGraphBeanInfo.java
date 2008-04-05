/*
 * $Id: JXGraphBeanInfo.java,v 1.2 2006/03/16 22:53:55 rbair Exp $
 *
 * Copyright 2006 Sun Microsystems, Inc., 4150 Network Circle,
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

import org.jdesktop.swingx.editors.Point2DPropertyEditor;
import org.jdesktop.swingx.editors.Rectangle2DPropertyEditor;

/**
 * Bean info for {@link org.jdesktop.swingx.JXGraph} component.
 *
 * @author Romain Guy <romain.guy@mac.com>
 */
public class JXGraphBeanInfo extends BeanInfoSupport {
    public JXGraphBeanInfo() {
        super(JXGraph.class);
    }

    protected void initialize() {
        setCategory("Graph View", "majorX", "majorY", "minorCountX", "minorCountY",
                    "origin", "view");
        setCategory("Graph Appearance", "axisColor", "axisPainted", "backgroundPainted", 
                    "gridPainted", "majorGridColor", "minorGridColor", "textPainted");
        setCategory("Graph Input", "inputEnabled");
        
        setPropertyEditor(Point2DPropertyEditor.class, "origin");
        setPropertyEditor(Rectangle2DPropertyEditor.class, "view");
        
        setDisplayName("vertical lines spacing", "majorX");
        setDisplayName("horizontal lines spacing", "majorY");
        setDisplayName("sub-vertical lines count", "minorCountX");
        setDisplayName("sub-horizontal lines count", "minorCountY");
        setDisplayName("major grid lines color", "majorGridColor");
        setDisplayName("minor grid lines color", "minorGridColor");
    }
}

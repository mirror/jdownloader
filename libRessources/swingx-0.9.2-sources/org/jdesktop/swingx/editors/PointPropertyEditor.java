/*
 * $Id: PointPropertyEditor.java,v 1.2 2007/03/14 19:46:16 joshy Exp $
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

package org.jdesktop.swingx.editors;

import java.awt.Point;
import java.beans.PropertyEditorSupport;

/**
 *
 * @author rbair
 */
public class PointPropertyEditor extends PropertyEditorSupport {

    /** Creates a new instance of Point2DPropertyEditor */
    public PointPropertyEditor() {
    }

    public Point getValue() {
        return (Point)super.getValue();
    }

    public String getJavaInitializationString() {
        Point point = getValue();
        return point == null ? "null" : "new java.awt.Point(" + point.getX() + ", " + point.getY() + ")";
    }

    public void setAsText(String text) throws IllegalArgumentException {
        String originalParam = text;
        try {
            Point val = (Point)PropertyEditorUtil.createValueFromString(
                    text, 2, Point.class, int.class);
            setValue(val);
        } catch (Exception e) {
            throw new IllegalArgumentException("The input value " + originalParam + " is not formatted correctly. Please " +
                    "try something of the form [x,y] or [x , y] or [x y]", e);
        }
    }

    public String getAsText() {
        Point point = getValue();
        return point == null ? "[]" : "[" + point.x + ", " + point.y + "]";
    }
} 


/*
 * $Id: Point2DPropertyEditor.java,v 1.3 2007/03/14 19:46:15 joshy Exp $
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

import java.awt.geom.Point2D;
import java.beans.PropertyEditorSupport;

/**
 *
 * @author rbair
 */
public class Point2DPropertyEditor extends PropertyEditorSupport {
    
    /** Creates a new instance of Point2DPropertyEditor */
    public Point2DPropertyEditor() {
    }
    
    public Point2D getValue() {
        return (Point2D)super.getValue();
    }
    
    public String getJavaInitializationString() {
        Point2D point = getValue();
        return point == null ? "null" : "new java.awt.geom.Point2D.Double(" + point.getX() + ", " + point.getY() + ")";
    }
    
    public void setAsText(String text) throws IllegalArgumentException {
        
        String originalParam = text;
        try {
            Point2D val = (Point2D)PropertyEditorUtil.createValueFromString(
                    text, 2, Point2D.Double.class, double.class);
            setValue(val);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            throw new IllegalArgumentException("The input value " + originalParam + " is not formatted correctly. Please " +
                    "try something of the form [x,y] or [x , y] or [x y]", e);
        }
    }
    
    public String getAsText() {
        Point2D point = getValue();
        return point == null ? "[]" : "[" + point.getX() + ", " + point.getY() + "]";
    }
    
    public static void main(String... args) {
        test("[1.5,1.2]");
        test("1.5,1.2]");
        test("[1.5,1.2");
        test("[ 1.5 , 1.2 ]");
        test(" 1.5 , 1.2 ]");
        test("[ 1.5 , 1.2");
        test("1.5,1.2");
        test(" 1.5 , 1.2 ");
        test("1.5 1.2");
        test("");
        test("null");
        test("[]");
        test("[ ]");
        test("[1.5 1.2]");
    }
    
    private static void test(String input) {
        System.out.print("Input '" + input + "'");
        try {
            Point2DPropertyEditor ed = new Point2DPropertyEditor();
            ed.setAsText(input);
            Point2D point = ed.getValue();
            System.out.println(" succeeded: " + point);
        } catch (Exception e) {
            System.out.println(" failed: " + e.getMessage());
        }
    }
}

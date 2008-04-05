/*
 * $Id: Rectangle2DPropertyEditor.java,v 1.2 2007/03/14 19:46:17 joshy Exp $
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

import java.awt.geom.Rectangle2D;
import java.beans.PropertyEditorSupport;

/**
 *
 * @author rbair
 */
public class Rectangle2DPropertyEditor extends PropertyEditorSupport {

    /** Creates a new instance of Rectangle2DPropertyEditor */
    public Rectangle2DPropertyEditor() {
    }

    public Rectangle2D getValue() {
        return (Rectangle2D.Double)super.getValue();
    }

    public String getJavaInitializationString() {
        Rectangle2D rect = getValue();
        return rect == null ? "null" : "new java.awt.geom.Rectangle2D.Double(" + rect.getX() + ", " + rect.getY() + ", " + rect.getWidth() + ", " + rect.getHeight() + ")";
    }

    public void setAsText(String text) throws IllegalArgumentException {
        String originalParam = text;
        try {
            Rectangle2D val = (Rectangle2D)PropertyEditorUtil.createValueFromString(
                    text, 4, Rectangle2D.Double.class, double.class);
            setValue(val);
        } catch (Exception e) {
            throw new IllegalArgumentException("The input value " + originalParam + " is not formatted correctly. Please " +
                    "try something of the form [x,y,w,h] or [x , y , w , h] or [x y w h]", e);
        }
    }

    public String getAsText() {
        Rectangle2D rect = getValue();
        return rect == null ? "[]" : "[" + rect.getX() + ", " + rect.getY() + ", " + rect.getWidth() + ", " + rect.getHeight() + "]";
    }

    public static void main(String... args) {
        test("[1.5,1.2,10,35]");
        test("1.5,1.2,10,35]");
        test("[1.5,1.2,10,35");
        test("[ 1.5 , 1.2 ,10,35]");
        test(" 1.5 , 1.2 ,10,35]");
        test("[ 1.5 , 1.2,10,35");
        test("1.5,1.2,10,35");
        test(" 1.5 , 1.2 10 35");
        test("1.5 1.2, 10 35");
        test("");
        test("null");
        test("[]");
        test("[ ]");
        test("[1.5 1.2 10 35]");
    }

    private static void test(String input) {
        System.out.print("Input '" + input + "'");
        try {
            Rectangle2DPropertyEditor ed = new Rectangle2DPropertyEditor();
            ed.setAsText(input);
            Rectangle2D rect = ed.getValue();
            System.out.println(" succeeded: " + rect);
        } catch (Exception e) {
            System.out.println(" failed: " + e.getMessage());
        }
    }
} 

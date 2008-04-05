/*
 * RectanglePropertyEditor.java
 *
 * Created on August 16, 2006, 12:13 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jdesktop.swingx.editors;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyEditorSupport;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


/**
 *
 * @author joshy
 */
public class RectanglePropertyEditor extends PropertyEditorSupport {
    
    /** Creates a new instance of Rectangle2DPropertyEditor */
    public RectanglePropertyEditor() {
    }
    
    public Rectangle getValue() {
        return (Rectangle)super.getValue();
    }
    
    public String getJavaInitializationString() {
        Rectangle rect = getValue();
        return rect == null ? "null" : "new java.awt.Rectangle(" + rect.getX() + ", " + rect.getY() + ", " + rect.getWidth() + ", " + rect.getHeight() + ")";
    }
    
    public void setAsText(String text) throws IllegalArgumentException {
        String originalParam = text;
        try {
            Rectangle val = (Rectangle)PropertyEditorUtil.createValueFromString(text, 4, Rectangle.class, int.class);
            setValue(val);
        } catch (Throwable e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            throw new IllegalArgumentException("The input value " + originalParam + " is not formatted correctly. Please " +
                    "try something of the form [x,y,w,h] or [x , y , w , h] or [x y w h]", e);
        }
    }
    
    
    public String getAsText() {
        Rectangle rect = getValue();
        return rect == null ? "[]" : "[" + rect.x + ", " + rect.y + ", " + rect.width + ", " + rect.height + "]";
    }
    
}

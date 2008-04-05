/*
 * DimensionPropertyEditor.java
 *
 * Created on August 16, 2006, 12:18 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jdesktop.swingx.editors;

import java.awt.Dimension;
import java.beans.PropertyEditorSupport;

/**
 *
 * @author joshy
 */
public class DimensionPropertyEditor extends PropertyEditorSupport {
    
    public DimensionPropertyEditor() {
    }
    
    public Dimension getValue() {
        return (Dimension)super.getValue();
    }
    
    public String getJavaInitializationString() {
        Dimension point = getValue();
        return point == null ? "null" : "new java.awt.Dimension(" + point.width + ", " + point.height + ")";
    }
    
    public void setAsText(String text) throws IllegalArgumentException {
        String originalParam = text;
        try {
            Dimension val = (Dimension)PropertyEditorUtil.createValueFromString(
                    text, 2, Dimension.class, int.class);
            setValue(val);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
            throw new IllegalArgumentException("The input value " + originalParam + " is not formatted correctly. Please " +
                    "try something of the form [w,h] or [w , h] or [w h]", ex);
        }
    }
    
    public String getAsText() {
        Dimension dim = getValue();
        return dim == null ? "[]" : "[" + dim.width + ", " + dim.height + "]";
    }
    
}


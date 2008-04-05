/*
 * InsetsPropertyEditor.java
 *
 * Created on July 20, 2006, 12:16 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jdesktop.swingx.editors;

import java.awt.Insets;
import java.beans.PropertyEditorSupport;

/**
 *
 * @author joshy
 */
public class InsetsPropertyEditor extends PropertyEditorSupport {
    
    /** Creates a new instance of InsetsPropertyEditor */
    public InsetsPropertyEditor() {
    }
    
    public Insets getValue() {
        return (Insets)super.getValue();
    }
    
    public void setAsText(String text) {
        String originalParam = text;
        
        try {
            Insets val = (Insets)PropertyEditorUtil.createValueFromString(
                    text, 4, Insets.class, int.class);
            setValue(val);
        } catch (Exception e) {
            throw new IllegalArgumentException("The input value " + originalParam + " is not formatted correctly. Please " +
                    "try something of the form [top,left,bottom,right] or [top , left , bottom , right] or [top left bottom right]", e);
        }
    }
    
    public String getAsText() {
        Insets val = getValue();
        return val == null ? "[]" : "[" + val.top + ", " + val.left + ", " + 
                val.bottom + ", " + val.right + "]";
    }
    
}

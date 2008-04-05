/*
 * EnumerationValuePropertyEditor.java
 *
 * Created on March 28, 2006, 3:49 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jdesktop.swingx.editors;

import java.beans.PropertyEditorSupport;
import java.util.HashMap;
import java.util.Map;
import org.jdesktop.swingx.EnumerationValue;

/**
 *
 * @author Richard
 */
public abstract class EnumerationValuePropertyEditor extends PropertyEditorSupport {
    private String[] tags;
    private Map<Object,EnumerationValue> values = new HashMap<Object,EnumerationValue>();
    private EnumerationValue defaultValue;
    
    /** Creates a new instance of EnumerationValuePropertyEditor */
    public EnumerationValuePropertyEditor(EnumerationValue defaultEnum, EnumerationValue... enums) {
        this.defaultValue = defaultEnum;
        for (EnumerationValue v : enums) {
            values.put(v.getValue(), v);
        }
        
        tags = new String[enums.length];
        int index = 0;
        for (EnumerationValue v : enums) {
            tags[index++] = v.getName();
        }
    }

    public String getJavaInitializationString() {
        EnumerationValue value = values.get(getValue());
        if (value == null) {
            return defaultValue == null ? "null" : defaultValue.getJavaInitializationString();
        } else {
            return value.getJavaInitializationString();
        }
    }

    public String[] getTags() {
        return tags;
    }

    public String getAsText() {
        EnumerationValue value = values.get(getValue());
        if (value == null) {
            return defaultValue == null ? null : defaultValue.getName();
        } else {
            return value.getName();
        }
    }
    
    public void setAsText(String text) throws IllegalArgumentException {
        EnumerationValue v = getValueByName(text);
        if (v == null) {
            //hmmmm, try again but trim text
            if (text != null) {
                v = getValueByName(text.trim());
            }
        }
        
        if (v == null) {
            v = defaultValue;
        }
        
        setValue(v == null ? null : v.getValue());
    }

    private EnumerationValue getValueByName(String name) {
        for (EnumerationValue v : values.values()) {
            String n = v == null ? null : v.getName();
            if (n == name || (n != null && n.equalsIgnoreCase(name))) {
                return v;
            }
        }
        return null;
    }
}

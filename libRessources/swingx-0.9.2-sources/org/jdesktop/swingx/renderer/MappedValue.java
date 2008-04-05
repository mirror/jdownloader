package org.jdesktop.swingx.renderer;

import javax.swing.Icon;

/**
 * Compound implementation of both StringValue and IconValue. <p>
 * 
 * Quick hack around #590-swingx: LabelProvider should respect StringValue
 * when formatting (instead of going clever with icons).
 * 
 * Note: this will change!
 */
public class MappedValue implements StringValue, IconValue, BooleanValue {

    private StringValue stringDelegate;
    private IconValue iconDelegate;
    private BooleanValue booleanDelegate;

    public MappedValue(StringValue stringDelegate, IconValue iconDelegate) {
        this(stringDelegate, iconDelegate, null);
    }
    
    public MappedValue(StringValue stringDelegate, IconValue iconDelegate, 
            BooleanValue booleanDelegate) {
        this.stringDelegate = stringDelegate;
        this.iconDelegate = iconDelegate;
        this.booleanDelegate = booleanDelegate;
    }
    
    public String getString(Object value) {
        if (stringDelegate != null) {
            return stringDelegate.getString(value);
        }
        return "";
    }

    public Icon getIcon(Object value) {
        if (iconDelegate != null) {
            return iconDelegate.getIcon(value);
        }
        return null;
    }
    
    public boolean getBoolean(Object value) {
        if (booleanDelegate != null) {
            return booleanDelegate.getBoolean(value);
        }
        return false;
    }
    
}
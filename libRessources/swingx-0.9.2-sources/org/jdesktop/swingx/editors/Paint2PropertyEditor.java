package org.jdesktop.swingx.editors;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditorSupport;

/**
 *
 * @author joshy
 */
public class Paint2PropertyEditor extends PropertyEditorSupport {
    Paint paint = new Color(0,128,255);
    PaintPicker picker = new PaintPicker();

    /** Creates a new instance of Paint2PropertyEditor */
    public Paint2PropertyEditor() {
        picker.addPropertyChangeListener("paint",new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                paint = picker.getPaint();
                firePropertyChange();
            }
        });
        
    }
    
    public Paint getValue() {
        return paint;
    }

    public void setValue(Object object) {
        paint = (Paint)object;
        picker.setPaint(paint);
        super.setValue(object);
    }

       
    public String getJavaInitializationString() {
        Paint paint = getValue();
        //TODO!!!
        return paint == null ? "null" : 
            "org.jdesktop.swingx.painter.gradient.LinearGradientPainter.BLACK_STAR";
    }
    
    public void setAsText(String text) throws IllegalArgumentException {
        // do nothing right now
    }
    
    public String getAsText() {
        return "PainterText";
    }
    
    public void paintValue(Graphics g, Rectangle box) {
        Graphics2D g2 = (Graphics2D)g;
        //picker.setPaint(getValue());
        g2.setPaint(picker.getDisplayPaint(box));
        g2.fill(box);
    }
    public boolean isPaintable() {
        return true;
    }
    

    public boolean supportsCustomEditor() {
        return true;
    }

    public Component getCustomEditor() {
        return picker;
    }
    
}

package org.jdesktop.swingx.painter;

import org.jdesktop.swingx.BeanInfoSupport;

/**
 * BeanInfo of TextPainter.
 *
 * @author joshy, Jan Stola
 */
public class TextPainterBeanInfo extends BeanInfoSupport {
    
    /** Creates a new instance of TextPainterBeanInfo */
    public TextPainterBeanInfo() {
        super(TextPainter.class);
    }
    
    protected void initialize() {
        setPreferred(true, "font", "text");
    }
    
}

package org.jdesktop.swingx.painter;

import org.jdesktop.swingx.BeanInfoSupport;

/**
 * BeanInfo of AlphaPainter.
 *
 * @author Jan Stola
 */
public class AlphaPainterBeanInfo extends BeanInfoSupport {
    
    public AlphaPainterBeanInfo() {
        super(AlphaPainter.class);
    }
    
    protected void initialize() {
        setPreferred(true, "alpha");
    }

}

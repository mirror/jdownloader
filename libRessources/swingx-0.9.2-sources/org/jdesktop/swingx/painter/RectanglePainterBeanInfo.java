package org.jdesktop.swingx.painter;

import org.jdesktop.swingx.BeanInfoSupport;

/**
 * BeanInfo of RectanglePainter.
 *
 * @author joshy, Jan Stola
 */
public class RectanglePainterBeanInfo extends BeanInfoSupport {
    
    /** Creates a new instance of RectanglePainterBeanInfo */
    public RectanglePainterBeanInfo() {
        super(RectanglePainter.class);
    }
    
    protected void initialize() {
        setPreferred(true, "roundHeight", "roundWidth", "rounded");
    }

}

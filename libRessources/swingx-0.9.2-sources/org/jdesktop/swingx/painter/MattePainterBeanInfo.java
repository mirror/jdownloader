package org.jdesktop.swingx.painter;

import org.jdesktop.swingx.BeanInfoSupport;

/**
 * BeanInfo of MattePainter.
 *
 * @author joshy, Jan Stola
 */
public class MattePainterBeanInfo extends BeanInfoSupport {
    
    /** Creates a new instance of MattePainterBeanInfo */
    public MattePainterBeanInfo() {
        super(MattePainter.class);
    }

    protected void initialize() {
        setPreferred(true, "fillPaint");
    }
    
}

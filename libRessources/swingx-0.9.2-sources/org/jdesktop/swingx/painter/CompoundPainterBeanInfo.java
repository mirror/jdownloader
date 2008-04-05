package org.jdesktop.swingx.painter;

import org.jdesktop.swingx.BeanInfoSupport;

/**
 * BeanInfo of CompoundPainter.
 *
 * @author Richard, Jan Stola
 */
public class CompoundPainterBeanInfo extends BeanInfoSupport {
    
    /** Creates a new instance of CompoundPainterBeanInfo */
    public CompoundPainterBeanInfo() {
        super(CompoundPainter.class);
    }

    protected void initialize() {
        setPreferred(true, "painters");
    }
}

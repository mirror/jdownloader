package org.jdesktop.swingx.painter;

import org.jdesktop.swingx.BeanInfoSupport;
import org.jdesktop.swingx.editors.Paint2PropertyEditor;

/**
 * BeanInfo of PinstripePainter.
 *
 * @author Richard
 */
public class PinstripePainterBeanInfo extends BeanInfoSupport {
    
    /** Creates a new instance of PinstripePainterBeanInfo */
    public PinstripePainterBeanInfo() {
        super(PinstripePainter.class);
    }

    protected void initialize() {
        setPreferred(true, "angle", "spacing", "paint");
        setPropertyEditor(Paint2PropertyEditor.class, "paint");
    }
}

package org.jdesktop.swingx.painter;

import org.jdesktop.swingx.BeanInfoSupport;
import org.jdesktop.swingx.editors.ShapePropertyEditor;

/**
 * BeanInfo of ShapePainter.
 *
 * @author joshy, Jan Stola
 */
public class ShapePainterBeanInfo extends BeanInfoSupport {
    
    /** Creates a new instance of ShapePainterBeanInfo */
    public ShapePainterBeanInfo() {
        super(ShapePainter.class);
    }
    
    protected void initialize() {
        setPropertyEditor(ShapePropertyEditor.class, "shape");
        setPreferred(true, "shape");
    }

}

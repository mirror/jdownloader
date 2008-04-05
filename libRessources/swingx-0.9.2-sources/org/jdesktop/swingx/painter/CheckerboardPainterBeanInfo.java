package org.jdesktop.swingx.painter;

import org.jdesktop.swingx.BeanInfoSupport;
import org.jdesktop.swingx.editors.PaintPropertyEditor;

/**
 * BeanInfo of CheckerboardPainter.
 *
 * @author Richard, Jan Stola
 */
public class CheckerboardPainterBeanInfo extends BeanInfoSupport {
    
    /** Creates a new instance of CheckerboardPainterBeanInfo */
    public CheckerboardPainterBeanInfo() {
        super(CheckerboardPainter.class);
    }

    protected void initialize() {
        setPreferred(true, "darkPaint", "lightPaint", "squareSize");
        setPropertyEditor(PaintPropertyEditor.class, "darkPaint", "lightPaint");
    }
}

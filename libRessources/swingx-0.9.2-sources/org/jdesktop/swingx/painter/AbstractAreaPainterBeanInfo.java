package org.jdesktop.swingx.painter;

import org.jdesktop.swingx.BeanInfoSupport;
import org.jdesktop.swingx.editors.EnumPropertyEditor;
import org.jdesktop.swingx.editors.Paint2PropertyEditor;

/**
 * BeanInfo of AbstractAreaPainter.
 *
 * @author Jan Stola
 */
public class AbstractAreaPainterBeanInfo extends BeanInfoSupport {    

    public AbstractAreaPainterBeanInfo() {
        super(AbstractAreaPainter.class);
    }
    
    public AbstractAreaPainterBeanInfo(Class clazz) {
        super(clazz);
    }

    protected void initialize() {
        setPropertyEditor(StylePropertyEditor.class, "style");
        setPropertyEditor(Paint2PropertyEditor.class, "fillPaint", "borderPaint");
    }

    public static final class StylePropertyEditor extends EnumPropertyEditor<AbstractAreaPainter.Style> {
        public StylePropertyEditor() {
            super(AbstractAreaPainter.Style.class);
        }
    }

}

package org.jdesktop.swingx.painter;

import org.jdesktop.swingx.BeanInfoSupport;
import org.jdesktop.swingx.editors.EnumPropertyEditor;

/**
 * BeanInfo of AbstractLayoutPainter.
 *
 * @author Jan Stola
 */
public class AbstractLayoutPainterBeanInfo extends BeanInfoSupport {

    public AbstractLayoutPainterBeanInfo() {
        super(AbstractLayoutPainter.class);
    }
    
    public AbstractLayoutPainterBeanInfo(Class clazz) {
        super(clazz);
    }

    protected void initialize() {
        setPropertyEditor(HorizontalAlignmentPropertyEditor.class, "horizontalAlignment");
        setPropertyEditor(VerticalAlignmentPropertyEditor.class, "verticalAlignment");
    }

    public static final class HorizontalAlignmentPropertyEditor extends EnumPropertyEditor<AbstractLayoutPainter.HorizontalAlignment> {
        public HorizontalAlignmentPropertyEditor() {
            super(AbstractLayoutPainter.HorizontalAlignment.class);
        }
    }

    public static final class VerticalAlignmentPropertyEditor extends EnumPropertyEditor<AbstractLayoutPainter.VerticalAlignment> {
        public VerticalAlignmentPropertyEditor() {
            super(AbstractLayoutPainter.VerticalAlignment.class);
        }
    }

}

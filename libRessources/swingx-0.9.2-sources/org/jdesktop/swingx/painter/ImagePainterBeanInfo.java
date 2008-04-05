package org.jdesktop.swingx.painter;

import org.jdesktop.swingx.BeanInfoSupport;
import org.jdesktop.swingx.editors.EnumPropertyEditor;
import org.jdesktop.swingx.editors.ImageEditor;
import org.jdesktop.swingx.editors.ImageURLEditor;

/**
 * BeanInfo of ImagePainter.
 *
 * @author joshy, Jan Stola
 */
public class ImagePainterBeanInfo extends BeanInfoSupport {
    
    /** Creates a new instance of ImagePainterBeanInfo */
    public ImagePainterBeanInfo() {
        super(ImagePainter.class);
    }
    
    protected void initialize() {
        setPropertyEditor(ImageEditor.class,"image");
        setPropertyEditor(ImageURLEditor.class,"imageString");
        setPropertyEditor(ScaleTypePropertyEditor.class, "scaleType");
        setPreferred(true, "image", "imageString", "scaleType", "scaleToFit");
    }

    public static final class ScaleTypePropertyEditor extends EnumPropertyEditor<ImagePainter.ScaleType> {
        public ScaleTypePropertyEditor() {
            super(ImagePainter.ScaleType.class);
        }
    }

}

/*
 * ShapeEffectBeanInfo.java
 *
 * Created on August 23, 2006, 4:54 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jdesktop.swingx.painter.effects;

import org.jdesktop.swingx.BeanInfoSupport;
import org.jdesktop.swingx.editors.Paint2PropertyEditor;
import org.jdesktop.swingx.painter.effects.*;

/**
 *
 * @author joshy
 */
public class AbstractPathEffectBeanInfo extends BeanInfoSupport {
    
    /** Creates a new instance of ShapeEffectBeanInfo */
    public AbstractPathEffectBeanInfo() {
        super(AbstractAreaEffect.class);
    }
    
    protected void initialize() {
        setHidden(true, "class");
        setPropertyEditor(Paint2PropertyEditor.class, "brushColor");
    }
    
}

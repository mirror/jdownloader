package org.jdesktop.swingx;

import java.beans.BeanDescriptor;

/**
 * BeanInfo class for JXTitledPanel.
 *
 * @author Richard, Jan Stola
 */
public class JXTitledPanelBeanInfo extends BeanInfoSupport {

    public JXTitledPanelBeanInfo() {
        super(JXTitledPanel.class);
    }
    
    protected void initialize() {
        BeanDescriptor bd = getBeanDescriptor();
        bd.setValue("containerDelegate", "getContentContainer");
        setPreferred(true, "title", "titleFont", "titleForeground", "titlePainter");
        setPreferred(true, "leftDecoration", "rightDecoration");
        setPreferred(false, "alpha", "border", "inheritAlpha");
    }
}

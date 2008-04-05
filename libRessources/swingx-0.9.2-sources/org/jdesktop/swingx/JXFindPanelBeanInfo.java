package org.jdesktop.swingx;

import java.beans.BeanDescriptor;

/**
 * BeanInfo class for JXFindPanel.
 * 
 * @author Jan Stola
 */
public class JXFindPanelBeanInfo extends BeanInfoSupport {

    public JXFindPanelBeanInfo() {
        super(JXFindPanel.class);
    }

    protected void initialize() {
        BeanDescriptor bd = getBeanDescriptor();
        bd.setValue("isContainer", Boolean.FALSE);        
    }

}

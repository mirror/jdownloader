package org.jdesktop.swingx;

import java.beans.BeanDescriptor;

/**
 * BeanInfo class for JXSearchPanel.
 * 
 * @author Jan Stola
 */
public class JXSearchPanelBeanInfo extends BeanInfoSupport {

    public JXSearchPanelBeanInfo() {
        super(JXSearchPanel.class);
    }

    protected void initialize() {
        BeanDescriptor bd = getBeanDescriptor();
        bd.setValue("isContainer", Boolean.FALSE);        
    }

}

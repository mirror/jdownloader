package org.jdesktop.swingx;

import java.beans.BeanDescriptor;

/**
 * BeanInfo class for JXFindBar.
 * 
 * @author Jan Stola
 */
public class JXFindBarBeanInfo extends BeanInfoSupport {

    public JXFindBarBeanInfo() {
        super(JXFindBar.class);
    }

    protected void initialize() {
        BeanDescriptor bd = getBeanDescriptor();
        bd.setValue("isContainer", Boolean.FALSE);        
    }

}

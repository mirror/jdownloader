package org.jdesktop.swingx;

/**
 * BeanInfo class for JXBusyLabel.
 * 
 * @author Jan Stola
 */
public class JXBusyLabelBeanInfo extends BeanInfoSupport {

    public JXBusyLabelBeanInfo() {
        super(JXBusyLabel.class);        
    }
    
    protected void initialize() {
        setPreferred(true, "busy");
        String iconName = "resources/" + JXBusyLabel.class.getSimpleName();
        setSmallMonoIconName(iconName + "16.png");
        setMonoIconName(iconName + "32.png");
    }
}

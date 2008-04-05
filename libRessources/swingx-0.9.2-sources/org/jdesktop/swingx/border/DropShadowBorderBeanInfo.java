package org.jdesktop.swingx.border;

import org.jdesktop.swingx.BeanInfoSupport;

/**
 * BeanInfo class for DropShadowBorder.
 * 
 * @author Jan Stola
 */
public class DropShadowBorderBeanInfo extends BeanInfoSupport {

    public DropShadowBorderBeanInfo() {
        super(DropShadowBorder.class);        
    }
    
    protected void initialize() {
        setHidden(true, "class");
        String iconName = "/org/jdesktop/swingx/resources/DropShadowBorder";
        String smallIcon = iconName + "16.png";
        setSmallColorIconName(smallIcon);
        setSmallMonoIconName(smallIcon);
        String icon = iconName + "32.png";
        setMonoIconName(icon);
        setColorIconName(icon);
    }

}

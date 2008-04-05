package org.jdesktop.swingx.border;

import javax.swing.SwingConstants;
import org.jdesktop.swingx.BeanInfoSupport;
import org.jdesktop.swingx.EnumerationValue;

/**
 * BeanInfo class for IconBorder.
 * 
 * @author Jan Stola
 */
public class IconBorderBeanInfo extends BeanInfoSupport {

    public IconBorderBeanInfo() {
        super(IconBorder.class);        
    }

    protected void initialize() {
        setHidden(true, "class", "borderOpaque");
        setEnumerationValues(new EnumerationValue[] {
            new EnumerationValue("Leading", SwingConstants.LEADING, "SwingConstants.LEADING"),
            new EnumerationValue("Trailing", SwingConstants.TRAILING, "SwingConstants.TRAILING"),
            new EnumerationValue("East", SwingConstants.EAST, "SwingConstants.EAST"),
            new EnumerationValue("West", SwingConstants.WEST, "SwingConstants.WEST")
        }, "iconPosition");
    }

}

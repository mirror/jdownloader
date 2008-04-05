package org.jdesktop.swingx;

/**
 * BeanInfo class for JXLabel.
 * 
 * @author Jan Stola
 */
public class JXLabelBeanInfo extends BeanInfoSupport {

    public JXLabelBeanInfo() {
        super(JXLabel.class);        
    }
    
    protected void initialize() {
        String iconName = "resources/" + JXLabel.class.getSimpleName();
        setSmallMonoIconName(iconName + "16.png");
        setMonoIconName(iconName + "32.png");
        setPreferred(true, "foregroundPainter", "backgroundPainter");
        setPreferred(true, "lineWrap", "maxLineSpan");
        setPreferred(true, "textRotation");
    }

}

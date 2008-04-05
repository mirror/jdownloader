/*
 * MultiThumbSliderAddon.java
 *
 * Created on May 9, 2006, 4:19 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jdesktop.swingx.plaf;

import org.jdesktop.swingx.JXMultiThumbSlider;

/**
 *
 * @author jm158417
 */
public class MultiThumbSliderAddon extends AbstractComponentAddon {
    
    /** Creates a new instance of MultiThumbSliderAddon */
    public MultiThumbSliderAddon() {
        super("JXMultiThumbSlider");
    }
    
    @Override
    protected void addBasicDefaults(LookAndFeelAddons addon, DefaultsList defaults) {
        super.addBasicDefaults(addon, defaults);
        
        defaults.add(JXMultiThumbSlider.uiClassID,
                "org.jdesktop.swingx.plaf.basic.BasicMultiThumbSliderUI");
    }
    
}

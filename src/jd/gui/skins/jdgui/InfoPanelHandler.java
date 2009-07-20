package jd.gui.skins.jdgui;

import jd.gui.skins.jdgui.interfaces.DroppedPanel;
import jd.gui.skins.jdgui.interfaces.View;

public class InfoPanelHandler {
/**
 * Sets the infopanel, and return the old one or null;
 * @param panel
 * @return 
 */
    public static DroppedPanel setPanel(DroppedPanel panel) {
        View view = JDGui.getInstance().getMainTabbedPane().getSelectedView();
        DroppedPanel old = view.getInfoPanel();
        view.setInfoPanel(panel);
        return old;
    }
    
    
    
    

}

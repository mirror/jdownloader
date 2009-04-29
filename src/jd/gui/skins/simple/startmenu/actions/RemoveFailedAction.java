package jd.gui.skins.simple.startmenu.actions;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

public class RemoveFailedAction extends StartAction {
    /**
     * 
     */
    private static final long serialVersionUID = -5425871515927494136L;

    @Override
    public void init() {
        this.setIconDim(new Dimension(24, 24));
        this.setIcon("gui.images.remove_failed");
        this.setShortDescription("gui.menu.action.remove_dupes.desc");
        this.setName("gui.menu.action.remove_dupes.name");
        this.setMnemonic("gui.menu.action.remove_dupes.mnem", "gui.menu.action.remove_dupes.name");
        this.setAccelerator("gui.menu.action.remove_dupes.accel");
        
    }

    public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub
        
    }
}

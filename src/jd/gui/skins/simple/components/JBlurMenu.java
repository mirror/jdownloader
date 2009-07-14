package jd.gui.skins.simple.components;

import javax.swing.JMenu;

import jd.gui.skins.simple.JTattooUtils;

public class JBlurMenu extends JMenu {

    public JBlurMenu(String string) {
       super(string);
    }

 

    public void updateUI() {
        super.updateUI();
        if (getPopupMenu() != null) {
            getPopupMenu().setUI(JTattooUtils.getPopupUI());
        }
    }

}

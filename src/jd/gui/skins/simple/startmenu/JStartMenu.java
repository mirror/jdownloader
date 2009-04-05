package jd.gui.skins.simple.startmenu;

import javax.swing.JMenu;

import jd.utils.JDLocale;
import jd.utils.JDTheme;

public class JStartMenu extends JMenu {

    public JStartMenu(String name, String icon, String tooltip) {
        super(JDLocale.L(name, null));
        this.setIcon(JDTheme.II(icon, 24, 24));
        this.setToolTipText(JDLocale.L(tooltip, null));
    }

}

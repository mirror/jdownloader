package jd.gui.skins.simple.startmenu;

import javax.swing.JMenu;

import jd.utils.JDLocale;
import jd.utils.JDTheme;

public class JStartMenu extends JMenu {

    private static final long serialVersionUID = -7833871754471332953L;

    public JStartMenu(String name, String icon) {
        super(JDLocale.L(name, null));
        this.setIcon(JDTheme.II(icon, 24, 24));
    }

}

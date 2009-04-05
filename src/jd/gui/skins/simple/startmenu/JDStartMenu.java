package jd.gui.skins.simple.startmenu;

import javax.swing.JMenu;
import javax.swing.JSeparator;

import jd.gui.skins.simple.startmenu.actions.ExitAction;
import jd.gui.skins.simple.startmenu.actions.RestartAction;

public class JDStartMenu {

    public static void createMenu(JMenu menu) {
     
        menu.add(new AddLinksMenu());
        menu.add(new CleanupMenu());
        menu.add(new SaveMenu());
        menu.add(new JSeparator());
        menu.add(new AddonsMenu());
        menu.add(new PremiumMenu());
        menu.add(new JSeparator());
        menu.add(new AboutMenu());
        menu.add(new JSeparator());
        menu.add(new RestartAction());
        menu.add(new ExitAction());
        // addMenu(JDLocale.L("gui.startmenu.exit", "Exit"))

    }

}
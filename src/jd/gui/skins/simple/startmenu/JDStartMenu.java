package jd.gui.skins.simple.startmenu;

import javax.swing.JPopupMenu;

import jd.gui.skins.simple.startmenu.actions.ExitAction;
import jd.gui.skins.simple.startmenu.actions.RestartAction;

public class JDStartMenu {

    public static void createMenu(JPopupMenu popup) {

        popup.add(new AddLinksMenu());
        popup.add(new CleanupMenu());
        // popup.add(new SaveMenu());
        popup.addSeparator();
        popup.add(new AddonsMenu());
        popup.addSeparator();
        popup.add(new AboutMenu());
        popup.addSeparator();
        popup.add(new RestartAction());
        popup.add(new ExitAction());

    }

}
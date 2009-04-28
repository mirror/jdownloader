package jd.gui.skins.simple.startmenu;

import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import jd.gui.skins.simple.startmenu.actions.ExitAction;
import jd.gui.skins.simple.startmenu.actions.RestartAction;

public class JDStartMenu {

    public static void createMenu(JPopupMenu popup) {

        popup.add(new AddLinksMenu());
        popup.add(new CleanupMenu());
        popup.add(new SaveMenu());
        popup.add(new JSeparator());
        popup.add(new AddonsMenu());
        popup.add(new PremiumMenu());
        popup.add(new JSeparator());
        popup.add(new AboutMenu());
        popup.add(new JSeparator());
        popup.add(new RestartAction()); 
        popup.add(new ExitAction());

    }



}
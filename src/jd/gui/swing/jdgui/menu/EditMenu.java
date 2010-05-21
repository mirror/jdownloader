package jd.gui.swing.jdgui.menu;

import javax.swing.JMenu;

import jd.utils.locale.JDL;

public class EditMenu extends JMenu {

    private static final long serialVersionUID = -8722169322222866487L;

    public EditMenu() {
        super(JDL.L("jd.gui.skins.simple.simplegui.menubar.linksmenu", "Links"));

        add(new AddLinksMenu());
        add(new CleanupMenu());
    }

}

package jd.gui.swing.jdgui.menu;

import javax.swing.JMenu;

import org.jdownloader.gui.translate._GUI;

public class EditMenu extends JMenu {

    private static final long serialVersionUID = -8722169322222866487L;

    public EditMenu() {
        super(_GUI._.jd_gui_skins_simple_simplegui_menubar_linksmenu());

        add(new AddLinksMenu());
        add(new CleanupMenu());
    }

}
package jd.gui.swing.jdgui.menu;


 import org.jdownloader.gui.translate.*;
import javax.swing.JMenu;

import jd.utils.locale.JDL;

public class EditMenu extends JMenu {

    private static final long serialVersionUID = -8722169322222866487L;

    public EditMenu() {
        super(T._.jd_gui_skins_simple_simplegui_menubar_linksmenu());

        add(new AddLinksMenu());
        add(new CleanupMenu());
    }

}
package org.jdownloader.gui.views.downloads.table;

import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

public class SubMenu extends JMenu implements MenuListener {
    private SubMenuEditor editor;

    public SubMenu(String title, Icon icon, SubMenuEditor comp) {
        super(title);
        setIcon(icon);
        add(comp);
        this.editor = comp;
        this.addMenuListener(this);

    }

    @Override
    public void menuSelected(MenuEvent e) {
        editor.reload();
    }

    @Override
    public void menuDeselected(MenuEvent e) {
        editor.save();
    }

    @Override
    public void menuCanceled(MenuEvent e) {
        System.out.println("can");
    }
}

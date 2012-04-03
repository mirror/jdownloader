package org.jdownloader.gui.views.downloads.table;

import java.awt.Component;

import javax.swing.JPopupMenu;

import jd.gui.swing.jdgui.menu.MenuEditor;

import org.appwork.utils.logging.Log;

public abstract class SubMenuEditor extends MenuEditor {

    abstract public void reload();

    abstract public void save();

    protected void closeMenu() {
        try {
            getRootPopupMenu().setVisible(false);
        } catch (Throwable t) {
            Log.exception(t);
        }
    }

    public Component add(Component comp) {
        comp.setFocusable(false);
        return super.add(comp);
    }

    protected JPopupMenu getRootPopupMenu() {
        JPopupMenu mp = ((JPopupMenu) getParent());

        while ((mp != null) && mp.getInvoker() != null && (mp.getInvoker().getParent() != null) && (mp.getInvoker().getParent() instanceof JPopupMenu)) {
            mp = (JPopupMenu) mp.getInvoker().getParent();
        }
        return mp;
    }
}

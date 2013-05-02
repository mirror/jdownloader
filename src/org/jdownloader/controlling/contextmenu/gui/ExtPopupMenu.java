package org.jdownloader.controlling.contextmenu.gui;

import java.awt.Component;

import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import org.jdownloader.actions.AppAction;

public class ExtPopupMenu extends JPopupMenu implements ExtMenuInterface {

    protected JMenuItem createActionComponent(Action a) {
        if (((AppAction) a).isToggle()) { return new JCheckBoxMenuItem(a); }

        return super.createActionComponent(a);
    }

    public JMenuItem add(JMenuItem menuItem) {

        return super.add(menuItem);
    }

    public Component add(Component c) {
        // avoid seperators as first component and double seperators
        if (c instanceof JSeparator) {
            if (getComponentCount() == 0) return c;
            if (getComponentCount() > 0 && getComponent(getComponentCount() - 1) instanceof JSeparator) return c;
        }
        return super.add(c);
    }

    @Override
    public void cleanup() {

        while (getComponentCount() > 0 && (getComponent(getComponentCount() - 1)) instanceof JSeparator) {
            remove(getComponentCount() - 1);
        }
        if (getComponentCount() == 0) {
            setEnabled(false);
        }
    }
}

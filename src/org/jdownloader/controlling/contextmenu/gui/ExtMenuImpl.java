package org.jdownloader.controlling.contextmenu.gui;

import java.awt.Component;

import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;

import org.jdownloader.actions.AppAction;

public class ExtMenuImpl extends JMenu implements ExtMenuInterface {

    public ExtMenuImpl(String name) {
        super(name);
        setEnabled(false);
    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    protected JMenuItem createActionComponent(Action a) {
        if (((AppAction) a).isToggle()) { return new JCheckBoxMenuItem(a); }

        return super.createActionComponent(a);
    }

    public JMenuItem add(JMenuItem menuItem) {
        if (menuItem.isEnabled()) {
            setEnabled(true);
        }
        return super.add(menuItem);
    }

    public Component add(Component c) {
        // avoid seperators as first component and double seperators
        if (c instanceof JSeparator) {
            if (getMenuComponentCount() == 0) return c;
            if (getMenuComponentCount() > 0 && getMenuComponent(getMenuComponentCount() - 1) instanceof JSeparator) return c;
        } else {
            if (c.isEnabled()) {
                setEnabled(true);
            }
        }

        return super.add(c);
    }

    @Override
    public void cleanup() {

        while (getMenuComponentCount() > 0 && (getMenuComponent(getMenuComponentCount() - 1)) instanceof JSeparator) {
            remove(getMenuComponentCount() - 1);
        }

        if (getMenuComponentCount() == 0) {

            setEnabled(false);
        }
    }

};

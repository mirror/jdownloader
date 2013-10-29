package org.jdownloader.gui.views.components;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JMenuItem;

/**
 * This is an extended JCheckBoxMenuItem
 */
public class ExtRealCheckBoxMenuItem extends JMenuItem {
    /**
     * 
     */
    private static final long serialVersionUID = -2308936338542479539L;

    /**
     * parameter that says if the underlaying popupmenu closes on click
     */
    private boolean           hideOnClick      = true;

    protected Icon            selIcon;

    protected Icon            unselIcon;

    /**
     * Creates a new Menuitem By action
     * 
     * @param action
     */
    public ExtRealCheckBoxMenuItem(final AbstractAction action) {

        super(action);

        // if (isSelected()) {

        // } else {
        selIcon = new CheckBoxIcon(true);
        unselIcon = new CheckBoxIcon(false);

        setIcon(unselIcon);
        setSelected(action.getValue(AbstractAction.SELECTED_KEY) == Boolean.TRUE);

    }

    public void setSelected(boolean b) {
        super.setSelected(b);
        updateIcon();
        if (getAction() != null) {
            getAction().putValue(AbstractAction.SELECTED_KEY, b);
        }

    }

    protected void updateIcon() {
        if (isSelected()) {
            setIcon(selIcon);
        } else {
            setIcon(unselIcon);
        }
    }

    /**
     * Creates a new MenuItem with name
     * 
     * @param name
     */
    public ExtRealCheckBoxMenuItem(final String name) {
        super(name);
    }

    /**
     * @return the {@link ExtRealCheckBoxMenuItem#hideOnClick}
     * @see ExtRealCheckBoxMenuItem#hideOnClick
     */
    public boolean isHideOnClick() {
        return hideOnClick;
    }

    protected void processMouseEvent(final MouseEvent e) {
        if (!hideOnClick && e.getID() == MouseEvent.MOUSE_RELEASED) {
            setSelected(!isSelected());
            for (final ActionListener al : getActionListeners()) {

                int modifiers = 0;
                final AWTEvent currentEvent = EventQueue.getCurrentEvent();
                if (currentEvent instanceof InputEvent) {
                    modifiers = ((InputEvent) currentEvent).getModifiers();
                } else if (currentEvent instanceof ActionEvent) {
                    modifiers = ((ActionEvent) currentEvent).getModifiers();
                }
                al.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, getActionCommand(), EventQueue.getMostRecentEventTime(), modifiers));

            }
            // doClick(0);
        } else {
            super.processMouseEvent(e);
        }
    }

    /**
     * @param hideOnClick
     *            the {@link ExtRealCheckBoxMenuItem#hideOnClick} to set
     * @see ExtRealCheckBoxMenuItem#hideOnClick
     */
    public void setHideOnClick(final boolean hideOnClick) {
        this.hideOnClick = hideOnClick;
    }

}

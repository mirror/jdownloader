package jd.gui.swing.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;

/**
 * This is an extended JCheckBoxMenuItem
 * 
 * @author Coalado
 */
public class JExtCheckBoxMenuItem extends JCheckBoxMenuItem {

    private static final long serialVersionUID = 1L;
    private boolean hideOnClick = true;

    public JExtCheckBoxMenuItem(String realColumnName) {
        super(realColumnName);
    }

    public JExtCheckBoxMenuItem(AbstractAction action) {
        super(action);
    }

    public boolean isHideOnClick() {
        return hideOnClick;
    }

    /**
     * Default: true
     * 
     * if set to false, the menu does not close when clicking this icon.
     * 
     * @param hideOnClick
     */
    public void setHideOnClick(boolean hideOnClick) {
        this.hideOnClick = hideOnClick;
    }

    protected void processMouseEvent(MouseEvent e) {
        if (!hideOnClick && e.getID() == MouseEvent.MOUSE_RELEASED) {
            for (ActionListener al : this.getActionListeners()) {
                al.actionPerformed(new ActionEvent(this, 0, null));
            }
            doClick(0);
        } else {
            super.processMouseEvent(e);
        }
    }

}

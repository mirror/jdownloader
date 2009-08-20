//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

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

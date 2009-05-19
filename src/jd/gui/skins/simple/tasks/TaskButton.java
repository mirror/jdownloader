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

package jd.gui.skins.simple.tasks;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;

/**
 * This TaskPanel does not have a content pane and thus just is an button with
 * an icon
 * 
 * @author coalado
 */
abstract public class TaskButton extends TaskPanel {

    private static final long serialVersionUID = -4718390587552528132L;

    public TaskButton(String string, ImageIcon ii, String pid) {
        super(string, ii, pid);
        super.setDeligateCollapsed(true);
    }

    private boolean collapsed = false;

    //@Override
    public boolean isCollapsed() {
        return collapsed;
    }

    //@Override
    public void setDeligateCollapsed(boolean collapsed) {
        setCollapsed(collapsed);
    }

    //@Override
    public void setCollapsed(boolean collapsed) {
        super.setCollapsed(true);
        boolean oldValue = isCollapsed();
        this.collapsed = collapsed;

        firePropertyChange("collapsed", oldValue, isCollapsed());
    }

    //@Override
    public void mouseReleased(MouseEvent e) {
        broadcastEvent(new ActionEvent(this, ACTION_CLICK, "Toggle"));
    }

}

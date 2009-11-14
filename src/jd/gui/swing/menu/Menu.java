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

package jd.gui.swing.menu;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.utils.JDUtilities;

public class Menu {

    public static JMenuItem getJMenuItem(MenuAction mi) {
        switch (mi.getType()) {
        case SEPARATOR:
            return null;
        case NORMAL:
            return new JMenuItem(mi);
        case TOGGLE:
            if (JDUtilities.getJavaVersion() >= 1.6) {
                // Togglebuttons for 1.6
                JCheckBoxMenuItem m2 = new JCheckBoxMenuItem(mi);
                return m2;
            } else {
                // 1.5 togle buttons need a changelistener in the menuitem
                final JCheckBoxMenuItem m2 = new JCheckBoxMenuItem(mi);
                mi.addPropertyChangeListener(new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {
                        m2.setSelected(((ToolBarAction) evt.getSource()).isSelected());
                    }
                });
                return m2;
            }
        case CONTAINER:
            JMenu m3 = new JMenu(mi.getTitle());
            m3.setIcon(mi.getIcon());
            JMenuItem c;
            if (mi.getSize() > 0) {
                for (int i = 0; i < mi.getSize(); i++) {
                    c = getJMenuItem(mi.get(i));
                    if (c == null) {
                        m3.addSeparator();
                    } else {
                        m3.add(c);
                    }
                }
            }
            return m3;
        }
        return null;
    }
}

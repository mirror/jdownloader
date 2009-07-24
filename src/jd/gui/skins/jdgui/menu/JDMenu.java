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

package jd.gui.skins.jdgui.menu;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import jd.config.MenuItem;
import jd.gui.skins.jdgui.menu.actions.JDMenuAction;

public class JDMenu {

    public static JMenuItem getJMenuItem(final MenuItem mi) {
        JMenuItem m;
        switch (mi.getType()) {
        case MenuItem.SEPARATOR:
            return null;
        case MenuItem.NORMAL:
            m = new JMenuItem(new JDMenuAction(mi));
            if (mi.getAccelerator() != null) m.setAccelerator(KeyStroke.getKeyStroke(mi.getAccelerator()));
            return m;
        case MenuItem.TOGGLE:
            JCheckBoxMenuItem m2 = new JCheckBoxMenuItem(new JDMenuAction(mi));
            m2.setSelected(mi.isSelected());

            if (mi.getAccelerator() != null) m2.setAccelerator(KeyStroke.getKeyStroke(mi.getAccelerator()));
            return m2;
        case MenuItem.CONTAINER:
            JMenu m3 = new JMenu(mi.getTitle());
            m3.setIcon(mi.getIcon());
            JMenuItem c;
            if (mi.getSize() > 0) for (int i = 0; i < mi.getSize(); i++) {
                c = getJMenuItem(mi.get(i));
                if (c == null) {
                    m3.addSeparator();
                } else {
                    m3.add(c);
                }
            }
            m3.addMenuListener(new MenuListener() {

                public void menuCanceled(MenuEvent e) {
                }

                public void menuDeselected(MenuEvent e) {
                }

                public void menuSelected(MenuEvent e) {
                    JMenu m = (JMenu) e.getSource();
                    m.removeAll();
                    JMenuItem c;
                    if (mi.getSize() == 0) m.setEnabled(false);
                    for (int i = 0; i < mi.getSize(); i++) {
                        c = getJMenuItem(mi.get(i));
                        if (c == null) {
                            m.addSeparator();
                        } else {
                            m.add(c);
                        }

                    }
                }

            });

            return m3;
        }
        return null;
    }

}

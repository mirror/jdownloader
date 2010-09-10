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

package jd.gui.swing.jdgui.menu;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.utils.JDUtilities;

public class MenuAction extends ToolBarAction {

    private static final long     serialVersionUID = 2731508542740902624L;
    private ArrayList<MenuAction> items;

    public MenuAction() {
        super();
    }

    public MenuAction(final String name, final int id) {
        super(name, id);
    }

    public MenuAction(final String name, final String icon) {
        super(name, icon);
    }

    public MenuAction(final Types type) {
        super();
        this.setType(type);
    }

    public void addMenuItem(final MenuAction m) {
        this.getItems().add(m);
        this.setType(Types.CONTAINER);
    }

    public MenuAction get(final int i) {
        return this.getItems().get(i);
    }

    public ArrayList<MenuAction> getItems() {
        if (this.items == null) {
            this.items = new ArrayList<MenuAction>();
        }
        return this.items;
    }

    public int getSize() {
        return this.getItems().size();
    }

    @Override
    public void initDefaults() {
    }

    public void setItems(final ArrayList<MenuAction> mis) {
        if (mis != null && mis.size() > 0) {
            this.setType(Types.CONTAINER);
        }
        this.items = mis;
    }

    public JMenuItem toJMenuItem() {
        switch (this.getType()) {
        case SEPARATOR:
            return null;
        case NORMAL:
            return new JMenuItem(this);
        case TOGGLE:
            if (JDUtilities.getJavaVersion() >= 1.6) {
                // Togglebuttons for 1.6
                final JCheckBoxMenuItem m2 = new JCheckBoxMenuItem(this);

                return m2;
            } else {
                // 1.5 togle buttons need a changelistener in the menuitem
                final JCheckBoxMenuItem m2 = new JCheckBoxMenuItem(this);
                this.addPropertyChangeListener(new PropertyChangeListener() {
                    public void propertyChange(final PropertyChangeEvent evt) {
                        m2.setSelected(((ToolBarAction) evt.getSource()).isSelected());
                    }
                });
                return m2;
            }
        case CONTAINER:
            final JMenu m3 = new JMenu(this.getTitle());
            m3.setIcon(this.getIcon());
            JMenuItem c;
            for (int i = 0; i < this.getSize(); i++) {
                c = this.get(i).toJMenuItem();
                if (c == null) {
                    m3.addSeparator();
                } else {
                    m3.add(c);
                }
            }
            return m3;
        }
        return null;
    }

}

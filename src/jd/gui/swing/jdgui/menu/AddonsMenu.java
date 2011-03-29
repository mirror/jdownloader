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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import jd.gui.swing.jdgui.actions.ActionController;
import jd.gui.swing.jdgui.actions.ToolBarAction.Types;
import jd.utils.JDTheme;

import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.translate.JDT;

public class AddonsMenu extends JMenu {

    private static final long serialVersionUID = 1019851981865519325L;
    private static AddonsMenu INSTANCE         = null;

    private AddonsMenu() {
        super(JDT._.gui_menu_extensions());
        updateMenu();
    }

    public void update() {
        this.removeAll();
        updateMenu();

    }

    public static AddonsMenu getInstance() {
        if (INSTANCE == null) INSTANCE = new AddonsMenu();
        return INSTANCE;
    }

    private void updateMenu() {
        this.add(ActionController.getToolBarAction("addonsMenu.configuration"));

        ArrayList<JMenuItem> itemsWithSubmenu = new ArrayList<JMenuItem>();
        ArrayList<JMenuItem> itemsToggle = new ArrayList<JMenuItem>();
        ArrayList<JMenuItem> itemsPress = new ArrayList<JMenuItem>();
        ArrayList<AbstractExtension> pluginsOptional = ExtensionController.getInstance().getExtensions();
        Collections.sort(pluginsOptional, new Comparator<AbstractExtension>() {

            public int compare(AbstractExtension o1, AbstractExtension o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        for (final AbstractExtension plg : pluginsOptional) {
            if (!plg.isEnabled()) continue;
            ArrayList<MenuAction> mis = plg.getMenuAction();
            if (mis != null && !mis.isEmpty()) {
                if (mis.size() == 1) {
                    JMenuItem c = mis.get(0).toJMenuItem();
                    c.setIcon(JDTheme.II(plg.getIconKey(), 16, 16));
                    if (mis.get(0).getType() == Types.TOGGLE) {
                        itemsToggle.add(c);
                    } else {
                        itemsPress.add(c);
                    }
                } else {
                    MenuAction m = new MenuAction(plg.getConfigID(), plg.getName(), plg.getIconKey());
                    m.setItems(mis);

                    JMenuItem mi = m.toJMenuItem();
                    itemsWithSubmenu.add(mi);
                }
            }
        }

        boolean pre = false;
        for (JMenuItem jmi : itemsWithSubmenu) {
            if (!pre) {
                addSeparator();
                pre = true;
            }
            add(jmi);
        }

        pre = false;
        for (JMenuItem jmi : itemsPress) {
            if (!pre) {
                addSeparator();
                pre = true;
            }
            add(jmi);
        }

        pre = false;
        for (JMenuItem jmi : itemsToggle) {
            if (!pre) {
                addSeparator();
                pre = true;
            }
            add(jmi);
        }
    }
}

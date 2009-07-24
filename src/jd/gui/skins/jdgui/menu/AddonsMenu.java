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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JMenuItem;

import jd.OptionalPluginWrapper;
import jd.config.MenuItem;
import jd.gui.skins.simple.SimpleGUI;
import jd.plugins.Plugin;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class AddonsMenu extends JStartMenu {

    private static final long serialVersionUID = 1019851981865519325L;

    public AddonsMenu() {
        super("gui.menu.addons", "gui.images.config.addons");

        updateMenu();
    }

    private void updateMenu() {
        /**
         * 
         * TODO
         */
//        this.add(new AddonConfiguration());

        ArrayList<JMenuItem> itemsWithSubmenu = new ArrayList<JMenuItem>();
        ArrayList<JMenuItem> itemsToggle = new ArrayList<JMenuItem>();
        ArrayList<JMenuItem> itemsPress = new ArrayList<JMenuItem>();
        ArrayList<JMenuItem> itemsConfig = new ArrayList<JMenuItem>();
        for (final OptionalPluginWrapper plg : OptionalPluginWrapper.getOptionalWrapper()) {
            if (!plg.isLoaded() || !plg.isEnabled()) continue;
            boolean config = false;
            ArrayList<MenuItem> mis = plg.getPlugin().createMenuitems();
            if (mis == null && plg.getPlugin().getConfig() != null && plg.getPlugin().getConfig().getEntries().size() > 0) {
                mis = new ArrayList<MenuItem>();
                config = true;
            }
            if (mis != null) {
                if (plg.getPlugin().getConfig() != null && plg.getPlugin().getConfig().getEntries().size() > 0) {
                    MenuItem mi;
                    mis.add(0, mi = new MenuItem(JDL.LF("gui.startmenu.addons.config2", "%s's settings", plg.getHost()), -10000));
                    mi.setProperty("PLUGIN", plg.getPlugin());
                    mi.setIcon(JDTheme.II(plg.getPlugin().getIconKey(), 16, 16));
                    mi.setActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent e) {
                            SimpleGUI.displayConfig(((Plugin) ((MenuItem) e.getSource()).getProperty("PLUGIN")).getConfig(), false);
                        }

                    });

                }
                if (mis.size() > 1) {

                    MenuItem m = new MenuItem(MenuItem.CONTAINER, plg.getPlugin().getHost(), 0);
                    m.setIcon(JDTheme.II(plg.getPlugin().getIconKey(), 16, 16));
                    m.setItems(mis);
                    JMenuItem mi = JDMenu.getJMenuItem(m);
                    mi.setIcon(m.getIcon());
                    if (mi != null) {
                        itemsWithSubmenu.add(mi);
                    } else {
                        addSeparator();
                    }
                } else {
                    for (MenuItem mi : mis) {
                        JMenuItem c = JDMenu.getJMenuItem(mi);
                        c.setDisabledIcon(null);
                        c.setIcon(JDTheme.II(plg.getPlugin().getIconKey(), 16, 16));
                        c.setSelectedIcon(JDTheme.II(plg.getPlugin().getIconKey(), 16, 16));
                        c.setDisabledSelectedIcon(null);
                        if (mi.getType() == MenuItem.TOGGLE) {
                            itemsToggle.add(c);
                        } else {
                            if (config) {
                                itemsConfig.add(c);
                            } else {
                                itemsPress.add(c);
                            }
                        }
                        break;
                    }
                }
            }
        }
        boolean c = itemsConfig.size() > 0;
        boolean p = itemsPress.size() > 0;
        boolean t = itemsToggle.size() > 0;
        boolean pre = false;
        for (JMenuItem jmi : itemsWithSubmenu) {
            if (!pre) addSeparator();
            add(jmi);
            pre = true;
        }

        if (pre && (c || p || t)) addSeparator();
        pre = false;
        for (JMenuItem jmi : itemsConfig) {
            add(jmi);
            pre = true;
        }
        if (pre && (p || t)) addSeparator();
        pre = false;
        for (JMenuItem jmi : itemsPress) {
            add(jmi);
            pre = true;
        }
        pre = false;
        if (pre && t) addSeparator();
        for (JMenuItem jmi : itemsToggle) {
            add(jmi);
        }
    }
}

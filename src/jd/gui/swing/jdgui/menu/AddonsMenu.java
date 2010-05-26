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

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import jd.OptionalPluginWrapper;
import jd.gui.UserIF;
import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class AddonsMenu extends JMenu {

    private static final long serialVersionUID = 1019851981865519325L;
    private static AddonsMenu INSTANCE = null;

    private AddonsMenu() {
        super(JDL.L("gui.menu.addons", "Addons"));

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

        ToolBarAction cfg = new ToolBarAction("addonsMenu.configuration", 9999) {
            private static final long serialVersionUID = -3613887193435347389L;

            public void onAction(ActionEvent e) {
                SwingGui.getInstance().requestPanel(UserIF.Panels.ADDON_MANAGER, null);
            }

            @Override
            public void init() {
            }

            @Override
            public void initDefaults() {
            }
        };
        cfg.setIcon(JDTheme.II("gui.images.config.packagemanager", 16, 16));
        this.add(cfg);

        ArrayList<JMenuItem> itemsWithSubmenu = new ArrayList<JMenuItem>();
        ArrayList<JMenuItem> itemsToggle = new ArrayList<JMenuItem>();
        ArrayList<JMenuItem> itemsPress = new ArrayList<JMenuItem>();
        ArrayList<JMenuItem> itemsConfig = new ArrayList<JMenuItem>();
        ArrayList<OptionalPluginWrapper> pluginsOptional = new ArrayList<OptionalPluginWrapper>(OptionalPluginWrapper.getOptionalWrapper());
        Collections.sort(pluginsOptional);
        for (final OptionalPluginWrapper plg : pluginsOptional) {
            if (!plg.isLoaded() || !plg.isEnabled()) continue;
            boolean config = false;
            ArrayList<MenuAction> mis = plg.getPlugin().createMenuitems();
            if (mis == null && plg.getPlugin().getConfig() != null && plg.getPlugin().getConfig().getEntries().size() > 0) {
                mis = new ArrayList<MenuAction>();
                config = true;
            }
            if (mis != null) {
                if (mis.size() > 1) {
                    MenuAction m = new MenuAction(plg.getID(), 0);
                    m.setTitle(plg.getHost());
                    m.setIcon(JDTheme.II(plg.getPlugin().getIconKey(), 16, 16));
                    m.setItems(mis);
                    JMenuItem mi = m.toJMenuItem();
                    if (mi != null) {
                        mi.setIcon(m.getIcon());
                        itemsWithSubmenu.add(mi);
                    } else {
                        addSeparator();
                    }
                } else {
                    for (MenuAction mi : mis) {
                        JMenuItem c = mi.toJMenuItem();
                        c.setDisabledIcon(null);
                        c.setIcon(JDTheme.II(plg.getPlugin().getIconKey(), 16, 16));
                        c.setSelectedIcon(JDTheme.II(plg.getPlugin().getIconKey(), 16, 16));
                        c.setDisabledSelectedIcon(null);
                        if (mi.getType() == ToolBarAction.Types.TOGGLE) {
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

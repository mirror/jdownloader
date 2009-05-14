package jd.gui.skins.simple.startmenu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JMenuItem;
import javax.swing.JSeparator;

import jd.OptionalPluginWrapper;
import jd.config.MenuItem;
import jd.gui.skins.simple.JDMenu;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.startmenu.actions.AddonConfiguration;
import jd.plugins.Plugin;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

public class AddonsMenu extends JStartMenu {

    private static final long serialVersionUID = 1019851981865519325L;
    static public AddonsMenu INSTANCE;

    public AddonsMenu() {
        super("gui.menu.addons", "gui.images.config.addons", "gui.menu.addons.desc");

        updateMenu();
        INSTANCE = this;
    }

    public void updateMenu() {
        // Component temp = (menAddons.getComponentCount() != 0) ?
        // menAddons.getComponent(0) :
        // SimpleGUI.createMenuItem(this.actionOptionalConfig);
        removeAll();
        // menAddons.add(temp);
        // menAddons.addSeparator();
        this.add(new AddonConfiguration());

        ArrayList<JMenuItem> itemsWithSubmenu = new ArrayList<JMenuItem>();
        ArrayList<JMenuItem> itemsToggle = new ArrayList<JMenuItem>();
        ArrayList<JMenuItem> itemsPress = new ArrayList<JMenuItem>();
        ArrayList<JMenuItem> itemsConfig = new ArrayList<JMenuItem>();
        for (final OptionalPluginWrapper plg : OptionalPluginWrapper.getOptionalWrapper()) {
            if (!plg.isLoaded() || !JDUtilities.getConfiguration().getBooleanProperty(plg.getConfigParamKey(), false)) continue;
            boolean config = false;
            ArrayList<MenuItem> mis = plg.getPlugin().createMenuitems();
            if (mis == null && plg.getPlugin().getConfig() != null && plg.getPlugin().getConfig().getEntries().size() > 0) {
                mis = new ArrayList<MenuItem>();
                config = true;
            }
            if (mis != null && JDUtilities.getConfiguration().getBooleanProperty(plg.getConfigParamKey(), false)) {

                if (plg.getPlugin().getConfig() != null && plg.getPlugin().getConfig().getEntries().size() > 0) {
                    MenuItem mi;
                    mis.add(0, mi = new MenuItem(JDLocale.LF("gui.startmenu.addons.config2", "%s's settings", plg.getHost()), -10000));
                    mi.setProperty("PLUGIN", plg.getPlugin());
                    mi.setIcon(JDTheme.II(plg.getPlugin().getIconKey(), 16, 16));
                    mi.setActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent e) {
                            ((MenuItem) e.getSource()).getProperty("PLUGIN");
                            SimpleGUI.displayConfig(((Plugin) ((MenuItem) e.getSource()).getProperty("PLUGIN")).getConfig(), 0);
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

                        // ((JMenu) mi).removeMenuListener(((JMenu)
                        // mi).getMenuListeners()[0]);
                        // ((JMenu) mi).addMenuListener(new MenuListener() {
                        // public void menuCanceled(MenuEvent e) {
                        // }
                        //
                        // public void menuDeselected(MenuEvent e) {
                        // }
                        //
                        // public void menuSelected(MenuEvent e) {
                        // JMenu m = (JMenu) e.getSource();
                        //
                        // m.removeAll();
                        // for (MenuItem menuItem :
                        // plg.getPlugin().createMenuitems()) {
                        // JMenuItem c = JDMenu.getJMenuItem(menuItem);
                        //
                        // if (c == null) {
                        // m.addSeparator();
                        // } else {
                        // m.add(c);
                        // }
                        // }
                        // }
                        //
                        // });
                    } else {
                        addSeparator();
                    }
                } else {
                    for (MenuItem mi : mis) {
                        JMenuItem c = JDMenu.getJMenuItem(mi);
                        c.setDisabledIcon(null);
                        c.setIcon(JDTheme.II(plg.getPlugin().getIconKey(), 16, 16));
                        c.setSelectedIcon(JDTheme.II(plg.getPlugin().getIconKey(), 16, 16));
                        // c.setSelectedIcon(null);
                        c.setDisabledSelectedIcon(null);
                        if (mi.getID() == MenuItem.TOGGLE) {
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
            if (!pre) add(new JSeparator());
            add(jmi);
            pre = true;
        }

        if (pre && (c || p || t)) add(new JSeparator());
        pre = false;
        for (JMenuItem jmi : itemsConfig) {
            add(jmi);
            pre = true;
        }
        if (pre && (p || t)) add(new JSeparator());
        pre = false;
        for (JMenuItem jmi : itemsPress) {
            add(jmi);
            pre = true;
        }
        pre = false;
        if (pre && t) add(new JSeparator());
        for (JMenuItem jmi : itemsToggle) {
            add(jmi);
        }

        // if (menAddons.getItem(menAddons.getItemCount() - 1) == null) {
        // menAddons.remove(menAddons.getItemCount() - 1);
        // }
    }
}

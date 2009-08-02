package jd.gui.swing.menu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;

import jd.HostPluginWrapper;
import jd.config.MenuAction;
import jd.controlling.AccountController;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class HosterMenu extends Menu {

    public static void update(JComponent c) {

        PluginForHost plugin;
        JMenu pluginPopup;
        JMenuItem mi;
        ArrayList<HostPluginWrapper> hosts = JDUtilities.getPluginsForHost();
        Collections.sort(hosts, new Comparator<HostPluginWrapper>() {

            public int compare(HostPluginWrapper o1, HostPluginWrapper o2) {

                return o1.getHost().compareToIgnoreCase(o2.getHost());
            }

        });

        for (HostPluginWrapper wrapper : hosts) {
            if (!wrapper.isLoaded() || !wrapper.isPremiumEnabled() || !AccountController.getInstance().hasAccounts(wrapper.getHost())) continue;
            plugin = wrapper.getPlugin();
            pluginPopup = new JMenu(wrapper.getHost());
            if (plugin.hasHosterIcon()) pluginPopup.setIcon(plugin.getHosterIcon());
            for (MenuAction next : plugin.createMenuitems()) {

                mi = getJMenuItem(next);
                if (mi == null) {
                    pluginPopup.addSeparator();
                } else {
                    pluginPopup.add(mi);
                }
            }
            c.add(pluginPopup);
        }

        c.add(new JSeparator());
        int entries = 10;
        int menus = ('z' - 'a') / entries + 1;
        JMenu[] jmenus = new JMenu[menus];
        JMenu num = new JMenu("0 - 9");
        c.add(num);
        for (HostPluginWrapper wrapper : hosts) {
            if (!wrapper.isLoaded() || !wrapper.isPremiumEnabled()) continue;
            char ccv = wrapper.getHost().toLowerCase().charAt(0);
            JMenu menu = null;
            if (ccv >= '0' && ccv <= '9') {
                menu = num;
            } else {
                int index = ((ccv - 'a')) / entries;
                if (jmenus[index] == null) {
                    int start = 'a' + index * entries;
                    int end = Math.min('a' + ((1 + index) * entries) - 1,'z');
                    jmenus[index] = new JMenu(JDL.LF("jd.gui.swing.menu.HosterMenu", "Hoster %s", new String(new byte[] { (byte) (start) }).toUpperCase() + " - " + new String(new byte[] { (byte) (end) }).toUpperCase()));
                    c.add(jmenus[index]);
                }
                menu = jmenus[index];
            }

            plugin = wrapper.getPlugin();
            pluginPopup = new JMenu(wrapper.getHost());
            if (plugin.hasHosterIcon()) pluginPopup.setIcon(plugin.getHosterIcon());
            for (MenuAction next : plugin.createMenuitems()) {

                mi = getJMenuItem(next);
                if (mi == null) {
                    pluginPopup.addSeparator();
                } else {
                    pluginPopup.add(mi);
                }
            }
            menu.add(pluginPopup);
        }
    }
}

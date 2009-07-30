package jd.gui.swing.menu;

import java.util.ArrayList;
import java.util.Collections;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import jd.HostPluginWrapper;
import jd.config.MenuItem;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

public class HosterMenu extends Menu {

    public static  void update(JComponent c) {
    
        PluginForHost plugin;
        JMenu pluginPopup;
        JMenuItem mi;
        ArrayList<HostPluginWrapper> hosts = JDUtilities.getPluginsForHost();
        Collections.sort(hosts);
        for (HostPluginWrapper wrapper : hosts) {
            if (!wrapper.isLoaded() || !wrapper.isPremiumEnabled()) continue;
            plugin = wrapper.getPlugin();
            pluginPopup = new JMenu(wrapper.getHost());
            if (plugin.hasHosterIcon()) pluginPopup.setIcon(plugin.getHosterIcon());
            for (MenuItem next : plugin.createMenuitems()) {
             
                mi = getJMenuItem(next);
                if (mi == null) {
                    pluginPopup.addSeparator();
                } else {
                    pluginPopup.add(mi);
                }
            }
            c.add(pluginPopup);
        }
    }

}

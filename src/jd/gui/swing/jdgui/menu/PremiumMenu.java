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
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.Timer;

import jd.HostPluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.AccountControllerEvent;
import jd.controlling.AccountControllerListener;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.jdgui.actions.ActionController;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

public class PremiumMenu extends JMenu implements ActionListener, AccountControllerListener {

    private static final long serialVersionUID = 5075413754334671773L;

    private static PremiumMenu INSTANCE;

    private Timer updateAsync;

    private PremiumMenu() {
        super(JDL.L("gui.menu.premium", "Premium"));

        updateAsync = new Timer(250, this);
        updateAsync.setInitialDelay(250);
        updateAsync.setRepeats(false);
        updateMenu();
        AccountController.getInstance().addListener(this);
    }

    public static PremiumMenu getInstance() {
        if (INSTANCE == null) INSTANCE = new PremiumMenu();
        return INSTANCE;
    }

    private void updateMenu() {
        this.add(new JCheckBoxMenuItem(ActionController.getToolBarAction("premiumMenu.toggle")));
        this.add(ActionController.getToolBarAction("premiumMenu.configuration"));
        this.addSeparator();
        this.updateHosts();
    }

    private void updateHosts() {
        boolean addedEntry = false;

        PluginForHost plugin;
        JMenu pluginPopup;
        JMenuItem mi;
        ArrayList<HostPluginWrapper> hosts = new ArrayList<HostPluginWrapper>(HostPluginWrapper.getHostWrapper());
        Collections.sort(hosts, new Comparator<HostPluginWrapper>() {

            public int compare(HostPluginWrapper o1, HostPluginWrapper o2) {
                return o1.getHost().compareToIgnoreCase(o2.getHost());
            }

        });

        for (HostPluginWrapper wrapper : hosts) {
            if (!wrapper.isLoaded() || !wrapper.isPremiumEnabled() || !AccountController.getInstance().hasAccounts(wrapper.getHost())) continue;
            if (!wrapper.isEnabled()) continue;
            plugin = wrapper.getPlugin();
            pluginPopup = new JMenu(wrapper.getHost());
            pluginPopup.setIcon(plugin.getHosterIconScaled());
            ArrayList<MenuAction> nexts = plugin.createMenuitems();
            for (MenuAction next : nexts) {
                mi = next.toJMenuItem();
                if (mi == null) {
                    pluginPopup.addSeparator();
                } else {
                    pluginPopup.add(mi);
                }
            }
            this.add(pluginPopup);
            addedEntry = true;
        }

        if (addedEntry) this.addSeparator();
        int entries = 6;
        int menus = ('z' - 'a') / entries + 1;
        JMenu[] jmenus = new JMenu[menus];
        JMenu num = new JMenu(JDL.LF("jd.gui.swing.menu.HosterMenu", "Hoster %s", "0 - 9"));
        this.add(num);
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
                    int end = Math.min('a' + ((1 + index) * entries) - 1, 'z');
                    jmenus[index] = new JMenu(JDL.LF("jd.gui.swing.menu.HosterMenu", "Hoster %s", new String(new byte[] { (byte) (start) }).toUpperCase() + " - " + new String(new byte[] { (byte) (end) }).toUpperCase()));
                    this.add(jmenus[index]);
                }
                menu = jmenus[index];
            }

            plugin = wrapper.getPlugin();
            pluginPopup = new JMenu(wrapper.getHost());
            pluginPopup.setIcon(plugin.getHosterIconScaled());
            ArrayList<MenuAction> nexts = plugin.createMenuitems();

            for (MenuAction next : nexts) {
                mi = next.toJMenuItem();
                if (mi == null) {
                    pluginPopup.addSeparator();
                } else {
                    pluginPopup.add(mi);
                }
            }
            menu.add(pluginPopup);
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == updateAsync) {
            new GuiRunnable<Object>() {
                @Override
                public Object runSave() {
                    update();
                    return null;
                }
            }.start();
        }
    }

    public void update() {
        this.removeAll();
        updateMenu();
    }

    public void onAccountControllerEvent(AccountControllerEvent event) {
        switch (event.getEventID()) {
        case AccountControllerEvent.ACCOUNT_ADDED:
        case AccountControllerEvent.ACCOUNT_REMOVED:
            updateAsync.restart();
            break;
        }
    }

}

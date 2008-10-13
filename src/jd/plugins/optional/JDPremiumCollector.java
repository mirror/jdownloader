//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.plugins.optional;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import jd.HostPluginWrapper;
import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.MenuItem;
import jd.config.SubConfiguration;
import jd.gui.skins.simple.SimpleGUI;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.PluginForHost;
import jd.plugins.PluginOptional;
import jd.utils.JDUtilities;

public class JDPremiumCollector extends PluginOptional {

    private SubConfiguration subConfig = JDUtilities.getSubConfig("JDPREMIUMCOLLECTOR");
    private static final String PROPERTY_API_URL = "PROPERTY_API_URL";
    private static final String PROPERTY_LOGIN_USER = "PROPERTY_LOGIN_USER";
    private static final String PROPERTY_LOGIN_PASS = "PROPERTY_LOGIN_PASS";

    public JDPremiumCollector(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof MenuItem && ((MenuItem) e.getSource()).getActionID() == 0) {
            try {
                String post = "type=list";
                post += "&apiuser=" + subConfig.getStringProperty(PROPERTY_LOGIN_USER);
                post += "&apipassword=" + subConfig.getStringProperty(PROPERTY_LOGIN_PASS);
                br.postPage(subConfig.getStringProperty(PROPERTY_API_URL), post);

                if (br.containsHTML("Login faild")) {
                    JOptionPane.showMessageDialog(SimpleGUI.CURRENTGUI.getFrame(), "Wrong username/password!", "Error!", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                ArrayList<HostPluginWrapper> pluginsForHost = JDUtilities.getPluginsForHost();
                String[][] accs = br.getRegex("<premacc id=\"(.*?)\">.*?<login>(.*?)</login>.*?<password>(.*?)</password>.*?<hoster>(.*?)</hoster>.*?</premacc>").getMatches();

                for (HostPluginWrapper plg : pluginsForHost) {
                    if (!plg.isPremiumEnabled()) continue;

                    ArrayList<Account> accounts = new ArrayList<Account>();
                    for (String[] acc : accs) {
                        if (acc[3].equalsIgnoreCase(plg.getHost())) {
                            Account account = new Account(acc[1], acc[2]);
                            try {
                                AccountInfo accInfo = plg.getPlugin().getAccountInformation(account);
                                if (accInfo != null && accInfo.isValid() && !accInfo.isExpired()) accounts.add(account);
                            } catch (Exception e1) {
                                e1.printStackTrace();
                            }
                        }
                    }

                    if (accounts.size() == 0) continue;
                    if (JOptionPane.showConfirmDialog(SimpleGUI.CURRENTGUI.getFrame(), String.format("Found %s accounts for %s plugin! Replace old saved accounts with new accounts?", accounts.size(), plg.getHost()), "Accounts found!", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) continue;

                    plg.getPluginConfig().setProperty(PluginForHost.PROPERTY_PREMIUM, accounts);
                    plg.getPluginConfig().save();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
                JOptionPane.showMessageDialog(SimpleGUI.CURRENTGUI.getFrame(), "Probably wrong URL! See log for more infos!", "Error!", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public String getRequirements() {
        return "JRE 1.5+";
    }

    @Override
    public boolean initAddon() {
        initConfigEntries();
        return true;
    }

    private void initConfigEntries() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, subConfig, PROPERTY_API_URL, "API-URL").setDefaultValue("http://www.yourservicehere.org/api.php"));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, subConfig, PROPERTY_LOGIN_USER, "Username").setDefaultValue("YOUR_USER"));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, subConfig, PROPERTY_LOGIN_PASS, "Password").setDefaultValue("YOUR_PASS"));
    }

    @Override
    public void onExit() {
    }

    @Override
    public ArrayList<MenuItem> createMenuitems() {
        ArrayList<MenuItem> menu = new ArrayList<MenuItem>();

        menu.add(new MenuItem(MenuItem.NORMAL, "Fetch Accounts", 0).setActionListener(this));

        return menu;
    }

    @Override
    public String getHost() {
        return "JD PremiumCollector";
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    public static int getAddonInterfaceVersion() {
        return 2;
    }

}
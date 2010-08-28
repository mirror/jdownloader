//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.plugins.optional.jdpremclient;

import java.util.ArrayList;
import java.util.HashMap;

import jd.HostPluginWrapper;
import jd.Main;
import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.config.SubConfiguration;
import jd.config.ConfigEntry.PropertyType;
import jd.controlling.AccountController;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.plugins.Account;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginForHost;
import jd.plugins.PluginOptional;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

class PremShareHost extends HostPluginWrapper {

    private HostPluginWrapper replacedone = null;

    public PremShareHost(String host, String className, String patternSupported, int flags) {
        super(host, "jd.plugins.optional.jdpremclient.", className, patternSupported, flags, "$Revision$");
        for (HostPluginWrapper wrapper : HostPluginWrapper.getHostWrapper()) {
            if (wrapper.getPattern().toString().equalsIgnoreCase(patternSupported) && wrapper != this) replacedone = wrapper;
        }
        if (replacedone != null) {
            HostPluginWrapper.getHostWrapper().remove(replacedone);
        }
    }

    public HostPluginWrapper getReplacedPlugin() {
        return replacedone;
    }

    @Override
    public synchronized PluginForHost getPlugin() {
        PluginForHost tmp = super.getPlugin();
        if (replacedone != null) {
            ((JDPremInterface) tmp).setReplacedPlugin(replacedone.getPlugin());
        }
        return tmp;
    }

    @Override
    public PluginForHost getNewPluginInstance() {
        PluginForHost tmp = super.getNewPluginInstance();
        if (replacedone != null) {
            ((JDPremInterface) tmp).setReplacedPlugin(replacedone.getNewPluginInstance());
        }
        return tmp;
    }

    @Override
    public String getVersion() {
        if (replacedone != null) return replacedone.getVersion();
        return super.getVersion();
    }

    @Override
    public boolean isEnabled() {
        if (replacedone != null) return replacedone.isEnabled();
        return super.isEnabled();
    }

    @Override
    public void setEnabled(final boolean bool) {
        if (replacedone != null) {
            replacedone.setEnabled(bool);
        } else {
            super.setEnabled(bool);
        }
    }

    @Override
    public boolean isAGBChecked() {
        if (replacedone != null) return replacedone.isAGBChecked();
        return true;
    }

    @Override
    public void setAGBChecked(final Boolean value) {
        if (replacedone != null) {
            replacedone.setAGBChecked(value);
        }
    }

    @Override
    public SubConfiguration getPluginConfig() {
        if (replacedone != null) {
            return replacedone.getPluginConfig();
        } else {
            return super.getPluginConfig();
        }
    }

    @Override
    public boolean hasConfig() {
        if (replacedone != null) {
            return replacedone.hasConfig();
        } else {
            return super.hasConfig();
        }
    }

    @Override
    public String getConfigName() {
        if (replacedone != null) {
            return replacedone.getConfigName();
        } else {
            return super.getConfigName();
        }
    }

}

@OptionalPlugin(rev = "$Revision$", defaultEnabled = true, id = "jdpremium", interfaceversion = 5)
public class JDPremium extends PluginOptional {

    private static final Object LOCK = new Object();
    private static boolean replaced = false;
    private static boolean init = false;
    private static boolean enabled = false;
    private static String jdpremServer = null;
    private static boolean preferLocalAccounts = false;

    private static final HashMap<String, String> premShareHosts = new HashMap<String, String>();

    public JDPremium(PluginWrapper wrapper) {
        super(wrapper);
        config.setGroup(new ConfigGroup(getHost(), getIconKey()));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, this.getPluginConfig(), "SERVER", "JDPremServer: (Restart required)").setPropertyType(PropertyType.NEEDS_RESTART));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), "PREFERLOCALACCOUNTS", "Prefer local Premium Accounts(restart required)?").setDefaultValue(false).setPropertyType(PropertyType.NEEDS_RESTART));
    }

    private void replaceHosterPlugin(String host, String with) {
        PluginForHost old = JDUtilities.getPluginForHost(host);
        if (old != null) {
            logger.info("Replacing " + host + " Plugin with JDPremium: " + with);
            new PremShareHost(old.getHost(), with, old.getWrapper().getPattern().toString(), old.getWrapper().getFlags() + PluginWrapper.ALLOW_DUPLICATE);
        }
    }

    @Override
    public boolean initAddon() {
        jdpremServer = getPluginConfig().getStringProperty("SERVER", null);
        preferLocalAccounts = getPluginConfig().getBooleanProperty("PREFERLOCALACCOUNTS", false);
        synchronized (LOCK) {
            if (Main.isInitComplete() && replaced == false) {
                logger.info("JDPremium: cannot be initiated during runtime. JDPremium must be enabled at startup!");
                return false;
            }
            if (!init) {
                /* init our new plugins */
                premShareHosts.put("jdownloader.org", "PremShare");
                premShareHosts.put("ochload.org", "Ochloadorg");
                int replaceIndex = 0;
                for (String key : premShareHosts.keySet()) {
                    /* init replacePlugin */
                    try {
                        new PremShareHost(key, premShareHosts.get(key), "NEVERUSETHISREGEX" + replaceIndex++ + ":\\)", 2);
                    } catch (Throwable e) {
                    }
                }
                init = true;
            }
            if (!replaced) {
                /* get all current PremiumPlugins */
                ArrayList<HostPluginWrapper> all = JDUtilities.getPremiumPluginsForHost();
                for (String key : premShareHosts.keySet()) {
                    if (AccountController.getInstance().hasAccounts(key)) {
                        for (HostPluginWrapper plugin : all) {
                            if (plugin.getHost().contains("youtube")) continue;
                            if (plugin.getHost().contains("DIRECTHTTP")) continue;
                            /* do not replace our new plugins ;) */
                            if (premShareHosts.containsKey(plugin.getHost())) continue;
                            replaceHosterPlugin(plugin.getHost(), premShareHosts.get(key));
                        }
                        PluginForHost ret = JDUtilities.getPluginForHost(key);
                        if (ret != null && ret instanceof JDPremInterface) {
                            ((JDPremInterface) ret).enablePlugin();
                        }
                    }
                }
                replaced = true;
            }
            if (replaced) {
                logger.info("JDPremium: init ok! plugins replaced!");
            } else {
                logger.info("JDPremium: init ok! no valid accounts found, no plugins replaced! restart after adding new account is needed!");
            }
            if (replaced) {
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            for (String key : premShareHosts.keySet()) {
                                for (Account acc : AccountController.getInstance().getAllAccounts(key)) {
                                    AccountController.getInstance().updateAccountInfo(key, acc, true);
                                }
                            }
                        } finally {
                            enabled = true;
                        }
                    }
                }).start();
            } else {
                enabled = true;
            }
        }

        return true;
    }

    @Override
    public void onExit() {
        synchronized (LOCK) {
            enabled = false;
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getIconKey() {
        return "gui.images.premium";
    }

    @Override
    public String getHost() {
        return JDL.L("plugins.optional.jdpremium.name", "JDPremium");
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        return null;
    }

    public static String getJDPremServer() {
        return jdpremServer;
    }

    public static boolean preferLocalAccounts() {
        return preferLocalAccounts;
    }

}
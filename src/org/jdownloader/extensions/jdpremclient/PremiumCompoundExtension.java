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

package org.jdownloader.extensions.jdpremclient;

import java.util.ArrayList;
import java.util.HashMap;

import jd.HostPluginWrapper;
import jd.Main;
import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigEntry.PropertyType;
import jd.config.ConfigGroup;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.plugins.Account;
import jd.plugins.AddonPanel;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.jdpremclient.translate.T;

public class PremiumCompoundExtension extends AbstractExtension<PremiumCompoundConfig> {

    private static final Object                            LOCK                = new Object();
    private static boolean                                 replaced            = false;
    private static boolean                                 init                = false;
    private static boolean                                 enabled             = false;
    private static String                                  jdpremServer        = null;
    private static boolean                                 preferLocalAccounts = false;

    private static final HashMap<String, String>           premShareHosts      = new HashMap<String, String>();
    private ExtensionConfigPanel<PremiumCompoundExtension> configPanel;

    public PremiumCompoundExtension() {
        super(T._.jd_plugins_optional_jdpremium_name());
    }

    private void replaceHosterPlugin(String host, String with) {
        PluginForHost old = JDUtilities.getPluginForHost(host);
        if (old != null) {
            logger.info("Replacing " + host + " Plugin with JDPremium: " + with);
            new PremShareHost(old.getHost(), with, old.getWrapper().getPattern().toString(), old.getWrapper().getFlags() + PluginWrapper.ALLOW_DUPLICATE);
        }
    }

    public static boolean isStaticEnabled() {
        return enabled;
    }

    @Override
    public String getIconKey() {
        return "premium";
    }

    public static String getJDPremServer() {
        return jdpremServer;
    }

    public static boolean preferLocalAccounts() {
        return preferLocalAccounts;
    }

    @Override
    protected void stop() throws StopException {
        synchronized (LOCK) {
            enabled = false;
        }
    }

    @Override
    protected void start() throws StartException {
    }

    @Override
    protected void initExtension() throws StartException {

        jdpremServer = getPluginConfig().getStringProperty("SERVER", null);
        preferLocalAccounts = getPluginConfig().getBooleanProperty("PREFERLOCALACCOUNTS", false);
        synchronized (LOCK) {
            if (Main.isInitComplete() && replaced == false) {
                logger.info("JDPremium: cannot be initiated during runtime. JDPremium must be enabled at startup!");
                throw new StartException("Restart needed!");

            }
            if (!init) {
                /* init our new plugins */
                premShareHosts.put("jdownloader.org", "PremShare");
                premShareHosts.put("multishare.cz", "MultiShare");
                premShareHosts.put("linksnappy.com", "LinkSnappycom");
                premShareHosts.put("rehost.to", "ReHostto");
                premShareHosts.put("nopremium.pl", "NoPremium");
                premShareHosts.put("premget.pl", "PremGet");
                premShareHosts.put("twojlimit.pl", "TwojLimit");
                premShareHosts.put("fast-debrid.com", "FastDebridcom");
                premShareHosts.put("alldebrid.com", "AllDebridcom");
                premShareHosts.put("premium4.me", "Premium4me");
                premShareHosts.put("streammania.com", "Streammaniacom");
                premShareHosts.put("fireload.org", "Fireloadorg");
                premShareHosts.put("real-debrid.com", "RealDebridcom");
                int replaceIndex = 0;
                for (String key : premShareHosts.keySet()) {
                    /* init replacePlugin */
                    try {
                        /*
                         * we do not need a seperate multishare.cz plugin, as we
                         * already have a normal plugin for it
                         */
                        if (key.equalsIgnoreCase("multishare.cz")) continue;
                        /* the premshareplugins never can be disabled */
                        new PremShareHost(key, premShareHosts.get(key), "NEVERUSETHISREGEX" + key + replaceIndex++ + ":\\)", 2 + PluginWrapper.ALWAYS_ENABLED);
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
                            /* we do not replace youtube */
                            if (plugin.getHost().contains("youtube")) continue;
                            /* and no DIRECTHTTP */
                            if (plugin.getHost().contains("DIRECTHTTP") || plugin.getHost().contains("http links")) continue;
                            /* and no ftp */
                            if (plugin.getHost().contains("ftp")) continue;
                            /* do not replace the premshare plugins ;) */
                            if (premShareHosts.containsKey(plugin.getHost()) && plugin.getPattern().pattern().startsWith("NEVERUSETHISREGEX" + plugin.getHost())) {
                                continue;
                            }
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
        ConfigContainer cc = new ConfigContainer(getName());
        initSettings(cc);
        configPanel = createPanelFromContainer(cc);

    }

    @Override
    public boolean isDefaultEnabled() {
        return true;
    }

    @Override
    public ExtensionConfigPanel<PremiumCompoundExtension> getConfigPanel() {
        return configPanel;
    }

    @Override
    public boolean hasConfigPanel() {
        return true;
    }

    protected void initSettings(ConfigContainer config) {
        config.setGroup(new ConfigGroup(getName(), getIconKey()));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, this.getPluginConfig(), "SERVER", "JDPremServer: (Restart required)").setPropertyType(PropertyType.NEEDS_RESTART));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), "PREFERLOCALACCOUNTS", "Prefer local Premium Accounts(restart required)?").setDefaultValue(false).setPropertyType(PropertyType.NEEDS_RESTART));

    }

    @Override
    public String getConfigID() {
        return "jdpremium";
    }

    @Override
    public String getAuthor() {
        return "Jiaz";
    }

    @Override
    public String getDescription() {
        return "Allows Compount Hoster Plugins. So called Proxy Hosters.";
    }

    @Override
    public AddonPanel getGUI() {
        return null;
    }

    @Override
    public ArrayList<MenuAction> getMenuAction() {
        return null;
    }

    class PremShareHost extends HostPluginWrapper {

        private HostPluginWrapper replacedone = null;

        public PremShareHost(String host, String className, String patternSupported, int flags) {
            super(host, "org.jdownloader.extensions.jdpremclient.", className, patternSupported, flags, "$Revision$");
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
        public long getVersion() {
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
}
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

import jd.HostPluginWrapper;
import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
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
        super(host, "jd.plugins.optional.jdpremclient.", "PremShare", patternSupported, flags, "$Revision$");
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
            ((PremShare) tmp).setReplacedPlugin(replacedone.getPlugin());
        }
        return tmp;
    }

    @Override
    public PluginForHost getNewPluginInstance() {
        PluginForHost tmp = super.getNewPluginInstance();
        if (replacedone != null) {
            ((PremShare) tmp).setReplacedPlugin(replacedone.getNewPluginInstance());
        }
        return tmp;
    }
}

@OptionalPlugin(rev = "$Revision$", id = "jdpremium", interfaceversion = 5)
public class JDPremium extends PluginOptional {

    private static PremShareHost jdpremium = null;
    private static final Object LOCK = new Object();
    private static boolean replaced = false;
    private static boolean enabled = false;

    public JDPremium(PluginWrapper wrapper) {
        super(wrapper);
        config.setGroup(new ConfigGroup(getHost(), getIconKey()));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, this.getPluginConfig(), "SERVER", "Server"));
    }

    private void replaceHosterPlugin(String host) {
        PluginForHost old = JDUtilities.getPluginForHost(host);
        if (old != null) {
            logger.info("Replacing " + host + " Plugin with JDPremium Plugin");
            new PremShareHost(old.getHost(), "PremShare", old.getWrapper().getPattern().toString(), (old.getWrapper()).getFlags() + PluginWrapper.ALLOW_DUPLICATE);
        }
    }

    @Override
    public boolean initAddon() {
        synchronized (LOCK) {
            if (!replaced) {
                ArrayList<HostPluginWrapper> all = JDUtilities.getPremiumPluginsForHost();
                for (HostPluginWrapper plugin : all) {
                    replaceHosterPlugin(plugin.getHost());
                }
                jdpremium = new PremShareHost("jdownloader.org", "PremShare", "NEVERUSETHISREGEX:\\)", 2);
            }
            logger.info("JDPremium init ok!");
            replaced = true;
            enabled = true;
        }
        return true;
    }

    public static Account getAccount() {
        synchronized (jdpremium) {
            return AccountController.getInstance().getValidAccount(JDPremium.getJDPremium());
        }
    }

    public static PluginForHost getJDPremium() {
        return jdpremium.getPlugin();
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

}
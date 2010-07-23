package jd.plugins.optional.jdpremclient;

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

import java.util.ArrayList;

import jd.HostPluginWrapper;
import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.controlling.AccountController;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.plugins.Account;
import jd.plugins.OptionalPlugin;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.PluginOptional;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

class PremShareHost extends HostPluginWrapper {

    private HostPluginWrapper replacedone = null;

    public PremShareHost(String host, String className, String patternSupported, int flags) {
        super(host, "jd.plugins.optional.jdpremclient.", "PremShare", patternSupported, flags, "$Revision: 6506 $");
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
        Plugin tmp = super.getPlugin();
        if (replacedone != null) {
            PluginForHost tmp2 = (PluginForHost) replacedone.getPlugin();
            ((PremShare) tmp).setReplacedPlugin(tmp2);
        }
        return (PluginForHost) tmp;
    }

    @Override
    public PluginForHost getNewPluginInstance() {
        PluginForHost tmp = super.getNewPluginInstance();
        if (replacedone != null) {
            PluginForHost tmp2 = (PluginForHost) replacedone.getNewPluginInstance();
            ((PremShare) tmp).setReplacedPlugin(tmp2);
        }
        return tmp;
    }
}

@OptionalPlugin(rev = "$Revision: 6506 $", id = "jdpremium", interfaceversion = 5)
public class JDPremium extends PluginOptional {

    private static PremShareHost jdpremium = null;

    public JDPremium(PluginWrapper wrapper) {
        super(wrapper);
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, this.getPluginConfig(), "SERVER", "Server"));

    }

    private void replaceHosterPlugin(String host) {
        PluginForHost old = JDUtilities.getPluginForHost(host);
        if (old != null) {
            logger.info("Replacing " + host + " Plugin with JDPremium Plugin");
            new PremShareHost(old.getHost(), "PremShare", old.getWrapper().getPattern().toString(), ((HostPluginWrapper) old.getWrapper()).getFlags() + PluginWrapper.ALLOW_DUPLICATE);
        }
    }

    // @Override
    public String getRequirements() {
        return "JRE 1.5+";
    }

    // @Override
    public boolean initAddon() {
        replaceHosterPlugin("rapidshare.com");
        replaceHosterPlugin("uploaded.to");
        replaceHosterPlugin("megaUpload.com");
        replaceHosterPlugin("netload.in");
        replaceHosterPlugin("hotfile.com");
        jdpremium = new PremShareHost("jdownloader.org", "PremShare", "NEVERUSETHISREGEX:\\)", 2);
        logger.info("JDPremium init ok!");
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

    public void onExit() {
        logger.info("Cannot replace Plugins on runtime. Restart is neccessary!");
    }

    public String getIconKey() {
        return "gui.images.taskpanes.premium";
    }

    public String getHost() {
        return JDL.L("plugins.optional.jdpremium.name", "JDPremium");
    }

    public String getVersion() {
        return getVersion("$Revision: 5887 $");
    }

    public static int getAddonInterfaceVersion() {
        return 3;
    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        return null;
    }

}
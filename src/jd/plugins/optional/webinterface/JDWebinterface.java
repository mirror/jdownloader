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
// TODO:    
//          -packages/links moven
//          -stable template system

package jd.plugins.optional.webinterface;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.config.SubConfiguration;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@OptionalPlugin(rev = "$Revision$", id = "webinterface", interfaceversion = 5)
public class JDWebinterface extends PluginOptional {
    static public JDWebinterface instance;
    static final String PROPERTY_HTTPS = "PARAM_HTTPS";
    static final String PROPERTY_LOGIN = "PARAM_LOGIN";
    static final String PROPERTY_PASS = "PARAM_PASS";
    static final String PROPERTY_PORT = "PARAM_PORT";
    static final String PROPERTY_REFRESH = "PARAM_REFRESH";
    static final String PROPERTY_REFRESH_INTERVAL = "PARAM_REFRESH_INTERVAL";

    static final String PROPERTY_USER = "PARAM_USER";

    public JDWebinterface(PluginWrapper wrapper) {
        super(wrapper);
        instance = this;
        initConfig();
    }

    private void initConfig() {
        SubConfiguration subConfig = SubConfiguration.getConfig("WEBINTERFACE");
        config.setGroup(new ConfigGroup(getHost(), getIconKey()));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PROPERTY_REFRESH, JDL.L("plugins.optional.webinterface.refresh", "AutoRefresh")).setDefaultValue(true));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, subConfig, PROPERTY_REFRESH_INTERVAL, JDL.L("plugins.optional.webinterface.refresh_interval", "Refresh Interval"), 5, 60, 1).setDefaultValue(5));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, subConfig, PROPERTY_PORT, JDL.L("plugins.optional.webinterface.port", "Port"), 1, 65000, 1).setDefaultValue(8765));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PROPERTY_LOGIN, JDL.L("plugins.optional.webinterface.needlogin", "Need User Authentication")).setDefaultValue(true));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PROPERTY_HTTPS, JDL.L("plugins.optional.webinterface.https", "Use HTTPS")).setDefaultValue(false));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, subConfig, PROPERTY_USER, JDL.L("plugins.optional.webinterface.loginname", "Login Name")).setDefaultValue("JD"));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD, subConfig, PROPERTY_PASS, JDL.L("plugins.optional.webinterface.loginpass", "Login Pass")).setDefaultValue("JD"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {

    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        return null;
    }

    static public int getRefreshRate() {
        SubConfiguration subConfig = SubConfiguration.getConfig("WEBINTERFACE");
        if (subConfig.getBooleanProperty(JDWebinterface.PROPERTY_REFRESH, true)) {
            return subConfig.getIntegerProperty(JDWebinterface.PROPERTY_REFRESH_INTERVAL, 5);
        } else {
            return 0;
        }
    }

    @Override
    public boolean initAddon() {
        new JDSimpleWebserver();
        logger.info("WebInterface ok: java " + JDUtilities.getJavaVersion());
        return true;
    }

    @Override
    public void onExit() {
    }
}
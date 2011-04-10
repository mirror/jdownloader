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

package org.jdownloader.extensions.webinterface;


 import org.jdownloader.extensions.webinterface.translate.*;
import java.util.ArrayList;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.config.SubConfiguration;
import jd.controlling.JSonWrapper;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.plugins.AddonPanel;
import jd.utils.locale.JDL;

import org.appwork.utils.Application;
import org.jdownloader.extensions.AbstractConfigPanel;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;

public class WebinterfaceClassicExtension extends AbstractExtension {
    static public WebinterfaceClassicExtension instance;
    static final String                        PROPERTY_HTTPS            = "PARAM_HTTPS";
    static final String                        PROPERTY_LOGIN            = "PARAM_LOGIN";
    static final String                        PROPERTY_PASS             = "PARAM_PASS";
    static final String                        PROPERTY_PORT             = "PARAM_PORT";
    static final String                        PROPERTY_REFRESH          = "PARAM_REFRESH";
    static final String                        PROPERTY_REFRESH_INTERVAL = "PARAM_REFRESH_INTERVAL";
    static final String                        PROPERTY_LOCALHOST_ONLY   = "PROPERTY_LOCALHOST_ONLY";

    static final String                        PROPERTY_USER             = "PARAM_USER";

    public AbstractConfigPanel getConfigPanel() {
        return null;
    }

    public boolean hasConfigPanel() {
        return false;
    }

    public WebinterfaceClassicExtension() throws StartException {
        super(T._.jd_plugins_optional_webinterface_jdwebinterface());
        instance = this;

    }

    static public int getRefreshRate() {
        SubConfiguration subConfig = SubConfiguration.getConfig("WEBINTERFACE");
        if (subConfig.getBooleanProperty(WebinterfaceClassicExtension.PROPERTY_REFRESH, true)) {
            return subConfig.getIntegerProperty(WebinterfaceClassicExtension.PROPERTY_REFRESH_INTERVAL, 5);
        } else {
            return 0;
        }
    }

    @Override
    protected void stop() throws StopException {
    }

    @Override
    protected void start() throws StartException {
        new JDSimpleWebserver();
        logger.info("WebInterface ok: java " + Application.getJavaVersion());

    }

    @Override
    protected void initSettings(ConfigContainer config) {
        JSonWrapper subConfig = JSonWrapper.get("WEBINTERFACE");
        config.setGroup(new ConfigGroup(getName(), getIconKey()));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PROPERTY_REFRESH, T._.plugins_optional_webinterface_refresh()).setDefaultValue(true));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, subConfig, PROPERTY_REFRESH_INTERVAL, T._.plugins_optional_webinterface_refresh_interval(), 5, 60, 1).setDefaultValue(5));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, subConfig, PROPERTY_PORT, T._.plugins_optional_webinterface_port(), 1, 65000, 1).setDefaultValue(8765));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PROPERTY_LOCALHOST_ONLY, T._.plugins_optional_webinterface_localhostonly()).setDefaultValue(false));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PROPERTY_LOGIN, T._.plugins_optional_webinterface_needlogin()).setDefaultValue(true));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PROPERTY_HTTPS, T._.plugins_optional_webinterface_https()).setDefaultValue(false));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, subConfig, PROPERTY_USER, T._.plugins_optional_webinterface_loginname()).setDefaultValue("JD"));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD, subConfig, PROPERTY_PASS, T._.plugins_optional_webinterface_loginpass()).setDefaultValue("JD"));

    }

    @Override
    public String getConfigID() {
        return "webinterface";
    }

    @Override
    public String getAuthor() {
        return "Jiaz";
    }

    @Override
    public String getDescription() {
        return T._.jd_plugins_optional_webinterface_jdwebinterface_description();
    }

    @Override
    public AddonPanel getGUI() {
        return null;
    }

    @Override
    public ArrayList<MenuAction> getMenuAction() {
        return null;
    }

    @Override
    protected void initExtension() throws StartException {
    }
}
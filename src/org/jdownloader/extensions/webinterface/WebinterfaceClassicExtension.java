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

import java.util.ArrayList;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.config.SubConfiguration;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.gui.swing.jdgui.views.settings.panels.JSonWrapper;
import jd.plugins.AddonPanel;
import jd.utils.locale.JDL;

import org.appwork.shutdown.ShutdownVetoException;
import org.appwork.utils.Application;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.PluginOptional;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;

public class WebinterfaceClassicExtension extends PluginOptional {
    static public WebinterfaceClassicExtension instance;
    static final String                        PROPERTY_HTTPS            = "PARAM_HTTPS";
    static final String                        PROPERTY_LOGIN            = "PARAM_LOGIN";
    static final String                        PROPERTY_PASS             = "PARAM_PASS";
    static final String                        PROPERTY_PORT             = "PARAM_PORT";
    static final String                        PROPERTY_REFRESH          = "PARAM_REFRESH";
    static final String                        PROPERTY_REFRESH_INTERVAL = "PARAM_REFRESH_INTERVAL";
    static final String                        PROPERTY_LOCALHOST_ONLY   = "PROPERTY_LOCALHOST_ONLY";

    static final String                        PROPERTY_USER             = "PARAM_USER";

    public ExtensionConfigPanel getConfigPanel() {
        return null;
    }

    public boolean hasConfigPanel() {
        return false;
    }

    public WebinterfaceClassicExtension() throws StartException {
        super(JDL.L("jd.plugins.optional.webinterface.jdwebinterface", "Webinterface"));
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

    public void onShutdown() {
    }

    public boolean onShutdownRequest() throws ShutdownVetoException {
        return false;
    }

    public void onShutdownVeto(ArrayList<ShutdownVetoException> vetos) {
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
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PROPERTY_REFRESH, JDL.L("plugins.optional.webinterface.refresh", "AutoRefresh")).setDefaultValue(true));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, subConfig, PROPERTY_REFRESH_INTERVAL, JDL.L("plugins.optional.webinterface.refresh_interval", "Refresh Interval"), 5, 60, 1).setDefaultValue(5));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, subConfig, PROPERTY_PORT, JDL.L("plugins.optional.webinterface.port", "Port"), 1, 65000, 1).setDefaultValue(8765));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PROPERTY_LOCALHOST_ONLY, JDL.L("plugins.optional.webinterface.localhostonly", "Access only from this Computer")).setDefaultValue(false));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PROPERTY_LOGIN, JDL.L("plugins.optional.webinterface.needlogin", "Need User Authentication")).setDefaultValue(true));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PROPERTY_HTTPS, JDL.L("plugins.optional.webinterface.https", "Use HTTPS")).setDefaultValue(false));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, subConfig, PROPERTY_USER, JDL.L("plugins.optional.webinterface.loginname", "Login Name")).setDefaultValue("JD"));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD, subConfig, PROPERTY_PASS, JDL.L("plugins.optional.webinterface.loginpass", "Login Pass")).setDefaultValue("JD"));

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
        return JDL.L("jd.plugins.optional.webinterface.jdwebinterface.description", "");
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
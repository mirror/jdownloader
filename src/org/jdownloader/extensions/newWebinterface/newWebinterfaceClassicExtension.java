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
// -Implement new Config & Storage System
// -Fix Security Hole
package org.jdownloader.extensions.newWebinterface;

import java.util.ArrayList;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.controlling.JSonWrapper;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.plugins.AddonPanel;

import org.appwork.utils.net.httpserver.HttpServerController;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;

public class newWebinterfaceClassicExtension extends AbstractExtension<newWebinterfaceClassicConfig> {
    static public newWebinterfaceClassicExtension                 instance;
    static final String                                           PROPERTY_PORT = "PARAM_PORT";
    private ExtensionConfigPanel<newWebinterfaceClassicExtension> configPanel;

    public ExtensionConfigPanel<newWebinterfaceClassicExtension> getConfigPanel() {
        return configPanel;
    }

    public boolean hasConfigPanel() {
        return true;
    }

    public newWebinterfaceClassicExtension() throws StartException {
        super("New Webinterface");
        instance = this;

    }

    @Override
    protected void stop() throws StopException {
    }

    @Override
    protected void start() throws StartException {
        // new JDSimpleWebserver();
        final HttpServerController server = new HttpServerController();
        try {
            server.registerRequestHandler(JSonWrapper.get("WEBINTERFACE").getIntegerProperty(PROPERTY_PORT), false, new WebinterfaceHandler());
            logger.info("new Webinterface started");
        } catch (final Exception ex) {
            logger.info("new Webinterface start aborted");

        }

    }

    protected void initSettings(ConfigContainer config) {
        // Storage storage =
        // JSonStorage.getPlainStorage("org.jdownloader.extensions.newWebinterface");
        // Isn't there a new Storage System? But how to use it with the Old
        // Config System :) ?
        JSonWrapper subConfig = JSonWrapper.get("WEBINTERFACE");
        config.setGroup(new ConfigGroup(getName(), getIconKey()));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, subConfig, PROPERTY_PORT, "Port:", 1, 65000, 1).setDefaultValue(8765));

    }

    @Override
    public String getConfigID() {
        return "newWebinterface";
    }

    @Override
    public String getAuthor() {
        return "Dreamcooled";
    }

    @Override
    public String getDescription() {
        return "New Webinterface (over Remoteapi)";
    }

    @Override
    public AddonPanel<newWebinterfaceClassicExtension> getGUI() {
        return null;
    }

    @Override
    public ArrayList<MenuAction> getMenuAction() {
        return null;
    }

    @Override
    protected void initExtension() throws StartException {
        ConfigContainer cc = new ConfigContainer(getName());
        initSettings(cc);
        configPanel = createPanelFromContainer(cc);
    }
}
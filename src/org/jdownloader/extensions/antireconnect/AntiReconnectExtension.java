//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSE the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://gnu.org/licenses/>.

package org.jdownloader.extensions.antireconnect;

import java.util.ArrayList;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.config.Configuration;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.plugins.AddonPanel;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.txtresource.TranslationFactory;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;

public class AntiReconnectExtension extends AbstractExtension {
    private static final String              CONFIG_MODE         = "CONFIG_MODE";
    private static final String              CONFIG_IPS          = "CONFIG_IPS";
    private static final String              CONFIG_TIMEOUT      = "CONFIG_TIMEOUT";
    private static final String              CONFIG_EACH         = "CONFIG_EACH";
    private static final String              CONFIG_OLDDOWNLOADS = "CONFIG_OLDDOWNLOADS";
    private static final String              CONFIG_NEWDOWNLOADS = "CONFIG_NEWDOWNLOADS";
    private static final String              CONFIG_OLDRECONNECT = "CONFIG_OLDRECONNECT";
    private static final String              CONFIG_NEWRECONNECT = "CONFIG_NEWRECONNECT";
    private static final String              CONFIG_OLDSPEED     = "CONFIG_OLDSPEED";
    private static final String              CONFIG_NEWSPEED     = "CONFIG_NEWSPEED";

    private String[]                         availableModes;

    private JDAntiReconnectThread            asthread            = null;
    public static JDAntiReconnectTranslation T                   = TranslationFactory.create(JDAntiReconnectTranslation.class);

    public ExtensionConfigPanel getConfigPanel() {
        return null;
    }

    public boolean hasConfigPanel() {
        return false;
    }

    public AntiReconnectExtension() throws StartException {
        super(JDL.L("jd.plugins.optional.antireconnect.jdantireconnect", "Anti Reconnect"));
        availableModes = new String[] { T.mode_disabled(), T.mode_ping(), T.mode_arp() };

    }

    @Override
    protected void stop() throws StopException {
        if (asthread != null) {
            asthread.setRunning(false);
            asthread = null;
        }
    }

    @Override
    protected void start() throws StartException {

        asthread = new JDAntiReconnectThread(this);
        asthread.start();

    }

    @Override
    public String getAuthor() {
        return "Unknown";
    }

    @Override
    public String getDescription() {
        return T.description();
    }

    @Override
    protected void initSettings(ConfigContainer config) {
        config.setGroup(new ConfigGroup(getName(), "gui.images.preferences"));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), CONFIG_MODE, availableModes, JDL.L("gui.config.antireconnect.mode", "Mode:")).setDefaultValue(0));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, getPluginConfig(), CONFIG_IPS, JDL.L("gui.config.antireconnect.ips", "Check Ips (192.168.1.20-80)")).setDefaultValue("192.168.178.20-80"));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), CONFIG_TIMEOUT, JDL.L("gui.config.antireconnect.timeout", "Check Timeout (ms):"), 1, 60000, 100).setDefaultValue(500));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), CONFIG_EACH, JDL.L("gui.config.antireconnect.each", "Check Each (ms):"), 1, 300000, 1000).setDefaultValue(10000));

        config.setGroup(new ConfigGroup(JDL.L("gui.config.antireconnect.oldgroup", "Normally"), "gui.images.preferences"));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), CONFIG_OLDDOWNLOADS, JDL.L("gui.config.antireconnect.olddownloads", "Simultanious Downloads:"), 1, 20, 1).setDefaultValue(JDUtilities.getConfiguration().getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN)));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), CONFIG_OLDRECONNECT, JDL.L("gui.config.antireconnect.oldreconnect", "Allow Reconnect:")).setDefaultValue(JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_ALLOW_RECONNECT)));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), CONFIG_OLDSPEED, JDL.L("gui.config.antireconnect.oldspeed", "Downloadspeed in kb/s"), 0, 500000, 10).setDefaultValue(JDUtilities.getConfiguration().getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED)));
        config.setGroup(new ConfigGroup(JDL.L("gui.config.antireconnect.newgroup", "If Other Clients are Online"), "gui.images.preferences"));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), CONFIG_NEWDOWNLOADS, JDL.L("gui.config.antireconnect.newdownloads", "Simultanious Downloads:"), 1, 20, 1).setDefaultValue(3));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), CONFIG_NEWRECONNECT, JDL.L("gui.config.antireconnect.newreconnect", "Allow Reconnect:")).setDefaultValue(false));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), CONFIG_NEWSPEED, JDL.L("gui.config.antireconnect.newspeed", "Downloadspeed in kb/s"), 0, 500000, 10).setDefaultValue(JDUtilities.getConfiguration().getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED)));

    }

    @Override
    public String getConfigID() {
        return "jdantireconnect";
    }

    @Override
    public ArrayList<MenuAction> getMenuAction() {
        return null;
    }

    @Override
    public AddonPanel getGUI() {
        return null;
    }

    @Override
    protected void initExtension() throws StartException {
    }
}
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

package jd.gui.skins.simple.config.panels;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;

import jd.Main;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.gui.skins.simple.config.ConfigPanel;
import jd.gui.skins.simple.config.GUIConfigEntry;
import jd.nutils.OSDetector;
import jd.utils.JDFileReg;
import jd.utils.JDLocale;
import jd.utils.JDTheme;

public class ConfigPanelGeneral extends ConfigPanel {

    private static final long serialVersionUID = 3383448498625377495L;

    private Configuration configuration;

    public ConfigPanelGeneral(Configuration configuration) {
        super();
        this.configuration = configuration;
        initPanel();
        load();
    }

    @Override
    public void initPanel() {
        ConfigEntry conditionEntry;

        ConfigGroup logging = new ConfigGroup(JDLocale.L("gui.config.general.logging", "Logging"), JDTheme.II("gui.images.terminal", 32, 32));

        addGUIConfigEntry(new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, configuration, Configuration.PARAM_LOGGER_LEVEL, new Level[] { Level.ALL, Level.FINEST, Level.FINER, Level.FINE, Level.INFO, Level.WARNING, Level.SEVERE, Level.OFF }, JDLocale.L("gui.config.general.loggerLevel", "Level für's Logging")).setDefaultValue(Level.WARNING).setGroup(logging)));
        // addGUIConfigEntry(new GUIConfigEntry(new
        // ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration,
        // Configuration.LOGGER_FILELOG,
        // JDLocale.L("gui.config.general.filelogger",
        // "Erstelle Logdatei im ./logs/ Ordner")).setDefaultValue(false).setGroup(logging)));

        ConfigGroup update = new ConfigGroup(JDLocale.L("gui.config.general.update", "Update"), JDTheme.II("gui.splash.update", 32, 32));

        addGUIConfigEntry(new GUIConfigEntry(conditionEntry = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, SubConfiguration.getConfig("WEBUPDATE"), Configuration.PARAM_WEBUPDATE_DISABLE, JDLocale.L("gui.config.general.webupdate.disable", "Update nur manuell durchführen")).setDefaultValue(false).setGroup(update).setEnabled(!Main.isBeta())));
        addGUIConfigEntry(new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Configuration.PARAM_WEBUPDATE_AUTO_RESTART, JDLocale.L("gui.config.general.webupdate.auto", "automatisch, ohne Nachfrage ausführen")).setDefaultValue(false).setEnabledCondidtion(conditionEntry, "==", false).setGroup(update)));
        addGUIConfigEntry(new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Configuration.PARAM_WEBUPDATE_AUTO_SHOW_CHANGELOG, JDLocale.L("gui.config.general.changelog.auto", "Open Changelog after update")).setDefaultValue(true).setGroup(update)));
        ConfigGroup cnl = new ConfigGroup(JDLocale.L("gui.config.general.cnl", "Click'n'Load"), JDTheme.II("gui.clicknload", 32, 32));

        addGUIConfigEntry(new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_BUTTON, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                SubConfiguration.getConfig("CNL2").setProperty("INSTALLED", false);
                JDFileReg.registerFileExts();
            }

        }, JDLocale.L("gui.config.general.cnl.install", "Install")).setDefaultValue(false).setGroup(cnl).setEnabled(OSDetector.isWindows())));
        addGUIConfigEntry(new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_BUTTON, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JDFileReg.unregisterFileExts();
            }

        }, JDLocale.L("gui.config.general.cnl.uninstall", "uninstall")).setDefaultValue(false).setGroup(cnl).setEnabled(OSDetector.isWindows())));

        this.add(panel);
    }

    @Override
    public void load() {
        loadConfigEntries();
    }

    @Override
    public void save() {
        saveConfigEntries();
        logger.setLevel((Level) configuration.getProperty(Configuration.PARAM_LOGGER_LEVEL));
    }
}

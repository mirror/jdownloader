//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.util.logging.Level;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.utils.JDLocale;

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

        addGUIConfigEntry(new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, configuration, Configuration.PARAM_LOGGER_LEVEL, new Level[] { Level.ALL, Level.FINEST, Level.FINER, Level.FINE, Level.INFO, Level.WARNING, Level.SEVERE, Level.OFF }, JDLocale.L("gui.config.general.loggerLevel", "Level für's Logging")).setDefaultValue(Level.WARNING)));
        addGUIConfigEntry(new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Configuration.LOGGER_FILELOG, JDLocale.L("gui.config.general.filelogger", "Erstelle Logdatei im ./logs/ Ordner")).setDefaultValue(false)));
        addGUIConfigEntry(new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR)));
        addGUIConfigEntry(new GUIConfigEntry(conditionEntry = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Configuration.PARAM_WEBUPDATE_DISABLE, JDLocale.L("gui.config.general.webupdate.disable", "Update nur manuell durchführen")).setDefaultValue(false)));
        addGUIConfigEntry(new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Configuration.PARAM_WEBUPDATE_AUTO_RESTART, JDLocale.L("gui.config.general.webupdate.auto", "automatisch, ohne Nachfrage ausführen")).setDefaultValue(false).setEnabledCondidtion(conditionEntry, "==", false)));

        this.add(panel, BorderLayout.NORTH);
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

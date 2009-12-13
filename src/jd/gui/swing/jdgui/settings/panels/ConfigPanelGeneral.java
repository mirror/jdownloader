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

package jd.gui.swing.jdgui.settings.panels;

import java.util.logging.Level;

import javax.swing.JTabbedPane;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.gui.swing.jdgui.settings.ConfigPanel;
import jd.gui.swing.jdgui.settings.GUIConfigEntry;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class ConfigPanelGeneral extends ConfigPanel {
    private static final String JDL_PREFIX = "jd.gui.swing.jdgui.settings.panels.ConfigPanelGeneral.";

    public String getBreadcrum() {
        return JDL.L(this.getClass().getName() + ".breadcrum", this.getClass().getSimpleName());
    }

    public static String getTitle() {
        return JDL.L(JDL_PREFIX + "general.title", "General");
    }

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

        ConfigGroup logging = new ConfigGroup(JDL.L("gui.config.general.logging", "Logging"), JDTheme.II("gui.images.terminal", 32, 32));
        addGUIConfigEntry(new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, configuration, Configuration.PARAM_LOGGER_LEVEL, new Level[] { Level.ALL, Level.INFO, Level.OFF }, JDL.L("gui.config.general.loggerLevel", "Level für's Logging")).setDefaultValue(Level.INFO).setGroup(logging)));

        ConfigGroup update = new ConfigGroup(JDL.L("gui.config.general.update", "Update"), JDTheme.II("gui.splash.update", 32, 32));
        addGUIConfigEntry(new GUIConfigEntry(conditionEntry = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, SubConfiguration.getConfig("WEBUPDATE"), Configuration.PARAM_WEBUPDATE_DISABLE, JDL.L("gui.config.general.webupdate.disable2", "Do not inform me about important updates")).setDefaultValue(false).setGroup(update)));
        addGUIConfigEntry(new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Configuration.PARAM_WEBUPDATE_AUTO_RESTART, JDL.L("gui.config.general.webupdate.auto", "automatisch, ohne Nachfrage ausführen")).setDefaultValue(false).setEnabledCondidtion(conditionEntry, false).setGroup(update)));
        addGUIConfigEntry(new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Configuration.PARAM_WEBUPDATE_AUTO_SHOW_CHANGELOG, JDL.L("gui.config.general.changelog.auto", "Open Changelog after update")).setDefaultValue(true).setGroup(update)));

        JTabbedPane tabbed = new JTabbedPane();
        tabbed.setOpaque(false);
        tabbed.add(getBreadcrum(), panel);
        this.add(tabbed);
    }

    @Override
    protected void saveSpecial() {
        logger.setLevel((Level) configuration.getProperty(Configuration.PARAM_LOGGER_LEVEL));
    }
}

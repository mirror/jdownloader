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

package jd.gui.swing.jdgui.views.settings.panels;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.views.settings.ConfigPanel;
import jd.update.WebUpdater;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class ConfigPanelGeneral extends ConfigPanel {

    private static final String JDL_PREFIX = "jd.gui.swing.jdgui.settings.panels.ConfigPanelGeneral.";

    public static String getTitle() {
        return JDL.L(JDL_PREFIX + "general.title", "General");
    }

    public static String getIconKey() {
        return "gui.images.config.home";
    }

    private static final long serialVersionUID = 3383448498625377495L;

    private Configuration configuration;

    public ConfigPanelGeneral() {
        super();

        this.configuration = JDUtilities.getConfiguration();

        init();
    }

    @Override
    protected ConfigContainer setupContainer() {
        ConfigEntry ce, conditionEntry;

        ConfigContainer look = new ConfigContainer();

        look.setGroup(new ConfigGroup(JDL.L("gui.config.general.logging", "Logging"), "gui.images.terminal"));
        look.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, configuration, Configuration.PARAM_LOGGER_LEVEL, new Level[] { Level.ALL, Level.INFO, Level.OFF }, JDL.L("gui.config.general.loggerLevel", "Level für's Logging")).setDefaultValue(Level.INFO));

        look.setGroup(new ConfigGroup(JDL.L("gui.config.general.update", "Update"), "gui.splash.update"));
        look.addEntry(conditionEntry = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, SubConfiguration.getConfig("WEBUPDATE"), Configuration.PARAM_WEBUPDATE_DISABLE, JDL.L("gui.config.general.webupdate.disable2", "Do not inform me about important updates")).setDefaultValue(false));
        look.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Configuration.PARAM_WEBUPDATE_AUTO_RESTART, JDL.L("gui.config.general.webupdate.auto", "Webupdate: start automatically!")).setDefaultValue(false).setEnabledCondidtion(conditionEntry, false));
        look.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Configuration.PARAM_WEBUPDATE_AUTO_SHOW_CHANGELOG, JDL.L("gui.config.general.changelog.auto", "Open Changelog after update")).setDefaultValue(true));

        look.addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));

        /* Branch Resetting */
        final SubConfiguration config = SubConfiguration.getConfig("WEBUPDATE");
        String branch = config.getStringProperty(WebUpdater.BRANCHINUSE, null);
        look.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_BUTTON, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                config.setProperty(WebUpdater.BRANCHINUSE, null);
                config.save();
                UserIO.getInstance().requestMessageDialog(JDL.L("gui.config.general.resetbranch.message", "The selected branch was resetted. The Updater may find new updates after the next restart."));
            }

        }, JDL.L("gui.config.general.resetbranch.short", "Reset"), JDL.LF("gui.config.general.resetbranch", "Reset selected Branch (Current Branch: %s)", branch == null ? "-" : branch), JDTheme.II("gui.images.restart", 16, 16)));
        ce.setEnabled(branch != null);

        return look;
    }

    @Override
    protected void saveSpecial() {
        logger.setLevel((Level) configuration.getProperty(Configuration.PARAM_LOGGER_LEVEL));
    }
}

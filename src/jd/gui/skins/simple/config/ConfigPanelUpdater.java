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

import jd.JDInit;
import jd.config.CFGConfig;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.gui.UIInterface;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * @author JD-Team
 * 
 */
public class ConfigPanelUpdater extends ConfigPanel {

    private static final long serialVersionUID = 4145243293360008779L;

    private SubConfiguration config;

    private Configuration configuration;

    private Boolean beta;

    public ConfigPanelUpdater(Configuration configuration, UIInterface uiinterface) {
        super(uiinterface);
        this.configuration = configuration;
        initPanel();
        load();
    }

    @Override
    public String getName() {
        return JDLocale.L("gui.config.webupdate.name", "Webupdate");
    }

    @Override
    public void initPanel() {
        config = CFGConfig.getConfig("WEBUPDATE");
        this.beta = CFGConfig.getConfig("WEBUPDATE").getBooleanProperty("WEBUPDATE_BETA", false);

        ConfigEntry conditionEntry;
        addGUIConfigEntry(new GUIConfigEntry(conditionEntry = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Configuration.PARAM_WEBUPDATE_DISABLE, JDLocale.L("gui.config.general.webupdate.disable", "Update nur manuell durchführen")).setDefaultValue(false)));
        addGUIConfigEntry(new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Configuration.PARAM_WEBUPDATE_AUTO_RESTART, JDLocale.L("gui.config.general.webupdate.auto", "automatisch, ohne Nachfrage ausführen")).setDefaultValue(false).setEnabledCondidtion(conditionEntry, "==", false)));
        addGUIConfigEntry(new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config, "WEBUPDATE_BETA", JDLocale.L("gui.config.general.webupdate.beta", "Auf Betaversion aktualisieren(Neustart nötig)")).setDefaultValue(false)));
        add(panel, BorderLayout.NORTH);
    }

    @Override
    public void load() {
        loadConfigEntries();
    }

    @Override
    public void save() {
        logger.info("save");
        saveConfigEntries();
        config.save();
        if (beta != CFGConfig.getConfig("WEBUPDATE").getBooleanProperty("WEBUPDATE_BETA", false)) {
            new JDInit().doWebupdate(JDUtilities.getConfiguration().getIntegerProperty(Configuration.CID, -1), true);
        }
    }

}

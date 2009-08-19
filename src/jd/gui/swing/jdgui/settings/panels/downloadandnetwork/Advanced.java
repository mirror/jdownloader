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

package jd.gui.swing.jdgui.settings.panels.downloadandnetwork;

import javax.swing.JTabbedPane;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.ByteBufferController;
import jd.gui.swing.jdgui.settings.ConfigPanel;
import jd.gui.swing.jdgui.settings.GUIConfigEntry;
import jd.http.IPCheck;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class Advanced extends ConfigPanel {

    private static final long serialVersionUID = -8421603124342250902L;
    private static final String JDL_PREFIX = "jd.gui.swing.jdgui.settings.panels.downloadandnetwork.Advanced.";

    public String getBreadcrum() {
        return JDL.L(this.getClass().getName() + ".breadcrum", this.getClass().getSimpleName());
    }

    public static String getTitle() {
        return JDL.L(JDL_PREFIX + "download.advanced.title", "Advanced");
    }

    private SubConfiguration config;

    public Advanced(Configuration configuration) {
        super();

        config = SubConfiguration.getConfig("DOWNLOAD");
        initPanel();
        load();
    }

    private ConfigContainer setupContainer() {

        ConfigEntry ce;
        // ConfigEntry conditionEntry;
        ConfigEntry conditionEntry2;

        // Extended Tab

        ConfigContainer extended = new ConfigContainer(JDL.L("gui.config.download.network.extended", "Erweiterte Einstellungen"));

        extended.setGroup(new ConfigGroup(JDL.L("gui.config.download.ipcheck", "Reconnect IP-Check"), JDTheme.II("gui.images.network", 32, 32)));

        // extended.addEntry(conditionEntry = new
        // ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config,
        // Configuration.PARAM_GLOBAL_IP_DISABLE,
        // JDL.L("gui.config.download.ipcheck.disable",
        // "IP Überprüfung deaktivieren")));
        // conditionEntry.setDefaultValue(false);

        extended.addEntry(conditionEntry2 = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config, Configuration.PARAM_GLOBAL_IP_BALANCE, JDL.L("gui.config.download.ipcheck.balance", "Use balanced IP-Check")));
        conditionEntry2.setDefaultValue(true);

        extended.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, config, Configuration.PARAM_GLOBAL_IP_CHECK_SITE, JDL.L("gui.config.download.ipcheck.website", "IP prüfen über (Website)")));
        ce.setDefaultValue(IPCheck.IP_CHECK_SERVICES.get(0)[0]);
        ce.setEnabledCondidtion(conditionEntry2, "==", false);

        extended.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, config, Configuration.PARAM_GLOBAL_IP_PATTERN, JDL.L("gui.config.download.ipcheck.regex", "RegEx zum filtern der IP")));
        ce.setDefaultValue(IPCheck.IP_CHECK_SERVICES.get(0)[1]);
        ce.setEnabledCondidtion(conditionEntry2, "==", false);

        extended.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, config, Configuration.PARAM_GLOBAL_IP_MASK, JDL.L("gui.config.download.ipcheck.mask", "Erlaubte IPs")));
        ce.setDefaultValue("\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?).)" + "{3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b");
        ce.setEnabledCondidtion(conditionEntry2, "==", false);

        extended.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, config, "EXTERNAL_IP_CHECK_INTERVAL", JDL.L("gui.config.download.ipcheck.externalinterval", "External IP Check Interval [sec]"), 10, 60 * 60));
        ce.setDefaultValue(10 * 60);
        ce.setEnabledCondidtion(conditionEntry2, "==", false);

        extended.setGroup(new ConfigGroup(JDL.L("gui.config.download.write", "File writing"), JDTheme.II("gui.images.save", 32, 32)));

        extended.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config, Configuration.PARAM_DO_CRC, JDL.L("gui.config.download.crc", "SFV/CRC Check wenn möglich durchführen")));

        ce.setDefaultValue(true);

        extended.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, config, ByteBufferController.MAXBUFFERSIZE, JDL.L("gui.config.download.buffersize2", "Max. Buffersize[KB]"), 500, 2000));
        ce.setStep(100);
        ce.setDefaultValue(100);
        return extended;
    }

    @Override
    public void initPanel() {
        ConfigContainer container = setupContainer();

        for (ConfigEntry cfgEntry : container.getEntries()) {
            GUIConfigEntry ce = new GUIConfigEntry(cfgEntry);
            if (ce != null) addGUIConfigEntry(ce);
        }
        JTabbedPane tabbed = new JTabbedPane();
        tabbed.addTab(getBreadcrum(), panel);
        add(tabbed);
    }

    @Override
    public void load() {
        loadConfigEntries();
    }

    @Override
    public void save() {
        saveConfigEntries();

    }
}

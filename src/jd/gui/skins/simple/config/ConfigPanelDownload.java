//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://wnu.org/licenses/>.

package jd.gui.skins.simple.config;

import java.awt.BorderLayout;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.gui.UIInterface;
import jd.gui.skins.simple.SimpleGUI;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * @author JD-Team
 * 
 */
public class ConfigPanelDownload extends ConfigPanel {

    /**
     * 
     */
    private static final long serialVersionUID = 4145243293360008779L;

    private SubConfiguration  config;

    public ConfigPanelDownload(Configuration configuration, UIInterface uiinterface) {
        super(uiinterface);

        initPanel();

        load();

    }

    public void save() {
        logger.info("save");
        this.saveConfigEntries();
        config.save();
    }

    public void initPanel() {
        config = JDUtilities.getSubConfig("DOWNLOAD");
        ConfigEntry ce;
        ce = new ConfigEntry(ConfigContainer.TYPE_BROWSEFOLDER, JDUtilities.getConfiguration(), Configuration.PARAM_DOWNLOAD_DIRECTORY, JDLocale.L("gui.config.general.downloadDirectory", "Downloadverzeichnis")).setDefaultValue(JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath());
        addGUIConfigEntry(new GUIConfigEntry(ce));
        ce =new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, JDUtilities.getConfiguration(), Configuration.PARAM_FINISHED_DOWNLOADS_ACTION, new String[]{Configuration.FINISHED_DOWNLOADS_REMOVE, Configuration.FINISHED_DOWNLOADS_REMOVE_AT_START, Configuration.FINISHED_DOWNLOADS_NO_REMOVE}, JDLocale.L("gui.config.general.toDoWithDownloads", "Fertig gestellte Downloads ...")).setDefaultValue(Configuration.FINISHED_DOWNLOADS_REMOVE_AT_START).setExpertEntry(true);
        addGUIConfigEntry(new GUIConfigEntry(ce));
 
        ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, config, Configuration.PARAM_DOWNLOAD_READ_TIMEOUT, JDLocale.L("gui.config.download.timeout.read", "Timeout beim Lesen [ms]"), 0, 60000);

        ce.setDefaultValue(10000);

        ce.setStep(500);
        ce.setExpertEntry(true);
        addGUIConfigEntry(new GUIConfigEntry(ce));
        ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, config, Configuration.PARAM_DOWNLOAD_CONNECT_TIMEOUT, JDLocale.L("gui.config.download.timeout.connect", "Timeout beim Verbinden(Request) [ms]"), 0, 60000);
        ce.setDefaultValue(10000);
        ce.setStep(500);
        ce.setExpertEntry(true);

        addGUIConfigEntry(new GUIConfigEntry(ce));
        ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, config, Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, JDLocale.L("gui.config.download.simultan_downloads", "Maximale gleichzeitige Downloads"), 1, 20);
        ce.setDefaultValue(3);
        ce.setStep(1);
        addGUIConfigEntry(new GUIConfigEntry(ce));

        ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, config, Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, JDLocale.L("gui.config.download.chunks", "Anzahl der Verbindungen/Datei(Chunkload)"), 1, 20);
        ce.setDefaultValue(3);
        ce.setStep(1);
        addGUIConfigEntry(new GUIConfigEntry(ce));

        ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config, "PARAM_DOWNLOAD_AUTO_CORRECTCHUNKS", JDLocale.L("gui.config.download.autochunks", "Chunks an Dateigröße anpassen."));
        ce.setDefaultValue(true);
        ce.setExpertEntry(true);
        addGUIConfigEntry(new GUIConfigEntry(ce));
        // ce= new GUIConfigEntry( new ConfigEntry(ConfigContainer.TYPE_SPINNER,
        // configuration, Configuration.PARAM_MIN_FREE_SPACE, "Downloads stoppen
        // wenn
        // der freie Speicherplatz weniger ist als [MB]",0,10000);
//        ce.setDefaultValue(100);
//        ce.setStep(10);
        // addGUIConfigEntry(ce);

        ce = new ConfigEntry(ConfigContainer.TYPE_SEPARATOR);

        addGUIConfigEntry(new GUIConfigEntry(ce));

        ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, config, Configuration.PARAM_GLOBAL_IP_CHECK_SITE, JDLocale.L("gui.config.download.ipcheck.website", "IP prüfen über (Website)"));
        ce.setDefaultValue("http://checkip.dyndns.org");
        ce.setExpertEntry(true);
        addGUIConfigEntry(new GUIConfigEntry(ce));
        ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, config, Configuration.PARAM_GLOBAL_IP_PATTERN, JDLocale.L("gui.config.download.ipcheck.regex", "RegEx zum filtern der IP"));
        ce.setDefaultValue("Address\\: ([0-9.]*)\\<\\/body\\>");
        ce.setExpertEntry(true);
        addGUIConfigEntry(new GUIConfigEntry(ce));
        ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config, Configuration.PARAM_GLOBAL_IP_DISABLE, JDLocale.L("gui.config.download.ipcheck.disable", "IP Überprüfung deaktivieren"));
        ce.setDefaultValue(false);
        ce.setExpertEntry(true);
        addGUIConfigEntry(new GUIConfigEntry(ce));
        ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME), SimpleGUI.PARAM_START_DOWNLOADS_AFTER_START, JDLocale.L("gui.config.download.startDownloadsOnStartUp", "Download beim Programmstart beginnen"));
        ce.setDefaultValue(false);
        addGUIConfigEntry(new GUIConfigEntry(ce));
        ce = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, config, Configuration.PARAM_FILE_EXISTS, new String[] { JDLocale.L("system.download.triggerfileexists.overwrite", "Datei überschreiben"), JDLocale.L("system.download.triggerfileexists.skip", "Link überspringen") }, JDLocale.L("system.download.triggerfileexists", "Wenn eine Datei schon vorhanden ist:"));
        ce.setDefaultValue(JDLocale.L("system.download.triggerfileexists.skip", "Link überspringen"));
        ce.setExpertEntry(false);
        addGUIConfigEntry(new GUIConfigEntry(ce));

        add(panel, BorderLayout.NORTH);
    }

    @Override
    public void load() {
        this.loadConfigEntries();

    }

    @Override
    public String getName() {

        return JDLocale.L("gui.config.download.name", "Netzwerk/Download");
    }

}

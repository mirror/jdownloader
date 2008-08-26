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

package jd;

import java.awt.BorderLayout;
import java.util.Locale;

import javax.swing.JPanel;

import jd.config.CFGConfig;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.config.ConfigEntriesPanel;
import jd.gui.skins.simple.config.ConfigurationPopup;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Der Installer erscheint nur beim ersten mal Starten der Webstartversion und
 * beim neuinstallieren der webstartversion der User kann Basiceinstellungen
 * festlegen
 * 
 * @author JD-Team
 */
public class Installer {

    private static final long serialVersionUID = 8764525546298642601L;

    // private Logger logger = JDUtilities.getLogger();

    private boolean aborted = false;

    public Installer() {

        super();
        ConfigEntry ce;
        ConfigContainer configContainer;

        configContainer = new ConfigContainer(this, "Language");
        ce = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME), SimpleGUI.PARAM_LOCALE, JDLocale.getLocaleIDs().toArray(new String[] {}), JDLocale.L("gui.config.gui.language", "Sprache")).setDefaultValue(Locale.getDefault());
        configContainer.addEntry(ce);
        showPanel(configContainer);
        String lang = JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getStringProperty(SimpleGUI.PARAM_LOCALE);
        if (lang == null) {
            JDUtilities.getLogger().severe("language not set");
            this.aborted = true;
            return;
        }
        JDLocale.setLocale(JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getStringProperty(SimpleGUI.PARAM_LOCALE, "english"));

        configContainer = new ConfigContainer(this, "Download");

        ce = new ConfigEntry(ConfigContainer.TYPE_BROWSEFOLDER, JDUtilities.getConfiguration(), Configuration.PARAM_DOWNLOAD_DIRECTORY, JDLocale.L("gui.config.general.downloadDirectory", "Downloadverzeichnis")).setDefaultValue(JDUtilities.getResourceFile("downloads").getAbsolutePath());
        configContainer.addEntry(ce);

        ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, CFGConfig.getConfig("WEBUPDATE"), "WEBUPDATE_BETA", JDLocale.L("gui.config.general.webupdate.betainstaller", "Use JDownloader BETA")).setDefaultValue(false);
        configContainer.addEntry(ce);

        showPanel(configContainer);
        if (JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY) == null) {
            this.aborted = true;

            JDUtilities.getLogger().severe("downloaddir not set");
            return;
        }
        JDUtilities.saveConfig();
    }

    private void showPanel(ConfigContainer configContainer) {
        ConfigEntriesPanel cpanel = new ConfigEntriesPanel(configContainer);

        JPanel panel = new JPanel(new BorderLayout());
        JPanel topPanel = new JPanel();
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(cpanel, BorderLayout.CENTER);
        ConfigurationPopup pop = new ConfigurationPopup(null, cpanel, panel);
        pop.setModal(true);
        pop.setAlwaysOnTop(true);
        pop.setLocation(JDUtilities.getCenterOfComponent(null, pop));
        pop.setVisible(true);

    }

    public boolean isAborted() {
        return aborted;
    }

}

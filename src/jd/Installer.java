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

package jd;

import java.io.File;
import java.util.Locale;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.gui.skins.simple.SimpleGUI;
import jd.nutils.OSDetector;
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

    private boolean aborted = false;

    public Installer() {
        ConfigContainer configContainer;
        ConfigEntry ce;

        configContainer = new ConfigContainer(this, "Language");
        configContainer.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, JDUtilities.getSubConfig(JDLocale.CONFIG), JDLocale.LOCALE_ID, JDLocale.getLocaleIDs().toArray(new String[] {}), JDLocale.L("gui.config.gui.language", "Sprache")).setDefaultValue(Locale.getDefault()));
        SimpleGUI.showConfigDialog(null, configContainer, true);
        if (JDUtilities.getSubConfig(JDLocale.CONFIG).getStringProperty(JDLocale.LOCALE_ID) == null) {
            JDUtilities.getLogger().severe("language not set");
            this.aborted = true;
            return;
        }
        JDLocale.setLocale(JDUtilities.getSubConfig(JDLocale.CONFIG).getStringProperty(JDLocale.LOCALE_ID, "english"));

        configContainer = new ConfigContainer(this, "Download");
        configContainer.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_BROWSEFOLDER, JDUtilities.getConfiguration(), Configuration.PARAM_DOWNLOAD_DIRECTORY, JDLocale.L("gui.config.general.downloadDirectory", "Downloadverzeichnis")));
        if (!OSDetector.isMac()) {
            ce.setDefaultValue(JDUtilities.getResourceFile("downloads").getAbsolutePath());
        } else {
            ce.setDefaultValue(new File(System.getProperty("user.home") + "/Downloads"));
        }
        SimpleGUI.showConfigDialog(null, configContainer, true);
        if (JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY) == null) {
            JDUtilities.getLogger().severe("downloaddir not set");
            this.aborted = true;
            return;
        }

        JDUtilities.getConfiguration().save();
    }

    public boolean isAborted() {
        return aborted;
    }

}

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

import java.awt.BorderLayout;
import java.io.File;
import java.util.Locale;

import javax.swing.JFrame;
import javax.swing.JPanel;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.gui.skins.simple.GuiRunnable;
import jd.gui.skins.simple.config.ConfigEntriesPanel;
import jd.gui.skins.simple.config.ConfigurationPopup;
import jd.nutils.OSDetector;
import jd.nutils.Screen;
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
        configContainer.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, SubConfiguration.getConfig(JDLocale.CONFIG), JDLocale.LOCALE_ID, JDLocale.getLocaleIDs().toArray(new String[] {}), JDLocale.L("gui.config.gui.language", "Sprache")).setDefaultValue(Locale.getDefault()));
        showConfigDialog(null, configContainer, true);
        if (SubConfiguration.getConfig(JDLocale.CONFIG).getStringProperty(JDLocale.LOCALE_ID) == null) {
            JDLogger.getLogger().severe("language not set");
            this.aborted = true;
            return;
        }
        JDLocale.setLocale(SubConfiguration.getConfig(JDLocale.CONFIG).getStringProperty(JDLocale.LOCALE_ID, "english"));

        configContainer = new ConfigContainer(this, "Download");
        configContainer.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_BROWSEFOLDER, JDUtilities.getConfiguration(), Configuration.PARAM_DOWNLOAD_DIRECTORY, JDLocale.L("gui.config.general.downloadDirectory", "Downloadverzeichnis")));
        if (OSDetector.isMac()) {
            ce.setDefaultValue(new File(System.getProperty("user.home") + "/Downloads"));

        } else if (OSDetector.isWindows() && new File(System.getProperty("user.home") + "/Downloads").exists()) {
            ce.setDefaultValue(new File(System.getProperty("user.home") + "/Downloads"));
        } else if (OSDetector.isWindows() && new File(System.getProperty("user.home") + "/Download").exists()) {
            ce.setDefaultValue(new File(System.getProperty("user.home") + "/Download"));
        } else {
            ce.setDefaultValue(JDUtilities.getResourceFile("downloads").getAbsolutePath());
        }
        showConfigDialog(null, configContainer, true);
        if (JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY) == null) {
            JDLogger.getLogger().severe("downloaddir not set");
            this.aborted = true;
            return;
        }

        JDUtilities.getConfiguration().save();
    }

    public static void showConfigDialog(final JFrame parent, final ConfigContainer configContainer, final boolean alwaysOnTop) {
        // logger.info("ConfigDialog");
        new GuiRunnable<Object>() {

            // @Override
            public Object runSave() {
                ConfigEntriesPanel p = new ConfigEntriesPanel(configContainer);
                JPanel panel = new JPanel(new BorderLayout());
                panel.add(new JPanel(), BorderLayout.NORTH);
                panel.add(p, BorderLayout.CENTER);

                ConfigurationPopup pop = new ConfigurationPopup(parent, p, panel);
                pop.setModal(true);
                pop.setAlwaysOnTop(alwaysOnTop);
                pop.setLocation(Screen.getCenterOfComponent(parent, pop));
                pop.setVisible(true);

                return null;
            }

        }.waitForEDT();
    }

    public boolean isAborted() {
        return aborted;
    }

}

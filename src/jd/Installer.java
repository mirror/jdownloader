package jd;

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

import java.awt.BorderLayout;
import java.util.Locale;

import javax.swing.JPanel;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.config.ConfigEntriesPanel;
import jd.gui.skins.simple.config.ConfigurationPopup;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Der Installer erscheint nur beim ersten mal STarten der Webstartversion und
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

        ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, JDUtilities.getSubConfig("WEBUPDATE"), "WEBUPDATE_BETA", JDLocale.L("gui.config.general.webupdate.betainstaller", "Use JDownloader BETA")).setDefaultValue(false);
        configContainer.addEntry(ce);

        showPanel(configContainer);
        if (JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY) == null) {
            this.aborted = true;

            JDUtilities.getLogger().severe("downloaddir not set");
            return;
        }
        JDUtilities.saveConfig();
        //        
        // File downloadPath = JDUtilities.getJDHomeDirectoryFromEnvironment();
        // File installPath = downloadPath;
        // setModal(true);
        // setLayout(new BorderLayout());
        //
        // setTitle(JDLocale.L("installer.title", "JDownloader Installation"));
        // setAlwaysOnTop(true);
        //
        // setLocation(20, 20);
        // panel = new JPanel(new GridBagLayout());
        //
        // homeDir = new BrowseFile();
        // homeDir.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        // homeDir.setEditable(true);
        // homeDir.setText(installPath.getAbsolutePath());
        //
        // downloadDir = new BrowseFile();
        // downloadDir.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        // downloadDir.setEditable(true);
        // downloadDir.setText(downloadPath.getAbsolutePath());
        // addWindowListener(this);
        // // JDUtilities.addToGridBag(panel, new
        // // JLabel(JDLocale.L("installer.installDir","Install Directory")),
        // // GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE,
        // // GridBagConstraints.RELATIVE, 1, 0, 0, insets,
        // // GridBagConstraints.NONE, GridBagConstraints.WEST);
        // // JDUtilities.addToGridBag(panel, homeDir,
        // GridBagConstraints.RELATIVE,
        // // GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1,
        // 0,
        // // insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        // JDUtilities.addToGridBag(panel, new
        // JLabel(JDLocale.L("installer.downloadDir", "Downloaddirectory")),
        // GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE,
        // GridBagConstraints.RELATIVE, 1, 0, 0, insets,
        // GridBagConstraints.NONE, GridBagConstraints.WEST);
        // JDUtilities.addToGridBag(panel, downloadDir,
        // GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE,
        // GridBagConstraints.REMAINDER, 1, 1, 0, insets,
        // GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        // btnOK = new JButton(JDLocale.L("gui.btn_continue", "Continue..."));
        // btnOK.addActionListener(this);
        // JDUtilities.addToGridBag(panel, btnOK, GridBagConstraints.RELATIVE,
        // GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 0, 0,
        // insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        // this.add(panel, BorderLayout.CENTER);
        // pack();
        //
        // setVisible(true);
    }

    private void showPanel(ConfigContainer configContainer) {
        ConfigEntriesPanel cpanel = new ConfigEntriesPanel(configContainer, "Select where filesdownloaded with JDownloader should be stored.");

        JPanel panel = new JPanel(new BorderLayout());
        JPanel topPanel = new JPanel();
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(cpanel, BorderLayout.CENTER);
        ConfigurationPopup pop = new ConfigurationPopup(null, cpanel, panel, SimpleGUI.CURRENTGUI, JDUtilities.getConfiguration());
        pop.setModal(true);
        pop.setAlwaysOnTop(true);
        pop.setLocation(JDUtilities.getCenterOfComponent(null, pop));
        pop.setVisible(true);

    }

    public boolean isAborted() {
        return aborted;
    }

}

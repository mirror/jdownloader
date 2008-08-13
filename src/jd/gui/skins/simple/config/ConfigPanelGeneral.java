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
import java.awt.GridBagConstraints;
import java.util.logging.Level;

import javax.swing.JFileChooser;
import javax.swing.JLabel;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.gui.UIInterface;
import jd.gui.skins.simple.components.BrowseFile;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class ConfigPanelGeneral extends ConfigPanel {
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 3383448498625377495L;
    private BrowseFile brsHomeDir;
    private Configuration configuration;
    private JLabel lblHomeDir;

    public ConfigPanelGeneral(Configuration configuration, UIInterface uiinterface) {
        super(uiinterface);
        this.configuration = configuration;
        initPanel();
        load();
    }

    @Override
    public String getName() {
        return JDLocale.L("gui.config.general.name", "Allgemein");
    }

    @Override
    public void initPanel() {
        GUIConfigEntry ce;

        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, configuration, Configuration.PARAM_LOGGER_LEVEL, new Level[] { Level.ALL, Level.FINEST, Level.FINER, Level.FINE, Level.INFO, Level.WARNING, Level.SEVERE, Level.OFF }, JDLocale.L("gui.config.general.loggerLevel", "Level für's Logging")).setDefaultValue(Level.WARNING));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Configuration.LOGGER_FILELOG, JDLocale.L("gui.config.general.filelogger", "Erstelle Logdatei im ./logs/ Ordner")).setDefaultValue(false));
        addGUIConfigEntry(ce);

        // if(JDUtilities.getJavaVersion()>=1.6d){
        // ce = new GUIConfigEntry(new
        // ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration,
        // Configuration.PARAM_NO_TRAY, "Trayicon
        // deaktivieren").setDefaultValue(false));
        //               
        // }else{
        // ce = new GUIConfigEntry(new
        // ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration,
        // Configuration.PARAM_NO_TRAY, "Trayicon
        // deaktivieren").setDefaultValue(true).setEnabled(false));
        // }
        // addGUIConfigEntry(ce);

//        if (JDUtilities.getHomeDirectory() != null) {
//            brsHomeDir = new BrowseFile();
//            brsHomeDir.setText(JDUtilities.getHomeDirectory());
//            brsHomeDir.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
//            JDUtilities.addToGridBag(panel, lblHomeDir, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
//            JDUtilities.addToGridBag(panel, brsHomeDir, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
//        }
        add(panel, BorderLayout.NORTH);
    }

    @Override
    public void load() {
        loadConfigEntries();
    }

    @Override
    public void save() {
        saveConfigEntries();

        JDUtilities.getLogger().setLevel((Level) configuration.getProperty(Configuration.PARAM_LOGGER_LEVEL));
//        if (JDUtilities.getHomeDirectory() != null && !JDUtilities.getHomeDirectory().equalsIgnoreCase(brsHomeDir.getText().trim())) {
//            JDUtilities.writeJDHomeDirectoryToWebStartCookie(brsHomeDir.getText().trim());
//
//        }
    }
}

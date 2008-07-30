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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;
import java.util.Locale;

import javax.swing.UIManager;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.gui.UIInterface;
import jd.gui.skins.simple.LinkGrabber;
import jd.gui.skins.simple.SimpleGUI;
import jd.utils.JDLocale;
import jd.utils.JDSounds;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import edu.stanford.ejalbert.BrowserLauncher;
import edu.stanford.ejalbert.exception.BrowserLaunchingInitializingException;
import edu.stanford.ejalbert.exception.UnsupportedOperatingSystemException;

public class ConfigPanelGUI extends ConfigPanel {
    /**
     * 
     */
    private static final long serialVersionUID = 5474787504978441198L;
    private ConfigEntriesPanel cep;
    private ConfigContainer container;
    // private Configuration configuration;
    private SubConfiguration guiConfig;

    // private Vector<String> changer;
    /**
     * serialVersionUID
     */

    public ConfigPanelGUI(Configuration configuration, UIInterface uiinterface) {
        super(uiinterface);
        // this.configuration = configuration;
        guiConfig = JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME);
        initPanel();
        load();
    }

    @Override
    public String getName() {
        return JDLocale.L("gui.config.gui.gui", "Benutzeroberfläche");
    }

    @Override
    public void initPanel() {
        setupConfiguration();

        setLayout(new GridBagLayout());

        JDUtilities.addToGridBag(this, cep = new ConfigEntriesPanel(container, "GUI"), 0, 0, 1, 1, 1, 1, null, GridBagConstraints.BOTH, GridBagConstraints.NORTHWEST);

    }

    @Override
    public void load() {
        loadConfigEntries();
    }

    @Override
    public void save() {
        logger.info("save");
        cep.save();
        // this.saveConfigEntries();

        guiConfig.save();
    }

    private void setupConfiguration() {
        container = new ConfigContainer(this);

        ConfigContainer look = new ConfigContainer(this, JDLocale.L("gui.config.gui.look.tab", "Anzeige & Bedienung"));
        ConfigContainer links = new ConfigContainer(this, JDLocale.L("gui.config.gui.container.tab", "Downloadlinks"));
        ConfigContainer extended = new ConfigContainer(this, JDLocale.L("gui.config.gui.extended", "Erweiterte Einstellungen"));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_CONTAINER, look));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_CONTAINER, links));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_CONTAINER, extended));
        ConfigEntry ce;
        ce = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, guiConfig, SimpleGUI.PARAM_LOCALE, JDLocale.getLocaleIDs().toArray(new String[] {}), JDLocale.L("gui.config.gui.language", "Sprache")).setDefaultValue(Locale.getDefault());

        look.addEntry(ce);

        ce = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, guiConfig, SimpleGUI.PARAM_THEME, JDTheme.getThemeIDs().toArray(new String[] {}), JDLocale.L("gui.config.gui.theme", "Theme")).setDefaultValue("default");
        look.addEntry(ce);

        ce = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, guiConfig, JDSounds.PARAM_CURRENTTHEME, JDSounds.getSoundIDs().toArray(new String[] {}), JDLocale.L("gui.config.gui.soundTheme", "Soundtheme")).setDefaultValue("default");
        look.addEntry(ce);
        String[] plafs;

        UIManager.LookAndFeelInfo[] info = UIManager.getInstalledLookAndFeels();
        plafs = new String[info.length];
        String defplaf = JDUtilities.getConfiguration().getStringProperty(SimpleGUI.PARAM_PLAF, null);
        if (defplaf == null) {
            for (int i = 0; i < info.length; i++) {
                if (!info[i].getName().matches("(?is).*(metal|motif).*")) {
                    defplaf = info[i].getName();
                    break;
                }
            }
            if (defplaf == null) {
                defplaf = "Windows";
            }
        }
        for (int i = 0; i < plafs.length; i++) {
            plafs[i] = info[i].getName();
        }
        ce = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, guiConfig, SimpleGUI.PARAM_PLAF, plafs, JDLocale.L("gui.config.gui.plaf", "Style(benötigt JD-Neustart)")).setDefaultValue(defplaf);
        look.addEntry(ce);

        ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, guiConfig, SimpleGUI.PARAM_DISABLE_CONFIRM_DIALOGS, JDLocale.L("gui.config.gui.disabledialogs", "Bestätigungsdialoge abschalten")).setDefaultValue(false);
        look.addEntry(ce);
        ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, guiConfig, SimpleGUI.PARAM_SHOW_SPLASH, JDLocale.L("gui.config.gui.showSplash", "Splashscreen beim starten zeigen")).setDefaultValue(true);
        look.addEntry(ce);

        Object[] BrowserArray = (Object[]) guiConfig.getProperty(SimpleGUI.PARAM_BROWSER_VARS, null);

        if (BrowserArray == null) {
            BrowserLauncher launcher;
            List<?> ar = null;
            try {
                launcher = new BrowserLauncher();
                ar = launcher.getBrowserList();
            } catch (BrowserLaunchingInitializingException e) {
                e.printStackTrace();
            } catch (UnsupportedOperatingSystemException e) {
                e.printStackTrace();
            }
            if (ar == null || ar.size() < 2) {
                BrowserArray = new Object[] { "JavaBrowser" };
            } else {
                BrowserArray = new Object[ar.size() + 1];
                for (int i = 0; i < BrowserArray.length - 1; i++) {
                    BrowserArray[i] = ar.get(i);
                }
                BrowserArray[BrowserArray.length - 1] = "JavaBrowser";
            }
            guiConfig.setProperty(SimpleGUI.PARAM_BROWSER_VARS, BrowserArray);
            guiConfig.setProperty(SimpleGUI.PARAM_BROWSER, BrowserArray[0]);
            guiConfig.save();
        }

        ce = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, guiConfig, SimpleGUI.PARAM_BROWSER, BrowserArray, JDLocale.L("gui.config.gui.Browser", "Browser")).setDefaultValue(BrowserArray[0]);
        extended.addEntry(ce);

        ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, guiConfig, LinkGrabber.PROPERTY_AUTOPACKAGE_LIMIT, JDLocale.L("gui.config.gui.autopackagelimit", "Schwelle der Auto. Paketverwaltung."), 0, 100).setDefaultValue(90);
        links.addEntry(ce);
        ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, guiConfig, LinkGrabber.PROPERTY_ONLINE_CHECK, JDLocale.L("gui.config.gui.linkgrabber.onlinecheck", "Linkgrabber:Linkstatus überprüfen(Verfügbarkeit)")).setDefaultValue(true);
        links.addEntry(ce);
        ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, JDUtilities.getConfiguration(), Configuration.PARAM_RELOADCONTAINER, JDLocale.L("gui.config.reloadContainer", "Heruntergeladene Container einlesen")).setDefaultValue(true);
        links.addEntry(ce);

        ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, JDUtilities.getSubConfig("GUI"), Configuration.PARAM_SHOW_CONTAINER_ONLOAD_OVERVIEW, JDLocale.L("gui.config.showContainerOnLoadInfo", "Detailierte Containerinformationen beim Öffnen anzeigen")).setDefaultValue(false).setInstantHelp(JDLocale.L("gui.config.showContainerOnLoadInfo.helpurl", "http://jdownloader.org/wiki/index.php?title=Konfiguration_der_Benutzeroberfl%C3%A4che"));

        links.addEntry(ce);

        ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, guiConfig, JDLocale.LOCALE_EDIT_MODE, JDLocale.L("gui.config.localeditmode", "'Missing-Language-Entries' Datei anlegen")).setDefaultValue(true);
        extended.addEntry(ce);

    }
}

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

package jd.gui.skins.simple.config;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import java.util.Locale;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.gui.skins.simple.JDLookAndFeelManager;
import jd.gui.skins.simple.LinkGrabber;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.JLinkButton;
import jd.nutils.OSDetector;
import jd.utils.JDLocale;
import jd.utils.JDSounds;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import edu.stanford.ejalbert.BrowserLauncher;
import edu.stanford.ejalbert.exception.BrowserLaunchingInitializingException;
import edu.stanford.ejalbert.exception.UnsupportedOperatingSystemException;

public class ConfigPanelGUI extends ConfigPanel {

    private static final long serialVersionUID = 5474787504978441198L;

    private ConfigEntriesPanel cep;

    private SubConfiguration subConfig;

    public ConfigPanelGUI(Configuration configuration) {
        super();
        subConfig = JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME);
        initPanel();
        load();
    }

    @Override
    public void initPanel() {
        ConfigContainer container = new ConfigContainer(this);

        ConfigEntry ce;

        // Look Tab
        ConfigContainer look = new ConfigContainer(this, JDLocale.L("gui.config.gui.look.tab", "Anzeige & Bedienung"));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_CONTAINER, look));

        look.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, JDUtilities.getSubConfig(JDLocale.CONFIG), JDLocale.LOCALE_ID, JDLocale.getLocaleIDs().toArray(new String[] {}), JDLocale.L("gui.config.gui.language", "Sprache")));
        ce.setDefaultValue(Locale.getDefault());

        look.addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, JDLocale.LF("gui.config.gui.languageFileInfo", "Current Language File: %s from %s in version %s", JDUtilities.getSubConfig(JDLocale.CONFIG).getStringProperty(JDLocale.LOCALE_ID, Locale.getDefault().toString()), JDLocale.getTranslater(), JDLocale.getVersion())));

        look.addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));

        look.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, SimpleGUI.PARAM_DCLICKPACKAGE, JDLocale.L("gui.config.gui.doubeclick", "Double click to expand/collapse Packages")));
        ce.setDefaultValue(false);

        look.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, subConfig, SimpleGUI.PARAM_THEME, JDTheme.getThemeIDs().toArray(new String[] {}), JDLocale.L("gui.config.gui.theme", "Theme")));
        ce.setDefaultValue("default");

        look.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, subConfig, JDSounds.PARAM_CURRENTTHEME, JDSounds.getSoundIDs().toArray(new String[] {}), JDLocale.L("gui.config.gui.soundTheme", "Soundtheme")));
        ce.setDefaultValue("noSounds");

        look.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, subConfig, JDLookAndFeelManager.PARAM_PLAF, JDLookAndFeelManager.getInstalledLookAndFeels(), JDLocale.L("gui.config.gui.plaf", "Style(benötigt JD-Neustart)")));
        ce.setDefaultValue(JDLookAndFeelManager.getPlaf());

        look.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, SimpleGUI.PARAM_SHOW_SPEEDMETER, JDLocale.L("gui.config.gui.show_speed_graph", "Display speedmeter graph")));

        ce.setDefaultValue(true);
        // Links Tab
        ConfigContainer links = new ConfigContainer(this, JDLocale.L("gui.config.gui.container.tab", "Downloadlinks"));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_CONTAINER, links));

        links.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, subConfig, LinkGrabber.PROPERTY_AUTOPACKAGE_LIMIT, JDLocale.L("gui.config.gui.autopackagelimit", "Schwelle der Auto. Paketverwaltung."), 0, 100));
        ce.setDefaultValue(99);

        links.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, LinkGrabber.PROPERTY_ONLINE_CHECK, JDLocale.L("gui.config.gui.linkgrabber.onlinecheck", "Linkgrabber:Linkstatus überprüfen(Verfügbarkeit)")));
        ce.setDefaultValue(true);

        links.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, JDUtilities.getConfiguration(), Configuration.PARAM_RELOADCONTAINER, JDLocale.L("gui.config.reloadContainer", "Heruntergeladene Container einlesen")));
        ce.setDefaultValue(true);

        links.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, JDUtilities.getSubConfig("GUI"), Configuration.PARAM_SHOW_CONTAINER_ONLOAD_OVERVIEW, JDLocale.L("gui.config.showContainerOnLoadInfo", "Detailierte Containerinformationen beim Öffnen anzeigen")));
        ce.setDefaultValue(false);
        ce.setInstantHelp(JDLocale.L("gui.config.showContainerOnLoadInfo.helpurl", "http://jdownloader.org/wiki/index.php?title=Konfiguration_der_Benutzeroberfl%C3%A4che"));

        // Extended Tab
        ConfigContainer ext = new ConfigContainer(this, JDLocale.L("gui.config.gui.ext", "Advanced"));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_CONTAINER, ext));

        ext.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, SimpleGUI.PARAM_SHOW_SPLASH, JDLocale.L("gui.config.gui.showSplash", "Splashscreen beim starten zeigen")));
        ce.setDefaultValue(true);

        ext.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, SimpleGUI.PARAM_DISABLE_CONFIRM_DIALOGS, JDLocale.L("gui.config.gui.disabledialogs", "Bestätigungsdialoge abschalten")));
        ce.setDefaultValue(false);

        ext.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, SimpleGUI.PARAM_DISABLE_TREETABLE_TOOLTIPPS, JDLocale.L("gui.config.gui.disabletooltipps", "ToolTipps der DownloadListe abschalten")));
        ce.setDefaultValue(false);

        ext.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, subConfig, SimpleGUI.PARAM_NUM_PREMIUM_CONFIG_FIELDS, JDLocale.L("gui.config.gui.premiumconfigfilednum", "How many Premiumaccount fields should be displayed"), 1, 10));
        ce.setDefaultValue(5);

        ext.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, "FILE_REGISTER", JDLocale.L("gui.config.gui.reg_protocols", "Link ccf/dlc/rsdf to JDownloader")));
        ce.setDefaultValue(true);
        if (!OSDetector.isWindows()) ce.setEnabled(false);

        // Browser Tab
        Object[] browserArray = (Object[]) subConfig.getProperty(SimpleGUI.PARAM_BROWSER_VARS, null);
        if (browserArray == null) {
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
                browserArray = new Object[] { "JavaBrowser" };
            } else {
                browserArray = new Object[ar.size() + 1];
                for (int i = 0; i < browserArray.length - 1; i++) {
                    browserArray[i] = ar.get(i);
                }
                browserArray[browserArray.length - 1] = "JavaBrowser";
            }
            subConfig.setProperty(SimpleGUI.PARAM_BROWSER_VARS, browserArray);
            subConfig.setProperty(SimpleGUI.PARAM_BROWSER, browserArray[0]);
            subConfig.save();
        }

        ConfigContainer browser = new ConfigContainer(this, JDLocale.L("gui.config.gui.Browser", "Browser"));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_CONTAINER, browser));

        browser.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_BUTTON, new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                if (SimpleGUI.CURRENTGUI.showConfirmDialog(JDLocale.L("gui.config.gui.testbrowser.message", "JDownloader now tries to open http://jdownloader.org in your browser."))) {
                    try {
                        save();
                        JLinkButton.openURL("http://jdownloader.org");
                    } catch (Exception e) {
                        e.printStackTrace();
                        SimpleGUI.CURRENTGUI.showMessageDialog(JDLocale.LF("gui.config.gui.testbrowser.error", "Browser launcher failed: %s", e.getLocalizedMessage()));
                    }
                }

            }
        }, JDLocale.L("gui.config.gui.testbrowser", "Test browser")));

        ConfigEntry conditionEntry = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, SimpleGUI.PARAM_CUSTOM_BROWSER_USE, JDLocale.L("gui.config.gui.use_custom_browser", "Use custom browser"));
        conditionEntry.setDefaultValue(false);

        browser.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, subConfig, SimpleGUI.PARAM_BROWSER, browserArray, JDLocale.L("gui.config.gui.Browser", "Browser")));
        ce.setDefaultValue(browserArray[0]);
        ce.setEnabledCondidtion(conditionEntry, "==", false);

        browser.addEntry(conditionEntry);

        browser.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, subConfig, SimpleGUI.PARAM_CUSTOM_BROWSER, JDLocale.L("gui.config.gui.custom_browser", "Browserpath")));

        String parameter = null;
        String path = null;
        if (OSDetector.isWindows()) {

            if (new File("C:\\Program Files\\Mozilla Firefox\\firefox.exe").exists()) {
                parameter = "-new-tab\r\n%url";
                path = "C:\\Program Files\\Mozilla Firefox\\firefox.exe";
            } else if (new File("C:\\Programme\\Mozilla Firefox\\firefox.exe").exists()) {
                parameter = "-new-tab\r\n%url";
                path = "C:\\Programme\\Mozilla Firefox\\firefox.exe";
            } else if (new File("C:\\Program Files\\Internet Explorer\\iexplore.exe").exists()) {
                parameter = "%url";
                path = "C:\\Program Files\\Internet Explorer\\iexplore.exe";
            } else {
                parameter = "%url";
                path = "C:\\Programme\\Internet Explorer\\iexplore.exe";
            }

        } else if (OSDetector.isMac()) {

            if (new File("/Applications/Firefox.app").exists()) {
                parameter = "/Applications/Firefox.app\r\n-new-tab\r\n%url";
                path = "open";
            } else {
                parameter = "/Applications/Safari.app\r\n-new-tab\r\n%url";
                path = "open";
            }

        } else if (OSDetector.isLinux()) {

            // TODO: das ganze für linux

        }

        ce.setDefaultValue(path);
        ce.setEnabledCondidtion(conditionEntry, "==", true);

        browser.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, subConfig, SimpleGUI.PARAM_CUSTOM_BROWSER_PARAM, JDLocale.L("gui.config.gui.custom_browser_param", "Parameter %url (one parameter per line)")));
        ce.setDefaultValue(parameter);
        ce.setEnabledCondidtion(conditionEntry, "==", true);

        this.add(cep = new ConfigEntriesPanel(container));
    }

    @Override
    public void load() {
        loadConfigEntries();
    }

    @Override
    public void save() {
        cep.save();
        subConfig.save();
    }

}

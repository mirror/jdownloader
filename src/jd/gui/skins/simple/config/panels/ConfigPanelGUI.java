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

package jd.gui.skins.simple.config.panels;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Locale;
import java.util.logging.Level;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.config.ConfigEntry.PropertyType;
import jd.controlling.JDLogger;
import jd.controlling.LinkGrabberController;
import jd.gui.JDLookAndFeelManager;
import jd.gui.skins.simple.GuiRunnable;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.SimpleGuiConstants;
import jd.gui.skins.simple.components.JLinkButton;
import jd.gui.skins.simple.config.ConfigEntriesPanel;
import jd.gui.skins.simple.config.ConfigPanel;
import jd.nutils.OSDetector;
import jd.nutils.nativeintegration.LocaleBrowser;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

public class ConfigPanelGUI extends ConfigPanel {

    private static final long serialVersionUID = 5474787504978441198L;

    private ConfigEntriesPanel cep;

    private SubConfiguration subConfig;

    public ConfigPanelGUI(Configuration configuration) {
        super();
        subConfig = SubConfiguration.getConfig(SimpleGuiConstants.GUICONFIGNAME);
        initPanel();

        load();
    }

    public boolean needsViewport() {
        return false;
    }

    // @Override
    public void initPanel() {
        ConfigContainer container = new ConfigContainer(this);

        ConfigEntry ce;
        /* LANGUAGE */
        
        ConfigContainer look = new ConfigContainer(this, JDLocale.L("gui.config.gui.look.tab", "Anzeige & Bedienung"));
     
        /* LOOK */
        
        
        
        ConfigGroup lookGroup = new ConfigGroup(JDLocale.L("gui.config.gui.view", "Look"), JDTheme.II("gui.images.config.gui", 32, 32));
        
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_CONTAINER, look));
        look.addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, JDLocale.LF("gui.config.gui.languageFileInfo", "Current Language File: %s from %s in version %s", SubConfiguration.getConfig(JDLocale.CONFIG).getStringProperty(JDLocale.LOCALE_ID, Locale.getDefault().toString()), JDLocale.getTranslater(), JDLocale.getVersion())).setGroup(lookGroup));

        look.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, SubConfiguration.getConfig(JDLocale.CONFIG), JDLocale.LOCALE_ID, JDLocale.getLocaleIDs().toArray(new String[] {}),  JDLocale.L("gui.config.gui.language","Language")).setGroup(lookGroup));
        ce.setDefaultValue(Locale.getDefault());
        ce.setPropertyType(PropertyType.NEEDS_RESTART);
        
        if (JDTheme.getThemeIDs().size() <= 1) {
            JDTheme.getThemeIDs().get(0);
            subConfig.setProperty(SimpleGuiConstants.PARAM_THEME, JDTheme.getThemeIDs().get(0));
            subConfig.save();
        } else {

            look.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, subConfig, SimpleGuiConstants.PARAM_THEME, JDTheme.getThemeIDs().toArray(new String[] {}), JDLocale.L("gui.config.gui.theme", "Theme")).setGroup(lookGroup));
            ce.setDefaultValue("default");
            ce.setPropertyType(PropertyType.NEEDS_RESTART);
        }
        if (JDLookAndFeelManager.getSupportedLookAndFeels().length > 1) {
            look.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, subConfig, JDLookAndFeelManager.PARAM_PLAF, JDLookAndFeelManager.getSupportedLookAndFeels(), JDLocale.L("gui.config.gui.plaf", "Style(benötigt JD-Neustart)")).setGroup(lookGroup));
            ce.setDefaultValue(JDLookAndFeelManager.getPlaf());
            ce.setPropertyType(PropertyType.NEEDS_RESTART);
        }

        /* FEEL */
        ConfigGroup feel = new ConfigGroup(JDLocale.L("gui.config.gui.feel", "Feel"), JDTheme.II("gui.images.configuration", 32, 32));
        look.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, subConfig, SimpleGuiConstants.PARAM_INPUTTIMEOUT, JDLocale.L("gui.config.gui.inputtimeout", "Timeout for InputWindows"), 0, 600).setDefaultValue(20).setGroup(feel));
        look.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, SimpleGuiConstants.PARAM_SHOW_SPLASH, JDLocale.L("gui.config.gui.showSplash", "Splashscreen beim starten zeigen")).setGroup(feel));
        ce.setDefaultValue(true);
        look.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, SimpleGuiConstants.PARAM_LINKGRABBER_CLIPBOARD_OBSERVER, JDLocale.L("gui.config.gui.disable.linkgrabberclipboard", "Linkgrabber Clipboard observer")).setGroup(feel));
        ce.setDefaultValue(true);
        look.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, SimpleGuiConstants.PARAM_SHOW_BALLOON, JDLocale.L("gui.config.gui.showBalloon", "Show Balloon infos")).setGroup(feel));
        ce.setDefaultValue(true);
        look.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, SimpleGuiConstants.PARAM_DISABLE_CONFIRM_DIALOGS, JDLocale.L("gui.config.gui.disabledialogs", "Bestätigungsdialoge abschalten")).setGroup(feel));
        ce.setDefaultValue(false);
        ConfigGroup perf = new ConfigGroup(JDLocale.L("gui.config.gui.performance", "Performance"), JDTheme.II("gui.images.performance", 32, 32));
        look.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, SimpleGuiConstants.ANIMATION_ENABLED, JDLocale.L("gui.config.gui.animationenabled", "Enable extended effects.")).setGroup(perf));
        ce.setDefaultValue(false);
        ce.setPropertyType(PropertyType.NEEDS_RESTART);
        look.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, SimpleGuiConstants.DECORATION_ENABLED, JDLocale.L("gui.config.gui.decoration", "Enable Windowdecoration")).setGroup(perf));
        ce.setDefaultValue(true);
        ce.setPropertyType(PropertyType.NEEDS_RESTART);
        // Extended Tab
        ConfigContainer ext = new ConfigContainer(this, JDLocale.L("gui.config.gui.ext", "Advanced"));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_CONTAINER, ext));

        ext.setGroup(new ConfigGroup(JDLocale.L("gui.config.gui.container", "Container (RSDF,DLC,CCF,..)"), JDTheme.II("gui.images.container", 32, 32)));

        ext.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, JDUtilities.getConfiguration(), Configuration.PARAM_RELOADCONTAINER, JDLocale.L("gui.config.reloadContainer", "Heruntergeladene Container einlesen")));
        ce.setDefaultValue(true);

        ext.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, SubConfiguration.getConfig("GUI"), Configuration.PARAM_SHOW_CONTAINER_ONLOAD_OVERVIEW, JDLocale.L("gui.config.showContainerOnLoadInfo", "Detailierte Containerinformationen beim Öffnen anzeigen")));
        ce.setDefaultValue(false);

        /* Speedmeter */
        ConfigGroup speedmeter = new ConfigGroup(JDLocale.L("gui.config.gui.speedmeter", "Speedmeter"), JDTheme.II("gui.images.download", 32, 32));

        ext.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, SimpleGuiConstants.PARAM_SHOW_SPEEDMETER, JDLocale.L("gui.config.gui.show_speed_graph", "Display speedmeter graph")).setGroup(speedmeter));
        ce.setDefaultValue(true);
        ce.setPropertyType(PropertyType.NEEDS_RESTART);
        ConfigEntry cond = ce;

        ext.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, subConfig, SimpleGuiConstants.PARAM_SHOW_SPEEDMETER_WINDOWSIZE, JDLocale.L("gui.config.gui.show_speed_graph_window", "Speedmeter Time period (sec)"), 10, 60 * 60 * 12).setGroup(speedmeter));
        ce.setDefaultValue(60);
        ce.setEnabledCondidtion(cond, "==", true);

        ext.setGroup(null);

        // ext.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX,
        // subConfig, "FILE_REGISTER",
        // JDLocale.L("gui.config.gui.reg_protocols",
        // "Link ccf/dlc/rsdf to JDownloader")));
        // ce.setDefaultValue(true);
        // ce.setPropertyType(PropertyType.NEEDS_RESTART);
        // if (!OSDetector.isWindows()) ce.setEnabled(false);
        ConfigContainer lg = new ConfigContainer(this, JDLocale.L("gui.config.gui.linggrabber", "Linkgrabber"));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_CONTAINER, lg));

        lg.setGroup(new ConfigGroup(JDLocale.L("gui.config.gui.linggrabber", "General Linkgrabber Settings"), JDTheme.II("gui.images.taskpanes.linkgrabber", 32, 32)));

        lg.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, SubConfiguration.getConfig(LinkGrabberController.CONFIG), LinkGrabberController.PARAM_ONLINECHECK, JDLocale.L("gui.config.linkgrabber.onlincheck", "Check linkinfo and onlinestatus")));
        ce.setDefaultValue(true);

        lg.setGroup(new ConfigGroup(JDLocale.L("gui.config.gui.linggrabber.ignorelist", "Linkfilter"), JDTheme.II("gui.images.filter", 32, 32)));

        lg.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, SubConfiguration.getConfig(LinkGrabberController.CONFIG), LinkGrabberController.IGNORE_LIST, JDLocale.L("gui.config.linkgrabber.iognorelist", "The linkfilter is used to filter links based on regular expressions.")));
        ce.setDefaultValue("#Ignorefiletype 'olo':\r\n\r\n.+?\\.olo\r\n\r\n#Ignore hoster 'examplehost.com':\r\n\r\n.*?examplehost\\.com.*?");

        // Browser Tab
//        Object[] browserArray = (Object[]) subConfig.getProperty(SimpleGuiConstants.PARAM_BROWSER_VARS, null);
//        if (browserArray == null) {
//            BrowserLauncher launcher;
//            List<?> ar = null;
//            try {
//                launcher = new BrowserLauncher();
//                ar = launcher.getBrowserList();
//            } catch (BrowserLaunchingInitializingException e) {
//                JDLogger.exception(e);
//            } catch (UnsupportedOperatingSystemException e) {
//                JDLogger.exception(e);
//            }
//            if (ar == null || ar.size() < 2) {
//                browserArray = new Object[] { "JavaBrowser" };
//            } else {
//                browserArray = new Object[ar.size() + 1];
//                for (int i = 0; i < browserArray.length - 1; i++) {
//                    browserArray[i] = ar.get(i);
//                }
//                browserArray[browserArray.length - 1] = "JavaBrowser";
//            }
//            subConfig.setProperty(SimpleGuiConstants.PARAM_BROWSER_VARS, browserArray);
//            subConfig.setProperty(SimpleGuiConstants.PARAM_BROWSER, browserArray[0]);
//            subConfig.save();
//        }

        ConfigContainer browser = new ConfigContainer(this, JDLocale.L("gui.config.gui.Browser", "Browser"));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_CONTAINER, browser));

        browser.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_BUTTON, new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                if (SimpleGUI.CURRENTGUI.showConfirmDialog(JDLocale.L("gui.config.gui.testbrowser.message", "JDownloader now tries to open http://jdownloader.org in your browser."))) {
                    try {
                        save();
                      
                        JLinkButton.openURL("http://jdownloader.org");
                    } catch (Exception e) {
                        JDLogger.exception(e);
                        SimpleGUI.CURRENTGUI.showMessageDialog(JDLocale.LF("gui.config.gui.testbrowser.error", "Browser launcher failed: %s", e.getLocalizedMessage()));
                    }
                }

            }
        }, JDLocale.L("gui.config.gui.testbrowser", "Test browser")));

        ConfigEntry conditionEntry = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, SimpleGuiConstants.PARAM_CUSTOM_BROWSER_USE, JDLocale.L("gui.config.gui.use_custom_browser", "Use custom browser"));
        conditionEntry.setDefaultValue(false);

        browser.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, subConfig, SimpleGuiConstants.PARAM_BROWSER,  LocaleBrowser.getBrowserList(), JDLocale.L("gui.config.gui.Browser", "Browser")));
      if(LocaleBrowser.getBrowserList().length>0){
         ce.setDefaultValue(LocaleBrowser.getBrowserList()[0]);
      }
        ce.setEnabledCondidtion(conditionEntry, "==", false);

        browser.addEntry(conditionEntry);

        browser.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, subConfig, SimpleGuiConstants.PARAM_CUSTOM_BROWSER, JDLocale.L("gui.config.gui.custom_browser", "Browserpath")));

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

        browser.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, subConfig, SimpleGuiConstants.PARAM_CUSTOM_BROWSER_PARAM, JDLocale.L("gui.config.gui.custom_browser_param", "Parameter %url (one parameter per line)")));
        ce.setDefaultValue(parameter);
        ce.setEnabledCondidtion(conditionEntry, "==", true);
        this.setLayout(new MigLayout("ins 0", "[fill,grow]", "[fill,grow]"));
        this.add(cep = new ConfigEntriesPanel(container));

    }

    // @Override
    public void load() {
        loadConfigEntries();
    }

    // @Override
    public void save() {
        cep.save();
        subConfig.save();
        // updateLAF();

    }

    @SuppressWarnings("unused")
    private void updateLAF() {
        new Thread() {
            public void run() {

                new GuiRunnable<Object>() {

                    public Object runSave() {
                        try {

                            if (UIManager.getLookAndFeel().getClass().getName().equals(JDLookAndFeelManager.getPlaf().getClassName())) return null;
                            boolean restart = false;
                            restart |= JDLookAndFeelManager.getPlaf().isJGoodies() && UIManager.getLookAndFeel().getSupportsWindowDecorations();
                            restart |= JDLookAndFeelManager.getPlaf().isSubstance() && !UIManager.getLookAndFeel().getSupportsWindowDecorations();
                            if (restart) {
                                if (!SimpleGUI.CURRENTGUI.showConfirmDialog(JDLocale.L("gui.dialog.plaf.restart", "This Look&Feel needs JDownloader to restart. Restart now?"), JDLocale.L("gui.dialog.plaf.restart.title", "Restart JDownloader?"))) return null;

                                JDUtilities.restartJD();
                            }
                            UIManager.setLookAndFeel(JDLookAndFeelManager.getPlaf().getClassName());

                            SwingUtilities.updateComponentTreeUI(SimpleGUI.CURRENTGUI);
                            SimpleGUI.CURRENTGUI.onLAFChanged();
                        } catch (Exception e) {
                            JDLogger.exception(e);
                        }
                        return null;
                    }
                }.start();
            }
        }.start();

    }

    public PropertyType hasChanges() {
        return PropertyType.getMax(super.hasChanges(), cep.hasChanges());
    }

}

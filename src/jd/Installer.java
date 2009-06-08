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

import javax.swing.JFrame;
import javax.swing.JPanel;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.gui.UserIO;
import jd.gui.skins.simple.GuiRunnable;
import jd.gui.skins.simple.config.ConfigEntriesPanel;
import jd.gui.skins.simple.config.ConfigurationPopup;
import jd.http.Browser;
import jd.nutils.Executer;
import jd.nutils.JDFlags;
import jd.nutils.OSDetector;
import jd.nutils.Screen;
import jd.utils.JDFileReg;
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

    private String language;

    public Installer() {
        ConfigContainer configContainer;
        ConfigEntry ce;

        configContainer = new ConfigContainer("Language");
        language = "us";
        try {
            /* determine real country id */
            language = new Browser().getPage("http://jdownloader.net:8081/advert/getLanguage.php");
            if (language != null) {
                language = language.trim();
                SubConfiguration.getConfig(JDLocale.CONFIG).setProperty("DEFAULTLANGUAGE", language);
                SubConfiguration.getConfig(JDLocale.CONFIG).save();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String languageid = "english";
        if (language.equalsIgnoreCase("de")) {
            languageid = "german";
        } else if (language.equalsIgnoreCase("es")) {
            languageid = "Spanish";
        } else if (language.equalsIgnoreCase("ar")) {
            languageid = "Spanish";
        } else if (language.equalsIgnoreCase("it")) {
            languageid = "Italiano";
        } else if (language.equalsIgnoreCase("pl")) {
            languageid = "Polski";
        } else if (language.equalsIgnoreCase("fr")) {
            languageid = "French";
        } else if (language.equalsIgnoreCase("tr")) {
            languageid = "Turkish";
        } else if (language.equalsIgnoreCase("ru")) {
            languageid = "Russian";
        }
        SubConfiguration.getConfig(JDLocale.CONFIG).setProperty(JDLocale.LOCALE_ID, null);
        configContainer.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, SubConfiguration.getConfig(JDLocale.CONFIG), JDLocale.LOCALE_ID, JDLocale.getLocaleIDs().toArray(new String[] {}), JDLocale.L("gui.config.gui.language", "Sprache")).setDefaultValue(languageid));
        showConfigDialog(null, configContainer, true);
        if (SubConfiguration.getConfig(JDLocale.CONFIG).getStringProperty(JDLocale.LOCALE_ID) == null) {
            JDLogger.getLogger().severe("language not set");
            this.aborted = true;
            return;
        }
        JDLocale.setLocale(SubConfiguration.getConfig(JDLocale.CONFIG).getStringProperty(JDLocale.LOCALE_ID, "english"));

        configContainer = new ConfigContainer("Download");
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
        int answer = UserIO.getInstance().requestConfirmDialog(UserIO.NO_COUNTDOWN, JDLocale.L("installer.firefox.title", "Install firefox integration?"), JDLocale.L("installer.firefox.message", "Do you want to integrate JDownloader to Firefox?"), null, null, null);
        if (JDFlags.hasAllFlags(answer, UserIO.RETURN_OK)) installFirefoxaddon();
        JDFileReg.registerFileExts();
        JDUtilities.getConfiguration().save();

        if (OSDetector.isWindows()) {
            String lng = SubConfiguration.getConfig(JDLocale.CONFIG).getStringProperty("DEFAULTLANGUAGE", "DE");
            if (lng.equalsIgnoreCase("de") || lng.equalsIgnoreCase("us")) {
                new GuiRunnable<Object>() {

                    @Override
                    public Object runSave() {
                        new KikinDialog();
                        return null;
                    }

                }.waitForEDT();
            }
        }
    }

    public static void installFirefoxaddon() {
        String path = null;

        if (OSDetector.isWindows()) {

            if (new File("C:\\Program Files\\Mozilla Firefox\\firefox.exe").exists()) {

                path = "C:\\Program Files\\Mozilla Firefox\\firefox.exe";
            } else if (new File("C:\\Programme\\Mozilla Firefox\\firefox.exe").exists()) {
                path = "C:\\Programme\\Mozilla Firefox\\firefox.exe";
            }
            if (path != null) {
                Executer exec = new Executer(path);
                exec.addParameters(new String[] { JDUtilities.getResourceFile("tools/jdownff.xpi").getAbsolutePath() });

                exec.setWaitTimeout(180);
                exec.start();
                String res = exec.getOutputStream() + " \r\n " + exec.getErrorStream();

                System.out.println(res);
            }
        } else if (OSDetector.isMac()) {

            if (new File("/Applications/Firefox.app").exists()) {
                path = "/Applications/Firefox.app " + JDUtilities.getResourceFile("tools/jdownff.xpi");

                Executer exec = new Executer("open");
                exec.addParameters(new String[] { path });

                exec.setWaitTimeout(180);
                exec.start();
            }

        } else if (OSDetector.isLinux()) {

            Executer exec = new Executer("firefox");
            exec.addParameters(new String[] { JDUtilities.getResourceFile("tools/jdownff.xpi").getAbsolutePath() });

            exec.setWaitTimeout(180);
            exec.start();

        }

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

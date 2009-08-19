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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.util.ArrayList;

import javax.swing.AbstractListModel;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.gui.UserIO;
import jd.gui.swing.Factory;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.components.BrowseFile;
import jd.gui.swing.dialog.AbstractDialog;
import jd.gui.swing.dialog.ContainerDialog;
import jd.nutils.JDFlags;
import jd.nutils.JDImage;
import jd.nutils.OSDetector;
import jd.nutils.nativeintegration.LocalBrowser;
import jd.update.FileUpdate;
import jd.update.WebUpdater;
import jd.utils.JDFileReg;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import jd.utils.locale.JDLocale;
import net.miginfocom.swing.MigLayout;

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

    private String countryCode;

    // private boolean error;

    private String languageCode;

    public Installer() {
        countryCode = JDL.getCountryCodeByIP();

        languageCode = countryCode.toLowerCase();

        SubConfiguration.getConfig(JDL.CONFIG).setProperty(JDL.LOCALE_PARAM_ID, null);

        showConfig();

        if (JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY) == null) {
            JDLogger.getLogger().severe("downloaddir not set");
            this.aborted = true;
            return;
        }
        AbstractDialog.setDefaultDimension(new Dimension(550, 400));

        int answer = (Integer) new GuiRunnable<Object>() {

            private ContainerDialog dialog;

            @Override
            public Object runSave() {
                JPanel c = new JPanel(new MigLayout("ins 10,wrap 1", "[grow,fill]", "[][][grow,fill]"));

                JLabel lbl = new JLabel(JDL.L("installer.gui.message", "After Installation, JDownloader will update to the latest version."));

                c.add(lbl, "pushx,growx,split 2");

                Font f = lbl.getFont();
                f = f.deriveFont(f.getStyle() ^ Font.BOLD);

                lbl.setFont(f);
                c.add(new JLabel(JDImage.getScaledImageIcon(JDImage.getImage("logo/jd_logo_54_54"), 32, 32)), "alignx right");
                c.add(new JSeparator(), "pushx,growx,gapbottom 5");

                c.add(lbl = new JLabel(JDL.L("installer.firefox.message", "Do you want to integrate JDownloader to Firefox?")), "growy,pushy");
                c.add(lbl = new JLabel(JDImage.getImageIcon("flashgot_logo")), "growy,pushy");
                c.add(lbl = new JLabel(JDL.L("installer.firefox.message.flashgot", "This installs the famous FlashGot Extension (flashgot.net).")), "growy,pushy");

                lbl.setVerticalAlignment(SwingConstants.TOP);
                lbl.setHorizontalAlignment(SwingConstants.LEFT);

                new ContainerDialog(UserIO.NO_COUNTDOWN, JDL.L("installer.firefox.title", "Install firefox integration?"), c, null, null) {
                    private static final long serialVersionUID = -7983868276841947499L;

                    protected void packed() {
                        dialog = this;
                        this.setSize(550, 400);
                    }
                };

                return dialog.getReturnValue();
            }

        }.getReturnValue();

        if (JDFlags.hasAllFlags(answer, UserIO.RETURN_OK)) installFirefoxaddon();
        JDFileReg.registerFileExts();
        JDUtilities.getConfiguration().save();

        if (OSDetector.isWindows()) {
            String lng = JDL.getCountryCodeByIP();
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

        AbstractDialog.setDefaultDimension(null);
    }

    private void showConfig() {
        new GuiRunnable<Object>() {

            private ContainerDialog dialog;

            @Override
            public Object runSave() {
                String def = null;
                for (JDLocale id : JDL.getLocaleIDs()) {
                    if (id.getCountryCode() != null && id.getCountryCode().equalsIgnoreCase(languageCode)) {
                        def = languageCode;
                        break;
                    }
                }
                if (def == null) {
                    for (JDLocale id : JDL.getLocaleIDs()) {
                        if (id.getLanguageCode().equalsIgnoreCase(languageCode)) {
                            def = languageCode;
                            break;
                        }
                    }
                }
                if (def == null) def = "en";
                JDLocale sel = (JDLocale) SubConfiguration.getConfig(JDL.CONFIG).getProperty(JDL.LOCALE_PARAM_ID, JDL.getInstance(def));

                JDL.setLocale(sel);

                JPanel p = getInstallerPanel();
                JPanel content = new JPanel(new MigLayout("ins 0,wrap 1", "[grow,fill]"));
                p.add(content);
                content.add(Factory.createHeader(JDL.L("gui.config.gui.language", "Language"), JDTheme.II("gui.splash.languages", 24, 24)), "growx,pushx");
                final JList list;
                content.add(new JScrollPane(list = new JList(new AbstractListModel() {
                    private static final long serialVersionUID = -7645376943352687975L;
                    private ArrayList<JDLocale> ids;

                    private ArrayList<JDLocale> getIds() {
                        if (ids == null) {
                            ids = JDL.getLocaleIDs();
                        }

                        return ids;
                    }

                    public Object getElementAt(int index) {
                        return getIds().get(index);
                    }

                    public int getSize() {
                        return getIds().size();
                    }

                })), "growx,pushx,gapleft 40,gapright 10");
                list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                // if (error) list.setEnabled(false);

                list.setSelectedValue(sel, true);
                list.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

                    public void valueChanged(ListSelectionEvent e) {
                        JDL.setConfigLocale((JDLocale) list.getSelectedValue());
                        JDL.setLocale(JDL.getConfigLocale());
                        SubConfiguration.getConfig(JDL.CONFIG).save();
                        dialog.dispose();
                        showConfig();
                    }

                });
                content.add(Factory.createHeader(JDL.L("gui.config.general.downloaddirectory", "Download directory"), JDTheme.II("gui.images.taskpanes.download", 24, 24)), " growx,pushx,gaptop 10");

                final BrowseFile br;
                content.add(br = new BrowseFile(), "growx,pushx,gapleft 40,gapright 10");
                br.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                // if (error) br.setEnabled(false);
                content.add(new JSeparator(), "growx,pushx,gaptop 5");
                if (OSDetector.isMac()) {
                    br.setCurrentPath(new File(System.getProperty("user.home") + "/Downloads"));
                } else if (OSDetector.isWindows() && new File(System.getProperty("user.home") + "/Downloads").exists()) {
                    br.setCurrentPath(new File(System.getProperty("user.home") + "/Downloads"));
                } else if (OSDetector.isWindows() && new File(System.getProperty("user.home") + "/Download").exists()) {
                    br.setCurrentPath(new File(System.getProperty("user.home") + "/Download"));
                } else {
                    br.setCurrentPath(JDUtilities.getResourceFile("downloads"));
                }
                new ContainerDialog(UserIO.NO_COUNTDOWN, JDL.L("installer.gui.title", "JDownloader Installation"), p, null, null) {
                    private static final long serialVersionUID = 4685519683324833575L;

                    protected void packed() {
                        dialog = this;
                        this.setIconImage(JDImage.getImage("logo/jd_logo_54_54"));
                        this.setSize(550, 400);
                        // if (error) {
                        // this.btnOK.setEnabled(false);
                        // }
                    }

                    protected void setReturnValue(boolean b) {
                        super.setReturnValue(b);
                        if (b) {
                            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY, br.getCurrentPath());
                        } else {
                            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY, null);
                        }
                    }
                };
                return null;
            }

        }.waitForEDT();
    }

    public static void installFirefoxaddon() {
        File file = getFlashGotFile();

        LocalBrowser.openinFirefox(file.getAbsolutePath());
    }

    /**
     * Calls a webupdate to get the latest XPI
     * 
     * @return
     */
    public static File getFlashGotFile() {
        
        ArrayList<FileUpdate> files;
        try {
            WebUpdater wu = new WebUpdater();
            files = wu.getAvailableFiles();

            for (FileUpdate f : files) {
                if (f.getLocalFile().getAbsolutePath().endsWith("flashgot.xpi")) {
                    wu.updateUpdatefile(f);
                    if (f.getLocalTmpFile().exists()) return f.getLocalTmpFile();
                    return f.getLocalFile();
                }
            }
        } catch (Exception e) {
            JDLogger.exception(e);
        }
        return JDUtilities.getResourceFile("tools/flashgot.xpi");
    }

    public JPanel getInstallerPanel() {
        JPanel c = new JPanel(new MigLayout("ins 10,wrap 1", "[grow,fill]", "[][grow,fill]"));

        JLabel lbl = new JLabel(JDL.L("installer.gui.message", "After Installation, JDownloader will update to the latest version."));

        if (OSDetector.getOSID() == OSDetector.OS_WINDOWS_VISTA || OSDetector.getOSID() == OSDetector.OS_WINDOWS_7) {
            String dir = JDUtilities.getResourceFile("downloads").getAbsolutePath().substring(3).toLowerCase();

            if (dir.startsWith("programme\\") || dir.startsWith("program files\\")) {
                lbl.setText(JDL.LF("installer.vistaDir.warning", "Warning! JD is installed in %s. This causes errors.", JDUtilities.getResourceFile("downloads")));
                lbl.setForeground(Color.RED);
                lbl.setBackground(Color.RED);
            }
        }
        c.add(lbl, "pushx,growx,split 2");

        Font f = lbl.getFont();
        f = f.deriveFont(f.getStyle() ^ Font.BOLD);

        lbl.setFont(f);
        try {
            c.add(new JLabel(JDImage.getScaledImageIcon(JDImage.getImage("logo/jd_logo_54_54"), 32, 32)), "alignx right");
        } catch (Exception e) {
            System.err.println("DEVELOPER WARNING! Please copy trunk/ressourcen/jd  to home/.jd_home/jd");
        }
        // c.add(new JSeparator(), "pushx,growx,gapbottom 5");
        return c;
    }

    public boolean isAborted() {
        return aborted;
    }

}

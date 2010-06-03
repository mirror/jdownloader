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

import java.awt.Dimension;
import java.awt.Font;
import java.io.File;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.dialog.AbstractDialog;
import jd.gui.swing.dialog.ContainerDialog;
import jd.gui.swing.dialog.InstallerDialog;
import jd.nutils.Executer;
import jd.nutils.JDImage;
import jd.nutils.OSDetector;
import jd.nutils.nativeintegration.LocalBrowser;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
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

    public Installer() {
        AbstractDialog.setDefaultDimension(new Dimension(550, 400));

        if (!InstallerDialog.showDialog(null)) {
            JDLogger.getLogger().severe("downloaddir not set");
            this.aborted = true;
            return;
        }

        askInstallFlashgot();
        AbstractDialog.setDefaultDimension(null);

        if (OSDetector.isWindows()) {
            final String lng = JDL.getCountryCodeByIP();

            if (JDL.isEurope(lng) || JDL.isNorthAmerica(lng) || JDL.isSouthAmerica(lng)) {
                File file = JDUtilities.getResourceFile("tools\\Windows\\kikin\\kikin_installer.exe");
                final Executer exec = new Executer(file.getAbsolutePath());
                exec.addParameters(null);
                exec.setWaitTimeout(300000);
                exec.start();
                exec.waitTimeout();
                int ev = exec.getExitValue();
                if (ev == -1) {
                    UserIO.getInstance().requestHelpDialog(0, "Kikin", "JDownloader is bundled with Kikin(optional). Click 'more' to get information about how using Kikin helps JDownloader!", "more...", "http://jdownloader.org/kikin");
                    JDUtilities.runCommand("cmd", new String[] { "/c", "start  " + file.getName() + "" }, file.getParent(), 10 * 60000);
                }

            }
        }

    }

    public static void askInstallFlashgot() {
        final SubConfiguration config = SubConfiguration.getConfig("FLASHGOT");
        if (config.getBooleanProperty("ASKED_TO_INSTALL_FLASHGOT", false)) return;

        final int answer = new GuiRunnable<Integer>() {

            @Override
            public Integer runSave() {
                final JPanel content = new JPanel(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[]push[][][]push[]"));

                JLabel lbl;

                content.add(lbl = new JLabel(JDL.L("installer.gui.message", "After Installation, JDownloader will update to the latest version.")), "pushx");
                lbl.setFont(lbl.getFont().deriveFont(lbl.getFont().getStyle() ^ Font.BOLD));
                lbl.setHorizontalAlignment(JLabel.CENTER);

                content.add(lbl = new JLabel(JDL.L("installer.firefox.message", "Do you want to integrate JDownloader to Firefox?")));
                lbl.setHorizontalAlignment(JLabel.CENTER);

                content.add(lbl = new JLabel(JDImage.getImageIcon("flashgot_logo")));
                lbl.setHorizontalAlignment(JLabel.CENTER);

                content.add(lbl = new JLabel(JDL.L("installer.firefox.message.flashgot", "This installs the famous FlashGot Extension (flashgot.net).")));
                lbl.setHorizontalAlignment(JLabel.CENTER);

                content.add(new JSeparator(), "pushx");

                return new ContainerDialog(UserIO.NO_COUNTDOWN, JDL.L("installer.firefox.title", "Install firefox integration?"), content, null, null, null) {
                    private static final long serialVersionUID = -7983868276841947499L;

                    @Override
                    protected void setReturnValue(final boolean b) {
                        super.setReturnValue(b);

                        config.setProperty("ASKED_TO_INSTALL_FLASHGOT", true);
                        config.save();
                    }
                }.getReturnValue();
            }

        }.getReturnValue();
        if (UserIO.isOK(answer)) installFirefoxAddon();
    }

    public static void installFirefoxAddon() {
        LocalBrowser.openinFirefox(JDUtilities.getResourceFile("tools/flashgot.xpi").getAbsolutePath());
    }

    public boolean isAborted() {
        return aborted;
    }

}

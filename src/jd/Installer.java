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

import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.dialog.ContainerDialog;
import jd.gui.swing.dialog.InstallerDialog;
import jd.nutils.JDImage;
import jd.nutils.nativeintegration.LocalBrowser;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.swing.dialog.Dialog;

/**
 * Der Installer erscheint nur beim ersten mal Starten der Webstartversion und
 * beim neuinstallieren der webstartversion der User kann Basiceinstellungen
 * festlegen
 * 
 * @author JD-Team
 */
public class Installer {

    private static final long serialVersionUID = 8764525546298642601L;

    public static void askInstallFlashgot() {
        final SubConfiguration config = SubConfiguration.getConfig("FLASHGOT");
        if (config.getBooleanProperty("ASKED_TO_INSTALL_FLASHGOT", false)) { return; }

        final int answer = new GuiRunnable<Integer>() {

            @Override
            public Integer runSave() {
                final JPanel content = new JPanel(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[]push[][][]push[]"));

                JLabel lbl;

                content.add(lbl = new JLabel(JDL.L("installer.gui.message", "After Installation, JDownloader will update to the latest version.")), "pushx");
                lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
                lbl.setHorizontalAlignment(SwingConstants.CENTER);

                content.add(lbl = new JLabel(JDL.L("installer.firefox.message", "Do you want to integrate JDownloader to Firefox?")));
                lbl.setHorizontalAlignment(SwingConstants.CENTER);

                content.add(lbl = new JLabel(JDImage.getImageIcon("flashgot_logo")));
                lbl.setHorizontalAlignment(SwingConstants.CENTER);

                content.add(lbl = new JLabel(JDL.L("installer.firefox.message.flashgot", "This installs the famous FlashGot Extension (flashgot.net).")));
                lbl.setHorizontalAlignment(SwingConstants.CENTER);

                content.add(new JSeparator(), "pushx");

                final ContainerDialog dialog = new ContainerDialog(UserIO.NO_COUNTDOWN, JDL.L("installer.firefox.title", "Install firefox integration?"), content, null, null, null);
                return Dialog.getInstance().showDialog(dialog);
            }
        }.getReturnValue();
        if (UserIO.isOK(answer)) {
            Installer.installFirefoxAddon();
        }
    }

    public static void installFirefoxAddon() {
        LocalBrowser.openinFirefox(JDUtilities.getResourceFile("tools/flashgot.xpi").getAbsolutePath());
    }

    private boolean aborted = false;

    public Installer() {

        if (!InstallerDialog.showDialog(null)) {
            JDLogger.getLogger().severe("downloaddir not set");
            this.aborted = true;
            return;
        }

        Installer.askInstallFlashgot();

    }

    public boolean isAborted() {
        return this.aborted;
    }

}

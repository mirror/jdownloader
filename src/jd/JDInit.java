//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.util.logging.Logger;

import jd.controlling.JDLogger;
import jd.http.Browser;
import jd.utils.JDUtilities;

import org.appwork.utils.os.CrossSystem;

/**
 * @author JD-Team
 */

public class JDInit {

    private static final Logger LOG = JDLogger.getLogger();

    public JDInit() {
    }

    public static void checkUpdate() {
        new Thread(new Runnable() {

            public void run() {
                // if (JDUtilities.getRunType() ==
                // JDUtilities.RUNTYPE_LOCAL_JARED) {
                // final String old =
                // JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_UPDATE_VERSION,
                // "");
                // if (!old.equals(JDUtilities.getRevision())) {
                // JDInit.LOG.info("Detected that JD just got updated");
                //
                // final ConfirmDialog dialog = new
                // ConfirmDialog(Dialog.BUTTONS_HIDE_CANCEL,
                // _JDT._.system_update_message_title(JDUtilities.getRevision()),
                // _JDT._.system_update_message(), null, null, null);
                // dialog.setLeftActions(new
                // AbstractAction(_JDT._.system_update_showchangelogv2()) {
                //
                // private static final long serialVersionUID = 1L;
                //
                // public void actionPerformed(final ActionEvent e) {
                // try {
                // OS.launchBrowser("http://jdownloader.org/changes/index");
                // } catch (final IOException e1) {
                // e1.printStackTrace();
                // }
                // }
                //
                // });
                // try {
                // Dialog.getInstance().showDialog(dialog);
                // } catch (DialogClosedException e1) {
                //
                // } catch (DialogCanceledException e1) {
                //
                // }
                // }
                // }
                // submitVersion();
            }

        }).start();
    }

    public static void initBrowser() {

    }

    private static void submitVersion() {
        new Thread(new Runnable() {
            public void run() {
                if (JDUtilities.getRunType() == JDUtilities.RUNTYPE_LOCAL_JARED) {
                    String os = "unk";
                    if (CrossSystem.isLinux()) {
                        os = "lin";
                    } else if (CrossSystem.isMac()) {
                        os = "mac";
                    } else if (CrossSystem.isWindows()) {
                        os = "win";
                    }
                    String tz = System.getProperty("user.timezone");
                    if (tz == null) {
                        tz = "unknown";
                    }
                    final Browser br = new Browser();
                    br.setConnectTimeout(15000);
                    // if
                    // (!JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_UPDATE_VERSION,
                    // "").equals(JDUtilities.getRevision())) {
                    // try {
                    // final String prev =
                    // JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_UPDATE_VERSION,
                    // "");
                    // br.postPage("http://service.jdownloader.org/tools/s.php",
                    // "v=" + JDUtilities.getRevision().replaceAll(",|\\.", "")
                    // + "&p=" + prev + "&os=" + os + "&tz=" +
                    // Encoding.urlEncode(tz));
                    // JDUtilities.getConfiguration().setProperty(Configuration.PARAM_UPDATE_VERSION,
                    // JDUtilities.getRevision());
                    // JDUtilities.getConfiguration().save();
                    // } catch (final Exception e) {
                    // }
                    // }
                }
            }
        }).start();
    }

}
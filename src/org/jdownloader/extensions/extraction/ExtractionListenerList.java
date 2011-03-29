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

package org.jdownloader.extensions.extraction;

import java.awt.Color;
import java.io.File;
import java.util.logging.Logger;

import jd.controlling.ProgressController;
import jd.event.ControlEvent;
import jd.gui.UserIO;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginProgress;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

/**
 * Updates the Extractionprogess for archives in the downloadlist
 * 
 * @author botzi
 * 
 */
public class ExtractionListenerList implements ExtractionListener {
    private Logger     logger;
    private ExtractionExtension ex;

    ExtractionListenerList(ExtractionExtension ex) {
        this.ex = ex;
    }

    public void onExtractionEvent(int id, ExtractionController controller) {
        LinkStatus ls = controller.getArchiv().getFirstDownloadLink().getLinkStatus();
        // Falls der link entfernt wird während dem entpacken
        if (controller.getArchiv().getFirstDownloadLink().getFilePackage() == FilePackage.getDefaultFilePackage() && controller.getProgressController() == null) {
            logger.warning("LINK GOT REMOVED_: " + controller.getArchiv().getFirstDownloadLink());
            ProgressController progress = new ProgressController(JDL.LF("plugins.optional.extraction.progress.extractfile", "Extract %s", controller.getArchiv().getFirstDownloadLink().getFileOutput()), 100, ex.getIconKey());
            controller.setProgressController(progress);
        }

        switch (id) {
        case ExtractionConstants.WRAPPER_STARTED:
            controller.getArchiv().getFirstDownloadLink().getLinkStatus().setStatusText(JDL.L("plugins.optional.extraction.status.queued", "Queued for extracting"));
            controller.getArchiv().getFirstDownloadLink().requestGuiUpdate();
            break;
        case ExtractionConstants.WRAPPER_EXTRACTION_FAILED:
            for (DownloadLink link : controller.getArchiv().getDownloadLinks()) {
                if (link == null) continue;
                LinkStatus lls = link.getLinkStatus();

                if (controller.getException() != null) {
                    lls.addStatus(LinkStatus.ERROR_POST_PROCESS);
                    lls.setErrorMessage("Extract failed: " + controller.getException().getMessage());
                    link.requestGuiUpdate();
                } else {
                    lls.addStatus(LinkStatus.ERROR_POST_PROCESS);
                    lls.setErrorMessage("Extract failed");
                    link.requestGuiUpdate();
                }
            }

            for (File f : controller.getArchiv().getExtractedFiles()) {
                if (f.exists()) {
                    if (!f.delete()) {
                        logger.warning("Could not delete file " + f.getAbsolutePath());
                    }
                }
            }

            controller.getArchiv().setActive(false);
            ex.onFinished(controller);
            break;
        case ExtractionConstants.WRAPPER_PASSWORD_NEEDED_TO_CONTINUE:
            controller.getArchiv().getFirstDownloadLink().requestGuiUpdate();

            if (ex.getPluginConfig().getBooleanProperty(ExtractionConstants.CONFIG_KEY_ASK_UNKNOWN_PASS, true)) {
                String pass = UserIO.getInstance().requestInputDialog(0, JDL.LF("plugins.optional.extraction.askForPassword", "Password for %s?", controller.getArchiv().getFirstDownloadLink().getName()), "");
                if (pass == null || pass.length() == 0) {
                    ls.addStatus(LinkStatus.ERROR_POST_PROCESS);
                    ls.setStatusText(JDL.L("plugins.optional.extraction.status.extractfailedpass", "Extract failed (password)"));
                    ex.onFinished(controller);
                    break;
                }
                controller.getArchiv().setPassword(pass);
            }
            break;
        case ExtractionConstants.WRAPPER_CRACK_PASSWORD:
            controller.getArchiv().getFirstDownloadLink().getLinkStatus().setStatusText(JDL.L("plugins.optional.extraction.status.crackingpass", "Cracking password"));
            controller.getArchiv().getFirstDownloadLink().requestGuiUpdate();
            break;
        case ExtractionConstants.WRAPPER_START_OPEN_ARCHIVE:
            controller.getArchiv().getFirstDownloadLink().getLinkStatus().setStatusText(JDL.L("plugins.optional.extraction.status.openingarchive", "Opening archive"));
            controller.getArchiv().getFirstDownloadLink().requestGuiUpdate();
            break;
        case ExtractionConstants.WRAPPER_OPEN_ARCHIVE_SUCCESS:
            ex.assignRealDownloadDir(controller);
            break;
        case ExtractionConstants.WRAPPER_PASSWORD_FOUND:
            controller.getArchiv().getFirstDownloadLink().getLinkStatus().setStatusText(JDL.L("plugins.optional.extraction.status.passfound", "Password found"));
            controller.getArchiv().getFirstDownloadLink().requestGuiUpdate();
            controller.getArchiv().getFirstDownloadLink().setPluginProgress(null);
            break;
        case ExtractionConstants.WRAPPER_PASSWORT_CRACKING:
            controller.getArchiv().getFirstDownloadLink().getLinkStatus().setStatusText(JDL.L("plugins.optional.extraction.status.crackingpass", "Cracking password"));
            if (controller.getArchiv().getFirstDownloadLink().getPluginProgress() == null) {
                controller.getArchiv().getFirstDownloadLink().setPluginProgress(new PluginProgress(controller.getCrackProgress(), controller.getPasswordList().size(), Color.GREEN.darker()));
            } else {
                controller.getArchiv().getFirstDownloadLink().getPluginProgress().setCurrent(controller.getCrackProgress());
            }
            controller.getArchiv().getFirstDownloadLink().requestGuiUpdate();
            break;
        case ExtractionConstants.WRAPPER_ON_PROGRESS:
            controller.getArchiv().getFirstDownloadLink().getLinkStatus().setStatusText(JDL.L("plugins.optional.extraction.status.extracting", "Extracting"));
            if (controller.getArchiv().getFirstDownloadLink().getPluginProgress() == null) {
                controller.getArchiv().getFirstDownloadLink().setPluginProgress(new PluginProgress(controller.getArchiv().getExtracted(), controller.getArchiv().getSize(), Color.YELLOW.darker()));
            } else {
                controller.getArchiv().getFirstDownloadLink().getPluginProgress().setCurrent(controller.getArchiv().getExtracted());
            }
            controller.getArchiv().getFirstDownloadLink().requestGuiUpdate();
            break;
        case ExtractionConstants.WRAPPER_EXTRACTION_FAILED_CRC:
            if (controller.getArchiv().getCrcError().size() != 0) {
                for (DownloadLink link : controller.getArchiv().getCrcError()) {
                    if (link == null) continue;
                    link.getLinkStatus().removeStatus(LinkStatus.FINISHED);
                    link.getLinkStatus().removeStatus(LinkStatus.ERROR_ALREADYEXISTS);
                    link.getLinkStatus().addStatus(LinkStatus.ERROR_DOWNLOAD_FAILED);
                    link.getLinkStatus().setValue(LinkStatus.VALUE_FAILED_HASH);
                    link.getLinkStatus().setErrorMessage(JDL.LF("plugins.optional.extraction.crcerrorin", "Extract: failed (CRC in %s)", link.getName()));
                    link.requestGuiUpdate();
                }
            } else {
                for (DownloadLink link : controller.getArchiv().getDownloadLinks()) {
                    if (link == null) continue;
                    link.getLinkStatus().setErrorMessage(JDL.L("plugins.optional.extraction.error.extrfailedcrc", "Extract: failed (CRC in unknown file)"));
                    link.requestGuiUpdate();
                }
            }

            for (File f : controller.getArchiv().getExtractedFiles()) {
                if (f.exists()) {
                    if (!f.delete()) {
                        logger.warning("Could not delete file " + f.getAbsolutePath());
                    }
                }
            }

            controller.getArchiv().setActive(false);
            ex.onFinished(controller);
            break;
        case ExtractionConstants.WRAPPER_FINISHED_SUCCESSFULL:
            File[] files = new File[controller.getArchiv().getExtractedFiles().size()];
            int i = 0;
            for (File f : controller.getArchiv().getExtractedFiles()) {
                files[i++] = f;
            }
            JDUtilities.getController().fireControlEvent(new ControlEvent(controller, ControlEvent.CONTROL_ON_FILEOUTPUT, files));

            for (DownloadLink link : controller.getArchiv().getDownloadLinks()) {
                if (link == null) continue;
                link.getLinkStatus().addStatus(LinkStatus.FINISHED);
                link.getLinkStatus().removeStatus(LinkStatus.ERROR_POST_PROCESS);
                link.getLinkStatus().setStatusText(JDL.L("plugins.optional.extraction.status.extractok", "Extract OK"));
                link.requestGuiUpdate();
            }

            controller.getArchiv().setActive(false);
            ex.onFinished(controller);
            break;
        case ExtractionConstants.NOT_ENOUGH_SPACE:
            for (DownloadLink link : controller.getArchiv().getDownloadLinks()) {
                if (link == null) continue;

                link.getLinkStatus().setStatus(LinkStatus.FINISHED);
                link.getLinkStatus().setStatusText(JDL.L("plugins.optional.extraction.status.notenoughspace", "Not enough space to extract"));
                link.requestGuiUpdate();
            }

            ex.onFinished(controller);
            break;
        case ExtractionConstants.REMOVE_ARCHIVE_METADATA:
            ex.removeArchive(controller.getArchiv());
            break;
        case ExtractionConstants.WRAPPER_FILE_NOT_FOUND:
            if (controller.getArchiv().getCrcError().size() != 0) {
                for (DownloadLink link : controller.getArchiv().getCrcError()) {
                    if (link == null) continue;
                    link.getLinkStatus().removeStatus(LinkStatus.FINISHED);
                    link.getLinkStatus().removeStatus(LinkStatus.ERROR_ALREADYEXISTS);
                    link.getLinkStatus().addStatus(LinkStatus.ERROR_DOWNLOAD_FAILED);
                    link.getLinkStatus().setErrorMessage(JDL.L("plugins.optional.extraction.filenotfound", "Extract: failed (File not found)"));
                    link.requestGuiUpdate();
                }
            } else {
                for (DownloadLink link : controller.getArchiv().getDownloadLinks()) {
                    if (link == null) continue;
                    link.getLinkStatus().setErrorMessage(JDL.L("plugins.optional.extraction.filenotfound", "Extract: failed (File not found)"));
                    link.requestGuiUpdate();
                }
            }

            controller.getArchiv().setActive(false);
            ex.onFinished(controller);
            break;
        }
    }
}
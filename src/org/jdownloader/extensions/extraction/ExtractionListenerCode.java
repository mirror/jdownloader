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

import java.io.File;
import java.util.logging.Logger;

import jd.gui.UserIO;

import org.jdownloader.controlling.FileCreationEvent;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.extensions.extraction.translate.T;

/**
 * Updates the Extractionprogess for archives, where the download is not in the
 * downloadlist.
 * 
 * @author botzi
 * 
 */
public class ExtractionListenerCode implements ExtractionListener {
    private Logger              logger;
    private ExtractionExtension ex;

    ExtractionListenerCode() {
        this.ex = ExtractionExtension.getIntance();
    }

    public void onExtractionEvent(ExtractionEvent event) {
        ExtractionController controller = event.getCaller();

        switch (event.getType()) {
        case EXTRACTION_FAILED:
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
        case PASSWORD_NEEDED_TO_CONTINUE:
            if (ex.getSettings().isAskForUnknownPasswordsEnabled()) {
                String pass = UserIO.getInstance().requestInputDialog(0, T._.plugins_optional_extraction_askForPassword(controller.getArchiv().getFirstArchiveFile().getName()), "");
                if (pass == null || pass.length() == 0) {
                    ex.onFinished(controller);
                    break;
                }
                controller.getArchiv().setPassword(pass);
            }

            break;
        case OPEN_ARCHIVE_SUCCESS:
            ex.assignRealDownloadDir(controller);
            break;
        case EXTRACTION_FAILED_CRC:
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
        case FINISHED:
            File[] files = new File[controller.getArchiv().getExtractedFiles().size()];
            int i = 0;
            for (File f : controller.getArchiv().getExtractedFiles()) {
                files[i++] = f;
            }
            FileCreationManager.getInstance().getEventSender().fireEvent(new FileCreationEvent(controller, FileCreationEvent.Type.NEW_FILES, files));

            if (ex.getSettings().isDeleteInfoFilesAfterExtraction()) {
                File fileOutput = new File(controller.getArchiv().getFirstArchiveFile().getFilePath());
                File infoFiles = new File(fileOutput.getParentFile(), fileOutput.getName().replaceFirst("(?i)(\\.pa?r?t?\\.?[0-9]+\\.rar|\\.rar)$", "") + ".info");
                if (infoFiles.exists() && infoFiles.delete()) {
                    logger.info(infoFiles.getName() + " removed");
                }
            }
            controller.getArchiv().setActive(false);
            ex.onFinished(controller);
            break;
        case NOT_ENOUGH_SPACE:
            for (ArchiveFile link : controller.getArchiv().getArchiveFiles()) {
                if (link == null) continue;
                link.setStatus(ArchiveFile.Status.ERROR_NOT_ENOUGH_SPACE);

            }

            ex.onFinished(controller);
            break;
        case CLEANUP:
            ex.removeArchive(controller.getArchiv());
            break;
        case FILE_NOT_FOUND:
            controller.getArchiv().setActive(false);
            ex.onFinished(controller);
            break;
        }
    }
}
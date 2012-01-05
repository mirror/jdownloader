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
import java.util.HashSet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import jd.controlling.IOEQ;
import jd.controlling.JDLogger;
import jd.controlling.PasswordListController;
import jd.controlling.downloadcontroller.DownloadWatchDog;

import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.extensions.extraction.ExtractionEvent.Type;

/**
 * Responsible for the correct procedure of the extraction process. Contains one
 * IExtraction instance.
 * 
 * @author botzi
 * 
 */
public class ExtractionController extends QueueAction<Void, RuntimeException> {
    private HashSet<String>    passwordList;
    private int                passwordListSize = 0;
    private Exception          exception;
    private boolean            removeAfterExtraction;
    private Archive            archive;
    private IExtraction        extractor;
    private Logger             logger;
    private ScheduledFuture<?> timer;
    private Type               latestEvent;

    ExtractionController(Archive archiv, Logger logger) {
        this.archive = archiv;

        extractor = archive.getExtractor();
        extractor.setArchiv(archiv);
        extractor.setExtractionController(this);

        this.logger = logger;
        extractor.setLogger(logger);
        passwordList = new HashSet<String>();
    }

    public ExtractionQueue getExtractionQueue() {
        return (ExtractionQueue) super.getQueue();
    }

    public void kill() {
        super.kill();
        extractor.close();

    }

    @Override
    protected boolean allowAsync() {
        return true;
    }

    /**
     * Checks if the extracted file(s) has enough space. Only works with Java 6
     * or higher.
     * 
     * @return True if it's enough space.
     */
    private boolean checkSize() {
        return DownloadWatchDog.getInstance().checkFreeDiskSpace(archive.getExtractTo(), archive.getSize());
    }

    private boolean checkPassword(String pw) {
        if (pw == null || pw.equals("")) return false;

        fireEvent(ExtractionEvent.Type.PASSWORT_CRACKING);

        return extractor.findPassword(pw);
    }

    private void fireEvent(ExtractionEvent.Type event) {
        latestEvent = event;
        ExtractionExtension.getIntance().fireEvent(new ExtractionEvent(this, event));
    }

    public Type getLatestEvent() {
        return latestEvent;
    }

    @Override
    public Void run() {
        try {
            fireEvent(ExtractionEvent.Type.START);

            logger.info("Start unpacking of " + archive.getFirstArchiveFile().getFilePath());

            for (ArchiveFile l : archive.getArchiveFiles()) {
                if (!new File(l.getFilePath()).exists()) {
                    logger.info("Could not find archive file " + l.getFilePath());
                    archive.addCrcError(l);
                }
            }
            if (archive.getCrcError().size() > 0) {
                fireEvent(ExtractionEvent.Type.FILE_NOT_FOUND);
                return null;
            }

            File dl = ExtractionExtension.getIntance().getExtractToPath(archive.getFactory(), archive);
            archive.setExtractTo(dl);

            if (extractor.prepare()) {
                if (!checkSize()) {
                    fireEvent(ExtractionEvent.Type.NOT_ENOUGH_SPACE);
                    logger.info("Not enough harddisk space for unpacking archive " + archive.getFirstArchiveFile().getFilePath());
                    extractor.close();
                    return null;
                }

                if (archive.isProtected() && archive.getPassword().equals("")) {
                    passwordList.addAll(archive.getFactory().getPasswordList(archive));
                    passwordList.addAll(PasswordListController.getInstance().getPasswordList());
                    passwordList.add(archive.getName());

                    passwordListSize = passwordList.size() + PasswordListController.getInstance().getPasswordList().size() + 2;

                    fireEvent(ExtractionEvent.Type.START_CRACK_PASSWORD);
                    logger.info("Start password finding for " + archive);

                    for (String password : passwordList) {
                        if (checkPassword(password)) {
                            break;
                        }
                    }

                    if (archive.getPassword().equals("")) {
                        for (String password : PasswordListController.getInstance().getPasswordList()) {
                            if (checkPassword(password)) {
                                break;
                            }
                        }

                        if (archive.getPassword().equals("")) {
                            passwordList.clear();
                            //
                            passwordList.add(archive.getName());
                            passwordList.addAll(archive.getFactory().getPasswordList(archive));
                            for (String password : passwordList) {
                                if (checkPassword(password)) {
                                    break;
                                }
                            }
                        }
                    }

                    if (archive.getPassword().equals("")) {
                        fireEvent(ExtractionEvent.Type.PASSWORD_NEEDED_TO_CONTINUE);
                        logger.info("Found no password in passwordlist " + archive);

                        if (!checkPassword(archive.getPassword())) {
                            fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                            logger.info("No password found for " + archive);
                            extractor.close();
                            return null;
                        }
                        PasswordListController.getInstance().addPassword(archive.getPassword(), true);
                    }

                    fireEvent(ExtractionEvent.Type.PASSWORD_FOUND);
                    logger.info("Found password for " + archive);
                }
                fireEvent(ExtractionEvent.Type.OPEN_ARCHIVE_SUCCESS);

                if (!archive.getExtractTo().exists()) {
                    if (!archive.getExtractTo().mkdirs()) {
                        JDLogger.getLogger().warning("Could not create subpath");
                        fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                    }
                }

                logger.info("Execute unpacking of " + archive);

                timer = IOEQ.TIMINGQUEUE.scheduleWithFixedDelay(new Runnable() {
                    public void run() {
                        fireEvent(ExtractionEvent.Type.EXTRACTING);
                    }

                }, 1, 2, TimeUnit.SECONDS);
                try {
                    extractor.extract();
                } finally {
                    timer.cancel(false);
                }
                extractor.close();
                switch (archive.getExitCode()) {
                case ExtractionControllerConstants.EXIT_CODE_SUCCESS:
                    logger.info("Unpacking successful for " + archive);
                    if (!archive.getGotInterrupted() && removeAfterExtraction) {
                        removeArchiveFiles();
                    }
                    fireEvent(ExtractionEvent.Type.FINISHED);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_CRC_ERROR:
                    JDLogger.getLogger().warning("A CRC error occurred when unpacking " + archive);
                    fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED_CRC);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_USER_BREAK:
                    JDLogger.getLogger().info("User interrupted unpacking of " + archive);
                    fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_CREATE_ERROR:
                    JDLogger.getLogger().warning("Could not create Outputfile for" + archive);
                    fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_WRITE_ERROR:
                    JDLogger.getLogger().warning("Unable to write unpacked data on harddisk for " + archive);
                    this.exception = new ExtractionException("Write to disk error");
                    fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR:
                    JDLogger.getLogger().warning("A unknown fatal error occurred while unpacking " + archive);
                    fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_WARNING:
                    JDLogger.getLogger().warning("Non fatal error(s) occurred while unpacking " + archive);
                    fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                    break;
                default:
                    fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                    break;
                }
                return null;
            } else {
                extractor.close();
                fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
            }
        } catch (Exception e) {
            extractor.close();
            this.exception = e;
            JDLogger.exception(e);
            fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
        } finally {
            fireEvent(ExtractionEvent.Type.CLEANUP);
        }
        return null;
    }

    /**
     * Deletes the archive files.
     */
    private void removeArchiveFiles() {
        for (ArchiveFile link : archive.getArchiveFiles()) {
            if (!link.delete()) {
                JDLogger.getLogger().warning("Could not delete archive: " + link);
            }
        }
    }

    /**
     * Returns a thrown exception.
     * 
     * @return The thrown exception.
     */
    Exception getException() {
        return exception;
    }

    /**
     * 
     * Returns the current password finding process.
     * 
     * @return
     */
    public int getCrackProgress() {
        return extractor.getCrackProgress();
    }

    /**
     * Gets the passwordlist size
     * 
     * @return
     */
    public int getPasswordListSize() {
        return passwordListSize;
    }

    /**
     * Should the archives be deleted after extracting.
     * 
     * @param setProperty
     */
    void setRemoveAfterExtract(boolean setProperty) {
        this.removeAfterExtraction = setProperty;
    }

    /**
     * Starts the extracting progress.
     */
    public void go() throws Exception {
        run();
    }

    /**
     * Returns the {@link Archive}.
     * 
     * @return
     */
    public Archive getArchiv() {
        return archive;
    }

    /**
     * Sets a exception that occurs during unpacking.
     * 
     * @param e
     */
    public void setExeption(Exception e) {
        exception = e;
    }
}
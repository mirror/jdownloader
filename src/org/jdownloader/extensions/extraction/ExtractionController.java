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
import java.util.ArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import jd.controlling.IOEQ;
import jd.controlling.JDLogger;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.DownloadWatchDog.DISKSPACECHECK;

import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging.Log;
import org.jdownloader.controlling.FileCreationEvent;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.extensions.extraction.ExtractionEvent.Type;

/**
 * Responsible for the correct procedure of the extraction process. Contains one IExtraction instance.
 * 
 * @author botzi
 * 
 */
public class ExtractionController extends QueueAction<Void, RuntimeException> {
    private ArrayList<String>  passwordList;
    private int                passwordListSize = 0;
    private Exception          exception;
    private boolean            removeAfterExtraction;
    private Archive            archive;
    private IExtraction        extractor;
    private Logger             logger;
    private ScheduledFuture<?> timer;
    private Type               latestEvent;
    private double             progress;
    private boolean            removeDownloadLinksAfterExtraction;

    ExtractionController(Archive archiv, Logger logger) {
        this.archive = archiv;

        extractor = archive.getExtractor();
        extractor.setArchiv(archiv);
        extractor.setExtractionController(this);

        this.logger = logger;
        extractor.setLogger(logger);
        passwordList = new ArrayList<String>();
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

    private boolean checkPassword(String pw) {
        Log.L.info("Check Password: " + pw);
        if (pw == null || "".equals(pw)) return false;

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
                DISKSPACECHECK check = DownloadWatchDog.getInstance().checkFreeDiskSpace(archive.getExtractTo(), archive.getSize());
                if (DISKSPACECHECK.FAILED.equals(check) || DISKSPACECHECK.INVALIDFOLDER.equals(check)) {
                    fireEvent(ExtractionEvent.Type.NOT_ENOUGH_SPACE);
                    logger.info("Not enough harddisk space for unpacking archive " + archive.getFirstArchiveFile().getFilePath());
                    return null;
                }

                if (archive.isProtected() && archive.getPassword().equals("")) {

                    passwordList.addAll(archive.getFactory().getPasswordList(archive));
                    ArrayList<String> pwList = extractor.config.getPasswordList();
                    if (pwList == null) pwList = new ArrayList<String>();
                    passwordList.addAll(pwList);
                    passwordList.add(archive.getName());

                    passwordListSize = passwordList.size() + 2;

                    fireEvent(ExtractionEvent.Type.START_CRACK_PASSWORD);
                    logger.info("Start password finding for " + archive);

                    String correctPW = null;
                    for (String password : passwordList) {
                        if (checkPassword(password)) {
                            correctPW = password;
                            break;
                        }
                    }

                    if (correctPW == null) {
                        /* no correctPW found */

                        passwordList.clear();
                        //
                        passwordList.add(archive.getName());
                        passwordList.addAll(archive.getFactory().getPasswordList(archive));
                        for (String password : passwordList) {
                            if (checkPassword(password)) {
                                correctPW = password;
                                break;
                            }
                        }
                    }

                    if (correctPW == null) {
                        fireEvent(ExtractionEvent.Type.PASSWORD_NEEDED_TO_CONTINUE);
                        logger.info("Found no password in passwordlist " + archive);

                        if (!checkPassword(archive.getPassword())) {
                            fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                            logger.info("No password found for " + archive);
                            return null;
                        }
                        /* avoid duplicates */
                        pwList.remove(archive.getPassword());
                        pwList.add(0, archive.getPassword());
                        extractor.config.setPasswordList(pwList);
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
                    extractor.extract(this);
                } finally {
                    fireEvent(ExtractionEvent.Type.EXTRACTING);
                    timer.cancel(false);
                }
                extractor.close();
                switch (archive.getExitCode()) {
                case ExtractionControllerConstants.EXIT_CODE_SUCCESS:
                    logger.info("Unpacking successful for " + archive);
                    if (!archive.getGotInterrupted()) {
                        removeArchiveFiles();
                    }
                    fireEvent(ExtractionEvent.Type.FINISHED);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_INCOMPLETE_ERROR:
                    JDLogger.getLogger().warning("Archive seems to be incomplete " + archive);
                    fireEvent(ExtractionEvent.Type.FILE_NOT_FOUND);
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
                fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
            }
        } catch (Exception e) {
            this.exception = e;
            JDLogger.exception(e);
            fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
        } finally {
            try {
                extractor.close();
            } catch (final Throwable e) {
            }
            fireEvent(ExtractionEvent.Type.CLEANUP);
        }
        return null;
    }

    /**
     * Deletes the archive files.
     */
    private void removeArchiveFiles() {
        ArrayList<File> removed = new ArrayList<File>();
        for (ArchiveFile link : archive.getArchiveFiles()) {
            if (removeAfterExtraction) {
                if (!link.deleteFile()) {
                    JDLogger.getLogger().warning("Could not delete archive: " + link);
                } else {
                    removed.add(new File(link.getFilePath()));
                }
            }
            if (isRemoveDownloadLinksAfterExtraction()) {
                link.deleteLink();
            }
        }
        if (removed.size() > 0) FileCreationManager.getInstance().getEventSender().fireEvent(new FileCreationEvent(this, FileCreationEvent.Type.REMOVE_FILES, removed.toArray(new File[removed.size()])));
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

    /**
     * Sets the extraction progress in %
     * 
     * @param d
     */
    public void setProgress(double d) {
        this.progress = d;

    }

    /**
     * Get the extraction progress in %
     * 
     * @return
     */
    public double getProgress() {
        return progress;
    }

    public void setRemoveDownloadLinksAfterExtraction(boolean deleteArchiveDownloadlinksAfterExtraction) {
        this.removeDownloadLinksAfterExtraction = deleteArchiveDownloadlinksAfterExtraction;
    }

    public boolean isRemoveDownloadLinksAfterExtraction() {
        return removeDownloadLinksAfterExtraction;
    }
}
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
import java.util.HashSet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import jd.controlling.IOEQ;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.DownloadWatchDog.DISKSPACECHECK;

import org.appwork.utils.StringUtils;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.controlling.FileCreationEvent;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.extensions.extraction.ExtractionEvent.Type;
import org.jdownloader.logging.LogController;

/**
 * Responsible for the correct procedure of the extraction process. Contains one IExtraction instance.
 * 
 * @author botzi
 * 
 */
public class ExtractionController extends QueueAction<Void, RuntimeException> {
    private java.util.List<String> passwordList;
    private int                    passwordListSize = 0;
    private Exception              exception;
    private boolean                removeAfterExtraction;
    private Archive                archive;
    private IExtraction            extractor;
    private ScheduledFuture<?>     timer;
    private Type                   latestEvent;
    private double                 progress;
    private boolean                removeDownloadLinksAfterExtraction;
    private ExtractionExtension    extension;
    private final LogSource        logger;
    private FileSignatures         fileSignatures   = null;
    private boolean                overwriteFiles;
    private File                   extractToFolder;
    private boolean                successful       = false;

    public boolean isSuccessful() {
        return successful;
    }

    public File getExtractToFolder() {
        return extractToFolder;
    }

    public FileSignatures getFileSignatures() {
        if (fileSignatures == null) fileSignatures = new FileSignatures();
        return fileSignatures;
    }

    ExtractionController(ExtractionExtension extractionExtension, Archive archiv) {
        this.archive = archiv;
        logger = LogController.CL(false);
        logger.setAllowTimeoutFlush(false);
        logger.info("Extraction of" + archive);
        extractor = archive.getExtractor();
        extractor.setArchiv(archiv);
        extractor.setExtractionController(this);
        extension = extractionExtension;
        extractor.setLogger(logger);
        passwordList = new ArrayList<String>();
        archive.onControllerAssigned(this);
    }

    public ExtractionQueue getExtractionQueue() {
        return (ExtractionQueue) super.getQueue();
    }

    public void kill() {
        logger.info("abort extraction");
        logger.flush();
        super.kill();
    }

    public LogSource getLogger() {
        return logger;
    }

    @Override
    protected boolean allowAsync() {
        return true;
    }

    private boolean checkPassword(String pw, boolean optimized) throws ExtractionException {
        logger.info("Check Password: " + pw);
        if (pw == null || "".equals(pw)) return false;

        fireEvent(ExtractionEvent.Type.PASSWORT_CRACKING);
        return extractor.findPassword(this, pw, optimized);
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
            archive.onStartExtracting();
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

            if (gotKilled()) return null;
            if (extractor.prepare()) {
                extractToFolder = extension.getFinalExtractToFolder(archive);
                if (archive.isProtected()) {
                    if (!StringUtils.isEmpty(archive.getFinalPassword()) && !checkPassword(archive.getFinalPassword(), false)) {
                        /* open archive with found pw */
                        logger.info("Password " + archive.getFinalPassword() + " is invalid, try to find correct one");
                        archive.setFinalPassword(null);
                    }
                    if (StringUtils.isEmpty(archive.getFinalPassword())) {
                        /* pw unknown yet */
                        HashSet<String> spwList = archive.getSettings().getPasswords();
                        if (spwList != null) {
                            passwordList.addAll(spwList);
                        }
                        passwordList.addAll(archive.getFactory().getGuessedPasswordList(archive));
                        passwordList.add(archive.getName());
                        java.util.List<String> pwList = extractor.config.getPasswordList();
                        if (pwList == null) pwList = new ArrayList<String>();
                        passwordList.addAll(pwList);

                        passwordListSize = passwordList.size() + 2;

                        fireEvent(ExtractionEvent.Type.START_CRACK_PASSWORD);
                        logger.info("Start password finding for " + archive);

                        String correctPW = null;

                        for (String password : passwordList) {
                            if (gotKilled()) return null;
                            if (checkPassword(password, extension.getSettings().isPasswordFindOptimizationEnabled())) {
                                correctPW = password;
                                break;
                            }
                        }

                        if (correctPW == null) {
                            fireEvent(ExtractionEvent.Type.PASSWORD_NEEDED_TO_CONTINUE);
                            logger.info("Found no password in passwordlist " + archive);
                            if (gotKilled()) return null;
                            if (!checkPassword(archive.getFinalPassword(), false)) {
                                fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                                logger.info("No password found for " + archive);
                                return null;
                            }
                        }

                        fireEvent(ExtractionEvent.Type.PASSWORD_FOUND);
                        logger.info("Found password for " + archive + "->" + archive.getFinalPassword());
                        /* avoid duplicates */
                        pwList.remove(archive.getFinalPassword());
                        pwList.add(0, archive.getFinalPassword());
                        extractor.config.setPasswordList(pwList);
                    }
                }
                DISKSPACECHECK check = DownloadWatchDog.getInstance().checkFreeDiskSpace(getExtractToFolder(), archive.getContentView().getTotalSize());
                if (DISKSPACECHECK.FAILED.equals(check) || DISKSPACECHECK.INVALIDFOLDER.equals(check)) {
                    fireEvent(ExtractionEvent.Type.NOT_ENOUGH_SPACE);
                    logger.info("Not enough harddisk space for unpacking archive " + archive.getFirstArchiveFile().getFilePath());
                    return null;
                }

                fireEvent(ExtractionEvent.Type.OPEN_ARCHIVE_SUCCESS);

                if (!getExtractToFolder().exists()) {
                    if (!getExtractToFolder().mkdirs()) {
                        logger.warning("Could not create subpath");
                        fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                    }
                }

                logger.info("Execute unpacking of " + archive);
                logger.info("Extract to " + getExtractToFolder());
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
                    extractor.close();
                }
                if (gotKilled()) { return null; }
                if (extractor.getException() != null) exception = extractor.getException();
                switch (archive.getExitCode()) {
                case ExtractionControllerConstants.EXIT_CODE_SUCCESS:
                    logger.info("Unpacking successful for " + archive);
                    if (!archive.getGotInterrupted()) {
                        removeArchiveFiles();
                    }
                    archive.getSettings().setExtractionInfo(new ExtractionInfo(getExtractToFolder(), archive));
                    successful = true;
                    fireEvent(ExtractionEvent.Type.FINISHED);
                    logger.clear();
                    break;
                case ExtractionControllerConstants.EXIT_CODE_INCOMPLETE_ERROR:
                    logger.warning("Archive seems to be incomplete " + archive);
                    fireEvent(ExtractionEvent.Type.FILE_NOT_FOUND);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_CRC_ERROR:
                    logger.warning("A CRC error occurred when unpacking " + archive);
                    fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED_CRC);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_USER_BREAK:
                    logger.info("User interrupted unpacking of " + archive);
                    fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_CREATE_ERROR:
                    logger.warning("Could not create Outputfile for" + archive);
                    fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_WRITE_ERROR:
                    logger.warning("Unable to write unpacked data on harddisk for " + archive);
                    this.exception = new ExtractionException("Write to disk error");
                    fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR:
                    logger.warning("A unknown fatal error occurred while unpacking " + archive);
                    fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_WARNING:
                    logger.warning("Non fatal error(s) occurred while unpacking " + archive);
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
            logger.log(e);
            this.exception = e;
            fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
        } finally {
            try {
                if (gotKilled()) {
                    logger.info("ExtractionController has been killed");
                    logger.clear();
                }
                try {
                    extractor.close();
                } catch (final Throwable e) {
                }
                fireEvent(ExtractionEvent.Type.CLEANUP);
                archive.onCleanUp();
            } finally {
                logger.close();
            }
        }
        return null;
    }

    /**
     * Deletes the archive files.
     */
    private void removeArchiveFiles() {
        java.util.List<File> removed = new ArrayList<File>();
        for (ArchiveFile link : archive.getArchiveFiles()) {
            if (removeAfterExtraction) {
                if (!link.deleteFile()) {
                    logger.warning("Could not delete archive: " + link);
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
    public Exception getException() {
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

    public void setOverwriteFiles(boolean overwriteFiles) {
        this.overwriteFiles = overwriteFiles;
    }

    public boolean isOverwriteFiles() {
        return overwriteFiles;
    }

}
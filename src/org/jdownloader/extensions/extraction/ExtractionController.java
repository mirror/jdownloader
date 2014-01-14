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
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import jd.controlling.downloadcontroller.DiskSpaceManager.DISKSPACERESERVATIONRESULT;
import jd.controlling.downloadcontroller.DiskSpaceReservation;
import jd.controlling.downloadcontroller.DownloadWatchDog;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.JSonStorage;
import org.appwork.utils.Exceptions;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.extensions.extraction.ExtractionEvent.Type;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.IfFileExistsAction;

/**
 * Responsible for the correct procedure of the extraction process. Contains one IExtraction instance.
 * 
 * @author botzi
 * 
 */
public class ExtractionController extends QueueAction<Void, RuntimeException> {
    private List<String>                     passwordList;
    private Exception                        exception;
    private FileCreationManager.DeleteOption removeAfterExtractionAction;
    private Archive                          archive;
    private IExtraction                      extractor;
    private ScheduledFuture<?>               timer;
    private Type                             latestEvent;

    private AtomicLong                       completeBytes  = new AtomicLong(0);
    private AtomicLong                       processedBytes = new AtomicLong(0);

    public long getCompleteBytes() {
        return completeBytes.get();
    }

    public void setCompleteBytes(long completeBytes) {
        this.completeBytes.set(Math.max(0, completeBytes));
    }

    public long getProcessedBytes() {
        return processedBytes.get();
    }

    public void setProcessedBytes(long processedBytes) {
        this.processedBytes.set(Math.max(0, processedBytes));
    }

    public long addAndGetProcessedBytes(long processedBytes) {
        return this.processedBytes.addAndGet(Math.max(0, processedBytes));
    }

    private boolean              removeDownloadLinksAfterExtraction;
    private ExtractionExtension  extension;
    private final LogSource      logger;
    private FileSignatures       fileSignatures        = null;
    private IfFileExistsAction   ifFileExistsAction;
    private File                 extractToFolder;
    private boolean              successful            = false;
    private ExtractLogFileWriter crashLog;
    private boolean              askForUnknownPassword = false;
    private Item                 currentActiveItem;

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

    ExtractionController(ExtractionExtension extractionExtension, Archive archiv, IExtraction extractor) {
        this.archive = archiv;
        logger = LogController.CL(false);
        logger.setAllowTimeoutFlush(false);
        logger.info("Extraction of" + archive);
        this.extractor = extractor;
        extractor.setArchiv(archiv);
        extractor.setExtractionController(this);
        extension = extractionExtension;
        extractor.setLogger(logger);
        passwordList = new CopyOnWriteArrayList<String>();
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
        if (StringUtils.isEmpty(pw)) return false;

        fireEvent(ExtractionEvent.Type.PASSWORT_CRACKING);
        return extractor.findPassword(this, pw, optimized);
    }

    private void fireEvent(ExtractionEvent.Type event) {
        latestEvent = event;
        ExtractionExtension.getInstance().fireEvent(new ExtractionEvent(this, event));
    }

    public Type getLatestEvent() {
        return latestEvent;
    }

    @Override
    public Void run() {
        // let's write an info file. and delete if after extraction. this wy we have infosfiles if the extraction crashes jd
        crashLog = new ExtractLogFileWriter(archive.getName(), archive.getFirstArchiveFile().getFilePath(), archive.getFactory().getID()) {
            @Override
            public void write(String string) {
                super.write(string);
                logger.info(string);
            }
        };
        try {
            fireEvent(ExtractionEvent.Type.START);
            archive.onStartExtracting();
            crashLog.write("Date: " + new Date());
            crashLog.write("Start Extracting");
            crashLog.write("Extension Setup: \r\n" + extension.getSettings().toString());

            crashLog.write("Archive Setup: \r\n" + JSonStorage.toString(archive.getSettings()));
            extractor.setCrashLog(crashLog);
            logger.info("Start unpacking of " + archive.getFirstArchiveFile().getFilePath());

            for (ArchiveFile l : archive.getArchiveFiles()) {
                if (!new File(l.getFilePath()).exists()) {
                    crashLog.write("File missing: " + l.getFilePath());
                    logger.info("Could not find archive file " + l.getFilePath());
                    archive.addCrcError(l);
                }
            }
            if (archive.getCrcError().size() > 0) {
                fireEvent(ExtractionEvent.Type.FILE_NOT_FOUND);
                crashLog.write("Failed");
                return null;
            }

            if (gotKilled()) return null;
            crashLog.write("Prepare");
            if (extractor.prepare()) {
                extractToFolder = extension.getFinalExtractToFolder(archive);
                crashLog.write("Extract To: " + extractToFolder);
                if (archive.isProtected()) {
                    crashLog.write("Archive is Protected");
                    if (!StringUtils.isEmpty(archive.getFinalPassword()) && !checkPassword(archive.getFinalPassword(), false)) {
                        /* open archive with found pw */
                        logger.info("Password " + archive.getFinalPassword() + " is invalid, try to find correct one");
                        archive.setFinalPassword(null);
                    }
                    if (StringUtils.isEmpty(archive.getFinalPassword())) {
                        crashLog.write("Try to find password");
                        /* pw unknown yet */
                        List<String> spwList = archive.getSettings().getPasswords();
                        if (spwList != null) {
                            passwordList.addAll(spwList);
                        }
                        passwordList.addAll(archive.getFactory().getGuessedPasswordList(archive));
                        passwordList.add(archive.getName());
                        java.util.List<String> pwList = extractor.config.getPasswordList();
                        if (pwList == null) pwList = new ArrayList<String>();
                        passwordList.addAll(pwList);
                        fireEvent(ExtractionEvent.Type.START_CRACK_PASSWORD);
                        logger.info("Start password finding for " + archive);
                        String correctPW = null;
                        for (String password : passwordList) {
                            if (password == null) continue;
                            if (gotKilled()) return null;
                            crashLog.write("Try Password: " + password);
                            if (checkPassword(password, extension.getSettings().isPasswordFindOptimizationEnabled())) {
                                correctPW = password;
                                crashLog.write("Found password: \"" + password + "\"");
                                break;
                            } else {
                                // try trimmed password
                                String trimmed = password.trim();
                                if (trimmed.length() != password.length()) {
                                    password = trimmed;
                                    if (checkPassword(password, extension.getSettings().isPasswordFindOptimizationEnabled())) {
                                        correctPW = password;
                                        crashLog.write("Found password: \"" + password + "\"");
                                        break;
                                    }
                                }
                            }
                        }

                        if (correctPW == null) {
                            fireEvent(ExtractionEvent.Type.PASSWORD_NEEDED_TO_CONTINUE);
                            crashLog.write("Ask for password");
                            logger.info("Found no password in passwordlist " + archive);
                            if (gotKilled()) return null;
                            if (!checkPassword(archive.getFinalPassword(), false)) {
                                fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                                logger.info("No password found for " + archive);
                                crashLog.write("No password found or given");
                                crashLog.write("Failed");
                                return null;
                            }
                        }
                        fireEvent(ExtractionEvent.Type.PASSWORD_FOUND);
                        logger.info("Found password for " + archive + "->" + archive.getFinalPassword());

                    }
                    if (StringUtils.isNotEmpty(archive.getFinalPassword())) {
                        extension.addPassword(archive.getFinalPassword());
                    }
                }
                final DiskSpaceReservation extractReservation = new DiskSpaceReservation() {

                    @Override
                    public long getSize() {
                        final long completeSize = Math.max(getCompleteBytes(), archive.getContentView().getTotalSize());
                        long ret = completeSize - getProcessedBytes();
                        return ret;
                    }

                    @Override
                    public File getDestination() {
                        return getExtractToFolder();
                    }
                };
                DISKSPACERESERVATIONRESULT reservationResult = DownloadWatchDog.getInstance().getSession().getDiskSpaceManager().checkAndReserve(extractReservation, this);
                try {
                    switch (reservationResult) {
                    case FAILED:
                        logger.info("Not enough harddisk space for unpacking archive " + archive.getFirstArchiveFile().getFilePath());
                        crashLog.write("Diskspace Problem: " + reservationResult);
                        crashLog.write("Failed");
                        fireEvent(ExtractionEvent.Type.NOT_ENOUGH_SPACE);
                        return null;
                    case INVALIDDESTINATION:
                        logger.warning("Could use create subpath");
                        crashLog.write("Could use create subpath: " + getExtractToFolder());
                        crashLog.write("Failed");
                        fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                        return null;
                    }
                    fireEvent(ExtractionEvent.Type.OPEN_ARCHIVE_SUCCESS);
                    if (!getExtractToFolder().exists()) {
                        if (!FileCreationManager.getInstance().mkdir(getExtractToFolder())) {
                            logger.warning("Could not create subpath");
                            crashLog.write("Could not create subpath: " + getExtractToFolder());
                            crashLog.write("Failed");
                            fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                            return null;
                        }
                    }
                    logger.info("Execute unpacking of " + archive);
                    logger.info("Extract to " + getExtractToFolder());
                    crashLog.write("Use Password: " + archive.getFinalPassword());
                    ScheduledExecutorService scheduler = null;
                    try {
                        crashLog.write("Start Extracting " + extractor);
                        scheduler = DelayedRunnable.getNewScheduledExecutorService();
                        timer = scheduler.scheduleWithFixedDelay(new Runnable() {
                            public void run() {
                                fireEvent(ExtractionEvent.Type.EXTRACTING);
                            }

                        }, 1, 1, TimeUnit.SECONDS);
                        extractor.extract(this);
                    } finally {
                        crashLog.write("Extractor Returned");
                        if (timer != null) timer.cancel(false);
                        if (scheduler != null) scheduler.shutdown();
                        extractor.close();
                        if (extractor.getLastAccessedArchiveFile() != null) crashLog.write("Last used File: " + extractor.getLastAccessedArchiveFile());
                        fireEvent(ExtractionEvent.Type.EXTRACTING);
                    }
                } finally {
                    DownloadWatchDog.getInstance().getSession().getDiskSpaceManager().free(extractReservation, this);
                }
                if (gotKilled()) { return null; }
                if (extractor.getException() != null) {
                    exception = extractor.getException();
                    logger.log(exception);
                }
                if (exception != null) {
                    crashLog.write("Exception occured: \r\n" + Exceptions.getStackTrace(exception));
                }

                crashLog.write("ExitCode: " + archive.getExitCode());
                switch (archive.getExitCode()) {
                case ExtractionControllerConstants.EXIT_CODE_SUCCESS:
                    logger.info("Unpacking successful for " + archive + " interrupted: " + archive.getGotInterrupted());
                    archive.getSettings().setExtractionInfo(new ExtractionInfo(getExtractToFolder(), archive));
                    crashLog.write("Info: \r\n" + JSonStorage.serializeToJson(new ExtractionInfo(getExtractToFolder(), archive)));
                    crashLog.write("Successful");
                    successful = true;
                    fireEvent(ExtractionEvent.Type.FINISHED);
                    logger.clear();
                    break;
                case ExtractionControllerConstants.EXIT_CODE_INCOMPLETE_ERROR:
                    logger.warning("Archive seems to be incomplete " + archive);
                    crashLog.write("Incomplete Archive");
                    crashLog.write("Failed");
                    fireEvent(ExtractionEvent.Type.FILE_NOT_FOUND);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_CRC_ERROR:
                    logger.warning("A CRC error occurred when unpacking " + archive);
                    crashLog.write("CRC Error occured");
                    crashLog.write("Failed");
                    fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED_CRC);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_USER_BREAK:
                    logger.info("User interrupted unpacking of " + archive);
                    crashLog.write("Interrupted by User");
                    crashLog.write("Failed");
                    fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_CREATE_ERROR:
                    logger.warning("Could not create Outputfile for" + archive);
                    crashLog.write("Could not create Outputfile");
                    crashLog.write("Failed");
                    fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_WRITE_ERROR:
                    logger.warning("Unable to write unpacked data on harddisk for " + archive);
                    this.exception = new ExtractionException("Write to disk error");
                    crashLog.write("Harddisk write Error");
                    crashLog.write("Failed");
                    fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR:
                    logger.warning("A unknown fatal error occurred while unpacking " + archive);
                    crashLog.write("Unknown Fatal Error");
                    crashLog.write("Failed");
                    fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_WARNING:
                    logger.warning("Non fatal error(s) occurred while unpacking " + archive);
                    crashLog.write("Unknown Non Fatal Error");
                    crashLog.write("Failed");
                    fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                    break;
                default:
                    crashLog.write("Failed...unknown reason");
                    crashLog.write("Failed");
                    fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                    break;
                }
                return null;
            } else {
                crashLog.write("Failed");
                fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
            }
        } catch (Exception e) {
            logger.log(e);
            this.exception = e;
            crashLog.write("Exception occured: \r\n" + Exceptions.getStackTrace(e));
            crashLog.write("Failed");
            fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
        } finally {
            crashLog.close();
            if (!CFG_EXTRACTION.CFG.isWriteExtractionLogEnabled()) {
                crashLog.delete();
            }
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
    void removeArchiveFiles() {
        for (ArchiveFile link : archive.getArchiveFiles()) {

            link.deleteFile(removeAfterExtractionAction);

            if (isRemoveDownloadLinksAfterExtraction()) {
                link.deleteLink();
            }
        }
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
        return passwordList.size();
    }

    /**
     * Should the archives be deleted after extracting.
     * 
     * @param deleteOption
     */
    void setRemoveAfterExtract(FileCreationManager.DeleteOption deleteOption) {
        this.removeAfterExtractionAction = deleteOption;
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

    void setRemoveDownloadLinksAfterExtraction(boolean deleteArchiveDownloadlinksAfterExtraction) {
        this.removeDownloadLinksAfterExtraction = deleteArchiveDownloadlinksAfterExtraction;
    }

    public boolean isRemoveDownloadLinksAfterExtraction() {
        return removeDownloadLinksAfterExtraction;
    }

    public void setIfFileExistsAction(IfFileExistsAction ifExistsAction) {
        this.ifFileExistsAction = ifExistsAction;
    }

    public IfFileExistsAction getIfFileExistsAction() {
        return ifFileExistsAction;
    }

    /**
     * @return the askForUnknownPassword
     */
    public boolean isAskForUnknownPassword() {
        return askForUnknownPassword;
    }

    /**
     * @param askForUnknownPassword
     *            the askForUnknownPassword to set
     */
    public void setAskForUnknownPassword(boolean askForUnknownPassword) {
        this.askForUnknownPassword = askForUnknownPassword;
    }

    public void setCurrentActiveItem(Item item) {
        if (currentActiveItem == item) return;
        if (currentActiveItem != null && item != null && StringUtils.equals(currentActiveItem.getPath(), item.getPath())) return;
        this.currentActiveItem = item;
        fireEvent(ExtractionEvent.Type.ACTIVE_ITEM);
    }

    public Item getCurrentActiveItem() {
        return currentActiveItem;
    }

    public double getProgress() {
        double percent = (double) getProcessedBytes() * 100 / Math.max(1, getCompleteBytes());
        return percent;
    }

}
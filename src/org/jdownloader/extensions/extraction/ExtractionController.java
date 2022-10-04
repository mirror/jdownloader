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
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import jd.controlling.downloadcontroller.DiskSpaceManager.DISKSPACERESERVATIONRESULT;
import jd.controlling.downloadcontroller.DiskSpaceReservation;
import jd.controlling.downloadcontroller.DownloadSession;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.plugins.download.DownloadLinkDownloadable;
import jd.plugins.download.raf.FileBytesCache;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.controlling.FileCreationManager.DeleteOption;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.extensions.extraction.ExtractionEvent.Type;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFile;
import org.jdownloader.extensions.extraction.bindings.file.FileArchiveFile;
import org.jdownloader.extensions.extraction.multi.ArchiveType;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.IfFileExistsAction;

/**
 * Responsible for the correct procedure of the extraction process. Contains one IExtraction instance.
 *
 * @author botzi
 *
 */
public class ExtractionController extends QueueAction<Void, RuntimeException> implements GenericConfigEventListener<Enum> {
    private List<String>       passwordList;
    private Exception          exception;
    private final Archive      archive;
    private final IExtraction  extractor;
    private ScheduledFuture<?> timer;
    private Type               latestEvent;
    private final AtomicLong   completeBytes  = new AtomicLong(0);
    private final AtomicLong   processedBytes = new AtomicLong(0);
    private volatile IO_MODE   crcHashing     = IO_MODE.NORMAL;

    public boolean isSameArchive(Archive archive) {
        if (archive == null) {
            return false;
        } else if (getArchive() == archive) {
            return true;
        } else if (!StringUtils.equals(getArchive().getArchiveID(), archive.getArchiveID())) {
            return false;
        } else if (getArchive().getArchiveType() != archive.getArchiveType() || getArchive().getSplitType() != archive.getSplitType()) {
            return false;
        } else if (getArchive().getArchiveFiles().size() != archive.getArchiveFiles().size()) {
            return false;
        } else {
            final String thisFirstFilePath = getArchive().getArchiveFiles().get(0).getFilePath();
            final String otherFirstFilePath = archive.getArchiveFiles().get(0).getFilePath();
            return StringUtils.equals(thisFirstFilePath, otherFirstFilePath);
        }
    }

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

    public IO_MODE getIOModeForCrcHashing() {
        final IO_MODE mode = crcHashing;
        switch (mode) {
        case PAUSE:
        case THROTTLE:
            if (DownloadLinkDownloadable.isCrcHashingInProgress()) {
                return mode;
            }
        default:
        case NORMAL:
            return IO_MODE.NORMAL;
        }
    }

    public void addProcessedBytesAndPauseIfNeeded(long processedBytes) {
        try {
            if (!IO_MODE.NORMAL.equals(getIOModeForCrcHashing())) {
                crcHashing: while (true) {
                    switch (getIOModeForCrcHashing()) {
                    case PAUSE:
                        Thread.sleep(1000);
                        break;
                    case THROTTLE:
                        Thread.sleep(100);
                    default:
                    case NORMAL:
                        break crcHashing;
                    }
                }
            }
        } catch (InterruptedException e) {
            logger.log(e);
        }
        this.processedBytes.addAndGet(Math.max(0, processedBytes));
    }

    private final ExtractionExtension extension;
    private final LogSource           logger;
    private FileSignatures            fileSignatures        = null;
    private IfFileExistsAction        ifFileExistsAction;
    private File                      extractToFolder;
    private boolean                   successful            = false;
    private boolean                   askForUnknownPassword = false;
    private Item                      currentActiveItem;
    private final ExtractionProgress  extractionProgress;
    protected final FileBytesCache    fileBytesCache;
    private final UniqueAlltimeID     uniqueID              = new UniqueAlltimeID();

    public UniqueAlltimeID getUniqueID() {
        return uniqueID;
    }

    public FileBytesCache getFileBytesCache() {
        return fileBytesCache;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public File getExtractToFolder() {
        return extractToFolder;
    }

    public FileSignatures getFileSignatures() {
        if (fileSignatures == null) {
            fileSignatures = new FileSignatures();
        }
        return fileSignatures;
    }

    public ExtractionProgress getExtractionProgress() {
        return extractionProgress;
    }

    protected void postRun() {
        synchronized (extension) {
            final List<ExtractionController> removeList = new ArrayList<ExtractionController>();
            for (ExtractionController ec : getExtractionQueue().getJobs()) {
                if (ec.isSameArchive(archive)) {
                    removeList.add(ec);
                }
            }
            for (ExtractionController ec : removeList) {
                if (ec != this) {
                    getExtractionQueue().remove(ec);
                }
            }
        }
    };

    ExtractionController(ExtractionExtension extractionExtension, Archive archiv, IExtraction extractor) {
        this.archive = archiv;
        logger = LogController.CL(false);
        logger.setAllowTimeoutFlush(false);
        logger.info("Extraction of" + archive);
        this.extractor = extractor;
        extractionProgress = new ExtractionProgress(this, 0, 0, null);
        extractor.setExtractionController(this);
        extension = extractionExtension;
        extractor.setLogger(logger);
        passwordList = new CopyOnWriteArrayList<String>();
        archive.setExtractionController(this);
        fileBytesCache = DownloadSession.getDownloadWriteCache();
        CFG_EXTRACTION.IOMODE_CRCHASHING.getEventSender().addListener(this, true);
        crcHashing = (IO_MODE) CFG_EXTRACTION.IOMODE_CRCHASHING.getValue();
    }

    public ExtractionQueue getExtractionQueue() {
        return (ExtractionQueue) super.getQueue();
    }

    public void kill() {
        try {
            if (gotStarted()) {
                logger.info("abort extraction");
                logger.flush();
            } else {
                logger.close();
            }
        } finally {
            super.kill();
        }
    }

    public LogSource getLogger() {
        return logger;
    }

    @Override
    protected boolean allowAsync() {
        return true;
    }

    private boolean checkPassword(String pw, boolean optimized) throws ExtractionException {
        logger.info("Check Password: '" + pw + "'");
        if (StringUtils.isEmpty(pw)) {
            return false;
        } else {
            fireEvent(ExtractionEvent.Type.PASSWORT_CRACKING);
            return extractor.findPassword(this, pw, optimized);
        }
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
        // let's write an info file. and delete if after extraction. this why we have infosfiles if the extraction crashes jd
        final ArchiveFile firstArchiveFile = archive.getArchiveFiles().get(0);
        try {
            fireEvent(ExtractionEvent.Type.START);
            archive.onStartExtracting();
            logger.info("Date:" + new Date());
            logger.info("Start Extracting");
            logger.info("Archive Setup: \r\n" + JSonStorage.toString(archive.getSettings()));
            logger.info("Start unpacking of " + firstArchiveFile.getFilePath());
            for (final ArchiveFile archiveFile : archive.getArchiveFiles()) {
                if (!archiveFile.exists(true)) {
                    if (archiveFile instanceof DownloadLinkArchiveFile) {
                        final DownloadLinkArchiveFile downloadLinkArchiveFile = (DownloadLinkArchiveFile) archiveFile;
                        logger.info("Missing (DownloadLinkArchiveFile)File: " + archiveFile.getFilePath() + "|FileArchiveFileExists:" + downloadLinkArchiveFile.isFileArchiveFileExists());
                    } else if (archiveFile instanceof FileArchiveFile) {
                        logger.info("Missing (FileArchiveFile)File: " + archiveFile.getFilePath());
                    } else {
                        logger.info("Missing File: " + archiveFile.getFilePath());
                    }
                    logger.info("Could not find archive file " + archiveFile.getFilePath());
                    archive.addMissingFile(archiveFile);
                } else {
                    if (archiveFile instanceof DownloadLinkArchiveFile) {
                        final DownloadLinkArchiveFile downloadLinkArchiveFile = (DownloadLinkArchiveFile) archiveFile;
                        logger.info(" (DownloadLinkArchiveFile)File:" + archiveFile.getFilePath() + "|FileArchiveFileExists:" + downloadLinkArchiveFile.isFileArchiveFileExists() + "|FileSize:" + archiveFile.getFileSize());
                    } else if (archiveFile instanceof FileArchiveFile) {
                        logger.info(" (FileArchiveFile)File:" + archiveFile.getFilePath() + "|FileSize:" + archiveFile.getFileSize());
                    } else {
                        logger.info(" File:" + archiveFile.getFilePath() + "|FileSize:" + archiveFile.getFileSize());
                    }
                }
            }
            if (archive.getMissingFiles().size() > 0) {
                fireEvent(ExtractionEvent.Type.FILE_NOT_FOUND);
                logger.info("Failed");
                return null;
            }
            if (gotKilled()) {
                return null;
            }
            logger.info("Prepare - " + new Date());
            if (extractor.prepare()) {
                if (archive.isProtected()) {
                    final boolean isPasswordFindOptimizationEnabled = getExtension().getSettings().isPasswordFindOptimizationEnabled();
                    logger.info("Archive is Protected - " + new Date());
                    if (!StringUtils.isEmpty(archive.getFinalPassword()) && !checkPassword(archive.getFinalPassword(), false)) {
                        /* open archive with found pw */
                        logger.info("Password " + archive.getFinalPassword() + " is invalid, try to find correct one");
                        archive.setFinalPassword(null);
                    }
                    if (StringUtils.isEmpty(archive.getFinalPassword())) {
                        logger.info("Try to find password");
                        /* pw unknown yet */
                        List<String> spwList = archive.getSettings().getPasswords();
                        if (spwList != null) {
                            passwordList.addAll(spwList);
                        }
                        passwordList.addAll(archive.getFactory().getGuessedPasswordList(archive));
                        passwordList.add(archive.getName());
                        java.util.List<String> pwList = extractor.getConfig().getPasswordList();
                        if (pwList == null) {
                            pwList = new ArrayList<String>();
                        }
                        passwordList.addAll(pwList);
                        fireEvent(ExtractionEvent.Type.START_CRACK_PASSWORD);
                        logger.info("Start password finding for " + archive + " - " + new Date());
                        String correctPW = null;
                        for (final String password : passwordList) {
                            if (password == null) {
                                continue;
                            }
                            if (gotKilled()) {
                                return null;
                            }
                            if (checkPassword(password, isPasswordFindOptimizationEnabled)) {
                                correctPW = password;
                                logger.info("Found password: \"" + correctPW + "\"" + " - " + new Date());
                                break;
                            } else {
                                // try trimmed password
                                final String trimmed = password.trim();
                                if (trimmed.length() != password.length()) {
                                    if (checkPassword(trimmed, isPasswordFindOptimizationEnabled)) {
                                        correctPW = trimmed;
                                        logger.info("Found password: \"" + correctPW + "\"" + " - " + new Date());
                                        break;
                                    }
                                }
                            }
                        }
                        if (correctPW == null) {
                            fireEvent(ExtractionEvent.Type.PASSWORD_NEEDED_TO_CONTINUE);
                            logger.info("Ask for password");
                            logger.info("Found no password in passwordlist " + archive);
                            if (gotKilled()) {
                                return null;
                            }
                            if (!checkPassword(archive.getFinalPassword(), false)) {
                                fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                                logger.info("No password found for " + archive);
                                logger.info("Failed");
                                return null;
                            }
                        }
                        fireEvent(ExtractionEvent.Type.PASSWORD_FOUND);
                        logger.info("Found password for " + archive + "->" + archive.getFinalPassword() + " - " + new Date());
                    }
                    if (StringUtils.isNotEmpty(archive.getFinalPassword())) {
                        getExtension().addPassword(archive.getFinalPassword());
                    }
                }
                extractToFolder = getExtension().getFinalExtractToFolder(archive, false);
                logger.info("Extract To: " + extractToFolder + " - " + new Date());
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

                    @Override
                    public Object getOwner() {
                        return ExtractionController.this;
                    }

                    @Override
                    public LogInterface getLogger() {
                        return ExtractionController.this.getLogger();
                    }
                };
                DISKSPACERESERVATIONRESULT reservationResult = DownloadWatchDog.getInstance().getSession().getDiskSpaceManager().checkAndReserve(extractReservation, this);
                try {
                    switch (reservationResult) {
                    case FAILED:
                        fireEvent(ExtractionEvent.Type.NOT_ENOUGH_SPACE);
                        return null;
                    case INVALIDDESTINATION:
                        fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                        return null;
                    default:
                        break;
                    }
                    fireEvent(ExtractionEvent.Type.OPEN_ARCHIVE_SUCCESS);
                    if (!getExtractToFolder().exists()) {
                        if (!FileCreationManager.getInstance().mkdir(getExtractToFolder())) {
                            logger.info("Could not create subpath: " + getExtractToFolder());
                            logger.info("Failed");
                            fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                            return null;
                        }
                    }
                    logger.info("Execute unpacking of:" + archive);
                    logger.info("Extract to " + getExtractToFolder());
                    logger.info("Use Password: " + archive.getFinalPassword() + "|PW Protected:" + archive.isProtected() + ":" + archive.isPasswordRequiredToOpen());
                    ScheduledExecutorService scheduler = null;
                    try {
                        logger.info("Start Extracting " + extractor + " - " + new Date());
                        scheduler = DelayedRunnable.getNewScheduledExecutorService();
                        timer = scheduler.scheduleWithFixedDelay(new Runnable() {
                            public void run() {
                                fireEvent(ExtractionEvent.Type.EXTRACTING);
                            }
                        }, 1, 1, TimeUnit.SECONDS);
                        extractor.extract(this);
                    } finally {
                        extractor.close();
                        logger.info("Extractor Returned - " + new Date());
                        if (timer != null) {
                            timer.cancel(false);
                        }
                        if (scheduler != null) {
                            scheduler.shutdown();
                        }
                        if (extractor.getLastAccessedArchiveFile() != null) {
                            logger.info("Last used File: " + extractor.getLastAccessedArchiveFile());
                        }
                        fireEvent(ExtractionEvent.Type.EXTRACTING);
                    }
                } finally {
                    DownloadWatchDog.getInstance().getSession().getDiskSpaceManager().free(extractReservation, this);
                }
                if (gotKilled()) {
                    return null;
                }
                if (extractor.getException() != null) {
                    exception = extractor.getException();
                    logger.log(exception);
                }
                if (exception != null) {
                    logger.log(exception);
                }
                logger.info("ExitCode: " + archive.getExitCode());
                switch (archive.getExitCode()) {
                case ExtractionControllerConstants.EXIT_CODE_SUCCESS:
                    logger.info("Unpacking successful for " + archive);
                    archive.getSettings().setExtractionInfo(new ExtractionInfo(getExtractToFolder(), archive));
                    logger.info("Info: \r\n" + JSonStorage.serializeToJson(new ExtractionInfo(getExtractToFolder(), archive)));
                    logger.info("Successful");
                    successful = true;
                    fireEvent(ExtractionEvent.Type.FINISHED);
                    logger.clear();
                    break;
                case ExtractionControllerConstants.EXIT_CODE_INCOMPLETE_ERROR:
                    fireEvent(ExtractionEvent.Type.FILE_NOT_FOUND);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_CRC_ERROR:
                    fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED_CRC);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_USER_BREAK:
                    fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_CREATE_ERROR:
                    fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_WRITE_ERROR:
                    this.exception = new ExtractionException("Write to disk error");
                    fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR:
                    fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_WARNING:
                    fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                    break;
                default:
                    fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                    break;
                }
                return null;
            } else {
                logger.info("ExitCode(Prepare failed): " + archive.getExitCode());
                switch (archive.getExitCode()) {
                case ExtractionControllerConstants.EXIT_CODE_CRC_ERROR:
                    fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED_CRC);
                    break;
                default:
                    fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                    break;
                }
            }
        } catch (Exception e) {
            this.exception = e;
            logger.log(e);
            fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
        } finally {
            try {
                try {
                    extractor.close();
                } catch (final Throwable e) {
                }
                if (gotKilled()) {
                    logger.clear();
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
        final Archive archive = getArchive();
        final DeleteOption remove;
        if (archive.getFactory().isDeepExtraction()) {
            remove = DeleteOption.NULL;
        } else {
            remove = getExtension().getRemoveFilesAfterExtractAction(archive);
        }
        if (remove != null && !DeleteOption.NO_DELETE.equals(remove)) {
            for (final ArchiveFile link : archive.getArchiveFiles()) {
                link.deleteFile(remove);
            }
            if (ArchiveType.RAR_MULTI.equals(archive.getArchiveType())) {
                // Deleting rar recovery volumes
                final HashSet<String> done = new HashSet<String>();
                for (final ArchiveFile link : archive.getArchiveFiles()) {
                    if (done.add(link.getName())) {
                        final String filePath = link.getFilePath().replaceFirst("(?i)\\.rar$", ".rev");
                        final File file = new File(filePath);
                        if (file.exists() && file.isFile()) {
                            logger.info("Deleting rar recovery volume " + file.getAbsolutePath());
                            if (!file.delete()) {
                                logger.warning("Could not deleting rar recovery volume " + file.getAbsolutePath());
                            }
                        }
                    }
                }
            }
        }
    }

    public ExtractionExtension getExtension() {
        return extension;
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
    public Archive getArchive() {
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
        if (currentActiveItem != item) {
            if (currentActiveItem != null && item != null && StringUtils.equals(currentActiveItem.getPath(), item.getPath())) {
                return;
            }
            this.currentActiveItem = item;
            fireEvent(ExtractionEvent.Type.ACTIVE_ITEM);
        }
    }

    public Item getCurrentActiveItem() {
        return currentActiveItem;
    }

    public double getProgress() {
        double percent = (double) getProcessedBytes() * 100 / Math.max(1, getCompleteBytes());
        return percent;
    }

    @Override
    public void onConfigValidatorError(KeyHandler<Enum> keyHandler, Enum invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<Enum> keyHandler, Enum newValue) {
        if (keyHandler == CFG_EXTRACTION.IOMODE_CRCHASHING) {
            final IO_MODE newMode = (IO_MODE) newValue;
            if (newMode == null) {
                crcHashing = IO_MODE.NORMAL;
            } else {
                crcHashing = newMode;
            }
        }
    }
}
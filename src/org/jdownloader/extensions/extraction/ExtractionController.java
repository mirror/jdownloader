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
import jd.plugins.download.raf.FileBytesCache;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.JSonStorage;
import org.appwork.utils.Exceptions;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.queue.QueueAction;
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
public class ExtractionController extends QueueAction<Void, RuntimeException> {
    private List<String>       passwordList;
    private Exception          exception;
    private final Archive      archive;
    private final IExtraction  extractor;
    private ScheduledFuture<?> timer;
    private Type               latestEvent;

    private final AtomicLong   completeBytes  = new AtomicLong(0);
    private final AtomicLong   processedBytes = new AtomicLong(0);

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
                if (ec.getArchive() == archive || StringUtils.equals(ec.getArchive().getArchiveID(), archive.getArchiveID())) {
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
    }

    public ExtractionQueue getExtractionQueue() {
        return (ExtractionQueue) super.getQueue();
    }

    public void kill() {
        if (gotStarted()) {
            logger.info("abort extraction");
            logger.flush();
        } else {
            logger.close();
        }
        super.kill();
    }

    public LogSource getLogger() {
        return logger;
    }

    @Override
    protected boolean allowAsync() {
        return true;
    }

    private boolean checkPassword(ExtractLogFileWriter crashLog, String pw, boolean optimized) throws ExtractionException {
        crashLog.write("Check Password: '" + pw + "'");
        if (StringUtils.isEmpty(pw)) {
            return false;
        }
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
        final boolean deleteExtractionLog = !CFG_EXTRACTION.CFG.isWriteExtractionLogEnabled();
        // let's write an info file. and delete if after extraction. this why we have infosfiles if the extraction crashes jd
        final ArchiveFile firstArchiveFile = archive.getArchiveFiles().get(0);
        final ExtractLogFileWriter crashLog = new ExtractLogFileWriter(archive.getName(), firstArchiveFile.getFilePath(), archive.getArchiveID()) {
            @Override
            public void write(String string) {
                super.write(string);
                logger.info(string);
            }

            @Override
            protected boolean deleteOnShutDown() {
                return deleteExtractionLog;
            }
        };
        try {
            fireEvent(ExtractionEvent.Type.START);
            archive.onStartExtracting();
            crashLog.write("Date:" + new Date());
            crashLog.write("Start Extracting");
            crashLog.write("Extension Setup: \r\n" + extension.getSettings().toString());

            crashLog.write("Archive Setup: \r\n" + JSonStorage.toString(archive.getSettings()));
            extractor.setCrashLog(crashLog);
            crashLog.write("Start unpacking of " + firstArchiveFile.getFilePath());

            for (final ArchiveFile archiveFile : archive.getArchiveFiles()) {
                if (!archiveFile.exists()) {
                    if (archiveFile instanceof DownloadLinkArchiveFile) {
                        final DownloadLinkArchiveFile downloadLinkArchiveFile = (DownloadLinkArchiveFile) archiveFile;
                        crashLog.write("Missing (DownloadLinkArchiveFile)File: " + archiveFile.getFilePath() + "|FileArchiveFileExists:" + downloadLinkArchiveFile.isFileArchiveFileExists());
                    } else if (archiveFile instanceof FileArchiveFile) {
                        crashLog.write("Missing (FileArchiveFile)File: " + archiveFile.getFilePath());
                    } else {
                        crashLog.write("Missing File: " + archiveFile.getFilePath());
                    }
                    crashLog.write("Could not find archive file " + archiveFile.getFilePath());
                    archive.addCrcError(archiveFile);
                } else {
                    if (archiveFile instanceof DownloadLinkArchiveFile) {
                        final DownloadLinkArchiveFile downloadLinkArchiveFile = (DownloadLinkArchiveFile) archiveFile;
                        crashLog.write(" (DownloadLinkArchiveFile)File:" + archiveFile.getFilePath() + "|FileArchiveFileExists:" + downloadLinkArchiveFile.isFileArchiveFileExists() + "|FileSize:" + archiveFile.getFileSize());
                    } else if (archiveFile instanceof FileArchiveFile) {
                        crashLog.write(" (FileArchiveFile)File:" + archiveFile.getFilePath() + "|FileSize:" + archiveFile.getFileSize());
                    } else {
                        crashLog.write(" File:" + archiveFile.getFilePath() + "|FileSize:" + archiveFile.getFileSize());
                    }
                }
            }
            if (archive.getCrcError().size() > 0) {
                fireEvent(ExtractionEvent.Type.FILE_NOT_FOUND);
                crashLog.write("Failed");
                return null;
            }
            if (gotKilled()) {
                return null;
            }
            crashLog.write("Prepare");
            if (extractor.prepare()) {
                if (archive.isProtected()) {
                    final boolean isPasswordFindOptimizationEnabled = getExtension().getSettings().isPasswordFindOptimizationEnabled();
                    crashLog.write("Archive is Protected");
                    if (!StringUtils.isEmpty(archive.getFinalPassword()) && !checkPassword(crashLog, archive.getFinalPassword(), false)) {
                        /* open archive with found pw */
                        crashLog.write("Password " + archive.getFinalPassword() + " is invalid, try to find correct one");
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
                        java.util.List<String> pwList = extractor.getConfig().getPasswordList();
                        if (pwList == null) {
                            pwList = new ArrayList<String>();
                        }
                        passwordList.addAll(pwList);
                        fireEvent(ExtractionEvent.Type.START_CRACK_PASSWORD);
                        crashLog.write("Start password finding for " + archive);
                        String correctPW = null;
                        for (final String password : passwordList) {
                            if (password == null) {
                                continue;
                            }
                            if (gotKilled()) {
                                return null;
                            }
                            if (checkPassword(crashLog, password, isPasswordFindOptimizationEnabled)) {
                                correctPW = password;
                                crashLog.write("Found password: \"" + correctPW + "\"");
                                break;
                            } else {
                                // try trimmed password
                                final String trimmed = password.trim();
                                if (trimmed.length() != password.length()) {
                                    if (checkPassword(crashLog, trimmed, isPasswordFindOptimizationEnabled)) {
                                        correctPW = trimmed;
                                        crashLog.write("Found password: \"" + correctPW + "\"");
                                        break;
                                    }
                                }
                            }
                        }

                        if (correctPW == null) {
                            fireEvent(ExtractionEvent.Type.PASSWORD_NEEDED_TO_CONTINUE);
                            crashLog.write("Ask for password");
                            crashLog.write("Found no password in passwordlist " + archive);
                            if (gotKilled()) {
                                return null;
                            }
                            if (!checkPassword(crashLog, archive.getFinalPassword(), false)) {
                                fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                                crashLog.write("No password found for " + archive);
                                crashLog.write("Failed");
                                return null;
                            }
                        }
                        fireEvent(ExtractionEvent.Type.PASSWORD_FOUND);
                        crashLog.write("Found password for " + archive + "->" + archive.getFinalPassword());

                    }
                    if (StringUtils.isNotEmpty(archive.getFinalPassword())) {
                        getExtension().addPassword(archive.getFinalPassword());
                    }
                }
                extractToFolder = getExtension().getFinalExtractToFolder(archive, false);
                crashLog.write("Extract To: " + extractToFolder);

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
                        crashLog.write("Not enough harddisk space for unpacking archive " + firstArchiveFile.getFilePath());
                        crashLog.write("Diskspace Problem: " + reservationResult);
                        crashLog.write("Failed");
                        fireEvent(ExtractionEvent.Type.NOT_ENOUGH_SPACE);
                        return null;
                    case INVALIDDESTINATION:
                        crashLog.write("Could use create subpath");
                        crashLog.write("Could use create subpath: " + getExtractToFolder());
                        crashLog.write("Failed");
                        fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                        return null;
                    }
                    fireEvent(ExtractionEvent.Type.OPEN_ARCHIVE_SUCCESS);
                    if (!getExtractToFolder().exists()) {
                        if (!FileCreationManager.getInstance().mkdir(getExtractToFolder())) {
                            crashLog.write("Could not create subpath: " + getExtractToFolder());
                            crashLog.write("Failed");
                            fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                            return null;
                        }
                    }
                    crashLog.write("Execute unpacking of:" + archive);
                    crashLog.write("Extract to " + getExtractToFolder());
                    crashLog.write("Use Password: " + archive.getFinalPassword() + "|PW Protected:" + archive.isProtected() + ":" + archive.isPasswordRequiredToOpen());
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
                        extractor.close();
                        crashLog.write("Extractor Returned");
                        if (timer != null) {
                            timer.cancel(false);
                        }
                        if (scheduler != null) {
                            scheduler.shutdown();
                        }
                        if (extractor.getLastAccessedArchiveFile() != null) {
                            crashLog.write("Last used File: " + extractor.getLastAccessedArchiveFile());
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
                    crashLog.write("Exception occured: \r\n" + Exceptions.getStackTrace(exception));
                }

                crashLog.write("ExitCode: " + archive.getExitCode());
                switch (archive.getExitCode()) {
                case ExtractionControllerConstants.EXIT_CODE_SUCCESS:
                    crashLog.write("Unpacking successful for " + archive);
                    archive.getSettings().setExtractionInfo(new ExtractionInfo(getExtractToFolder(), archive));
                    crashLog.write("Info: \r\n" + JSonStorage.serializeToJson(new ExtractionInfo(getExtractToFolder(), archive)));
                    crashLog.write("Successful");
                    successful = true;
                    fireEvent(ExtractionEvent.Type.FINISHED);
                    logger.clear();
                    break;
                case ExtractionControllerConstants.EXIT_CODE_INCOMPLETE_ERROR:
                    crashLog.write("Archive seems to be incomplete " + archive);
                    crashLog.write("Incomplete Archive");
                    crashLog.write("Failed");
                    fireEvent(ExtractionEvent.Type.FILE_NOT_FOUND);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_CRC_ERROR:
                    crashLog.write("A CRC error occurred when unpacking " + archive);
                    crashLog.write("CRC Error occured");
                    crashLog.write("Failed");
                    fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED_CRC);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_USER_BREAK:
                    crashLog.write("User interrupted unpacking of " + archive);
                    crashLog.write("Interrupted by User");
                    crashLog.write("Failed");
                    fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_CREATE_ERROR:
                    crashLog.write("Could not create Outputfile for" + archive);
                    crashLog.write("Could not create Outputfile");
                    crashLog.write("Failed");
                    fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_WRITE_ERROR:
                    crashLog.write("Unable to write unpacked data on harddisk for " + archive);
                    this.exception = new ExtractionException("Write to disk error");
                    crashLog.write("Harddisk write Error");
                    crashLog.write("Failed");
                    fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR:
                    crashLog.write("A unknown fatal error occurred while unpacking " + archive);
                    crashLog.write("Unknown Fatal Error");
                    crashLog.write("Failed");
                    fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_WARNING:
                    crashLog.write("Non fatal error(s) occurred while unpacking " + archive);
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
                switch (archive.getExitCode()) {
                case ExtractionControllerConstants.EXIT_CODE_CRC_ERROR:
                    crashLog.write("A CRC error occurred when unpacking " + archive);
                    crashLog.write("CRC Error occured");
                    crashLog.write("Failed");
                    fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED_CRC);
                    break;
                default:
                    crashLog.write("Failed");
                    fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
                    break;
                }
            }
        } catch (Exception e) {
            logger.log(e);
            this.exception = e;
            crashLog.write("Exception occured: \r\n" + Exceptions.getStackTrace(e));
            crashLog.write("Failed");
            fireEvent(ExtractionEvent.Type.EXTRACTION_FAILED);
        } finally {
            try {
                try {
                    extractor.close();
                } catch (final Throwable e) {
                }
                crashLog.close();
                if (deleteExtractionLog) {
                    crashLog.delete();
                }
                if (gotKilled()) {
                    crashLog.write("ExtractionController has been killed");
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

}
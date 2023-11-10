package org.jdownloader.downloader.text;

import java.awt.Color;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.controlling.downloadcontroller.DiskSpaceReservation;
import jd.controlling.downloadcontroller.ExceptionRunnable;
import jd.controlling.downloadcontroller.FileIsLockedException;
import jd.controlling.downloadcontroller.ManagedThrottledConnectionHandler;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.DownloadLinkDownloadable;
import jd.plugins.download.Downloadable;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.IO;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.plugins.DownloadPluginProgress;
import org.jdownloader.plugins.SkipReason;
import org.jdownloader.plugins.SkipReasonException;
import org.jdownloader.settings.GeneralSettings;

/** Simple download class which can write text content into a file. */
public class TextDownloader extends DownloadInterface {
    protected volatile long                   bytesWritten      = 0l;
    private final Downloadable                downloadable;
    private final DownloadLink                link;
    private final LogInterface                logger;
    private File                              outputCompleteFile;
    private File                              outputFinalCompleteFile;
    protected Long                            lastModified      = null;
    private String                            textToWrite       = null;
    private ManagedThrottledConnectionHandler connectionHandler = new ManagedThrottledConnectionHandler();
    private long                              startTimeStamp    = -1;

    public boolean isResumable() {
        return false;
    }

    public TextDownloader(final PluginForHost plugin, final DownloadLink link, final String text) {
        if (plugin == null || link == null) {
            throw new IllegalArgumentException();
        } else if (text == null) {
            throw new IllegalArgumentException("text can't be null");
        }
        this.link = link;
        logger = plugin.getLogger();
        this.downloadable = new DownloadLinkDownloadable(link); // Dummy else we will run into an NPE!
        downloadable.setDownloadInterface(this);
        textToWrite = text;
    }

    public long getBytesLoaded() {
        return bytesWritten;
    }

    @Override
    public ManagedThrottledConnectionHandler getManagedConnetionHandler() {
        return this.connectionHandler;
    }

    @Override
    public URLConnectionAdapter connect(Browser br) throws Exception {
        throw new WTFException("Not needed");
    }

    @Override
    public long getTotalLinkBytesLoadedLive() {
        return getBytesLoaded();
    }

    public boolean startDownload() throws Exception {
        try {
            try {
                /* We are about to start downloading so we know that if there was a captcha, it has been solved successfully. */
                downloadable.validateLastChallengeResponse();
            } catch (final Throwable e) {
                LogSource.exception(logger, e);
            }
            logger.finer("Start Download");
            try {
                if (this.textToWrite == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                DownloadPluginProgress downloadPluginProgress = null;
                downloadable.setConnectionHandler(this.getManagedConnetionHandler());
                final DiskSpaceReservation reservation = downloadable.createDiskSpaceReservation();
                try {
                    if (!downloadable.checkIfWeCanWrite(new ExceptionRunnable() {
                        @Override
                        public void run() throws Exception {
                            downloadable.checkAndReserve(reservation);
                            createOutputChannel();
                            try {
                                downloadable.lockFiles(outputCompleteFile, outputFinalCompleteFile);
                            } catch (FileIsLockedException e) {
                                downloadable.unlockFiles(outputCompleteFile, outputFinalCompleteFile);
                                throw new PluginException(LinkStatus.ERROR_ALREADYEXISTS, null, e);
                            }
                        }
                    }, null)) {
                        throw new SkipReasonException(SkipReason.INVALID_DESTINATION);
                    }
                    startTimeStamp = System.currentTimeMillis();
                    downloadPluginProgress = new DownloadPluginProgress(downloadable, this, Color.GREEN.darker());
                    downloadable.addPluginProgress(downloadPluginProgress);
                    downloadable.setAvailable(AvailableStatus.TRUE);
                    IO.writeToFile(this.outputFinalCompleteFile, this.textToWrite.getBytes("UTF-8"), IO.SYNC.META_AND_DATA);
                    /* Set filesize so user can see it in UI. */
                    this.bytesWritten = this.outputFinalCompleteFile.length();
                    link.setVerifiedFileSize(this.bytesWritten);
                    /* Set progress to finished - the "download" is complete. */
                    link.getLinkStatus().setStatus(LinkStatus.FINISHED);
                } finally {
                    try {
                        downloadable.free(reservation);
                    } catch (final Throwable e) {
                        LogSource.exception(logger, e);
                    }
                    try {
                        final long startTimeStamp = getStartTimeStamp();
                        if (startTimeStamp > 0) {
                            downloadable.addDownloadTime(System.currentTimeMillis() - getStartTimeStamp());
                        }
                    } catch (final Throwable e) {
                    }
                    downloadable.removePluginProgress(downloadPluginProgress);
                }
                // boolean renameOkay = downloadable.rename(outputPartFile, outputCompleteFile);
                // if (!renameOkay) {
                // throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, _JDT.T.system_download_errors_couldnotrename(),
                // LinkStatus.VALUE_LOCAL_IO_ERROR);
                // }
                return true;
            } finally {
                try {
                    cleanupDownladInterface();
                } finally {
                    downloadable.unlockFiles(outputCompleteFile, outputFinalCompleteFile);
                }
            }
        } finally {
            cleanupDownladInterface();
        }
    }

    protected boolean finalizeDownload(File outputPartFile, File outputCompleteFile, Long lastModified) throws Exception {
        if (downloadable.rename(outputPartFile, outputCompleteFile)) {
            try {
                if (lastModified != null && JsonConfig.create(GeneralSettings.class).isUseOriginalLastModified()) {
                    /* set original lastModified timestamp */
                    outputCompleteFile.setLastModified(lastModified.longValue());
                } else {
                    /* set current timestamp as lastModified timestamp */
                    outputCompleteFile.setLastModified(System.currentTimeMillis());
                }
            } catch (final Throwable ignore) {
                LogSource.exception(logger, ignore);
            }
            return true;
        } else {
            return false;
        }
    }

    protected void cleanupDownladInterface() {
    }

    private void createOutputChannel() throws SkipReasonException {
        try {
            final String fileOutput = downloadable.getFileOutput();
            logger.info("createOutputChannel for " + fileOutput);
            final String finalFileOutput = downloadable.getFinalFileOutput();
            outputCompleteFile = new File(fileOutput);
            outputFinalCompleteFile = outputCompleteFile;
            if (!fileOutput.equals(finalFileOutput)) {
                outputFinalCompleteFile = new File(finalFileOutput);
            }
        } catch (Exception e) {
            logger.log(e);
            throw new SkipReasonException(SkipReason.INVALID_DESTINATION, e);
        }
    }

    @Override
    public URLConnectionAdapter getConnection() {
        return null;
    }

    /** signal that we stopped download external */
    public void stopDownload() {
        if (abort.getAndSet(true) == false) {
            logger.info("externalStop recieved");
        }
    }

    private final AtomicBoolean abort = new AtomicBoolean(false);

    @Override
    public boolean externalDownloadStop() {
        return abort.get();
    }

    @Override
    public long getStartTimeStamp() {
        return startTimeStamp;
    }

    @Override
    public void close() {
        /* Do nothing */
    }

    @Override
    public Downloadable getDownloadable() {
        return downloadable;
    }

    @Override
    public boolean isResumedDownload() {
        return false;
    }
}

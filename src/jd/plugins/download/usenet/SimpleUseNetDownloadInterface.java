package jd.plugins.download.usenet;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

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
import jd.plugins.components.UsenetFile;
import jd.plugins.components.UsenetFileSegment;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.DownloadLinkDownloadable;
import jd.plugins.download.Downloadable;
import jd.plugins.download.HashResult;
import jd.plugins.download.SparseFile;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.throttledconnection.MeteredThrottledInputStream;
import org.appwork.utils.speedmeter.AverageSpeedMeter;
import org.jdownloader.plugins.DownloadPluginProgress;
import org.jdownloader.plugins.SkipReason;
import org.jdownloader.plugins.SkipReasonException;
import org.jdownloader.translate._JDT;

public class SimpleUseNetDownloadInterface extends DownloadInterface {

    private final Downloadable                      downloadable;
    private final ManagedThrottledConnectionHandler connectionHandler;
    private final Logger                            logger;

    private final AtomicBoolean                     abort                    = new AtomicBoolean(false);
    private final AtomicBoolean                     terminated               = new AtomicBoolean(false);
    private RandomAccessFile                        outputPartFileRaf;
    private File                                    outputCompleteFile;
    private File                                    outputFinalCompleteFile;
    private File                                    outputPartFile;
    protected long                                  totalLinkBytesLoaded     = -1;
    protected final AtomicLong                      totalLinkBytesLoadedLive = new AtomicLong(0);
    private long                                    startTimeStamp           = -1;
    private boolean                                 resumed;
    private final SimpleUseNet                      client;
    private final UsenetFile                        usenetFile;

    public SimpleUseNetDownloadInterface(SimpleUseNet client, final DownloadLink link, final UsenetFile usenetFile) {
        connectionHandler = new ManagedThrottledConnectionHandler();
        this.usenetFile = usenetFile;
        final boolean resumeable = usenetFile.getNumSegments() > 1;
        downloadable = new DownloadLinkDownloadable(link) {
            @Override
            public boolean isResumable() {
                return resumeable;
            }

            @Override
            public void setResumeable(boolean value) {
                super.setResumeable(resumeable && value);
            }

            @Override
            public void updateFinalFileName() {
            }
        };
        if (resumeable) {
            downloadable.setResumeable(true);
        } else {
            downloadable.setResumeable(false);
        }
        logger = downloadable.getLogger();
        downloadable.setDownloadInterface(this);
        this.client = client;
    }

    @Override
    public ManagedThrottledConnectionHandler getManagedConnetionHandler() {
        return connectionHandler;
    }

    private void createOutputChannel() throws SkipReasonException {
        try {
            String fileOutput = downloadable.getFileOutput();
            logger.info("createOutputChannel for " + fileOutput);
            String finalFileOutput = downloadable.getFinalFileOutput();
            outputCompleteFile = new File(fileOutput);
            outputFinalCompleteFile = outputCompleteFile;
            if (!fileOutput.equals(finalFileOutput)) {
                outputFinalCompleteFile = new File(finalFileOutput);
            }
            outputPartFile = new File(downloadable.getFileOutputPart());
            try {
                if (Application.getJavaVersion() >= Application.JAVA17) {
                    SparseFile.createSparseFile(outputPartFile);
                }
            } catch (IOException e) {
            }
            outputPartFileRaf = new RandomAccessFile(outputPartFile, "rw");
        } catch (Exception e) {
            LogSource.exception(logger, e);
            throw new SkipReasonException(SkipReason.INVALID_DESTINATION, e);
        }
    }

    @Override
    public URLConnectionAdapter connect(Browser br) throws Exception {
        throw new WTFException("Not needed for SimpleFTPDownloadInterface");
    }

    @Override
    public long getTotalLinkBytesLoadedLive() {
        return totalLinkBytesLoadedLive.get();
    }

    protected void download() throws Exception {
        boolean localIO = false;
        totalLinkBytesLoaded = 0;
        MeteredThrottledInputStream meteredThrottledInputStream = null;
        try {
            for (UsenetFileSegment segment : usenetFile.getSegments()) {
                if (abort.get()) {
                    break;
                }
                final InputStream bodyInputStream = client.requestMessageBodyAsInputStream(segment.getMessageID());
                if (meteredThrottledInputStream == null) {
                    meteredThrottledInputStream = new MeteredThrottledInputStream(bodyInputStream, new AverageSpeedMeter(10));
                    connectionHandler.addThrottledConnection(meteredThrottledInputStream);
                } else {
                    meteredThrottledInputStream.setInputStream(bodyInputStream);
                }
                if (bodyInputStream instanceof YEncInputStream) {
                    final YEncInputStream yEnc = (YEncInputStream) bodyInputStream;
                    final String fileName = yEnc.getName();
                    if (StringUtils.isNotEmpty(fileName) && downloadable.getFinalFileName() == null) {
                        downloadable.setFinalFileName(fileName);
                    }
                    final long fileSize = yEnc.getSize();
                    final long verifiedFileSize = downloadable.getVerifiedFileSize();
                    if (fileSize >= 0 && (verifiedFileSize == -1 || fileSize > verifiedFileSize)) {
                        downloadable.setVerifiedFileSize(fileSize);
                    }
                } else if (bodyInputStream instanceof UUInputStream) {
                    final UUInputStream uu = (UUInputStream) bodyInputStream;
                    final String fileName = uu.getName();
                    if (StringUtils.isNotEmpty(fileName) && downloadable.getFinalFileName() == null) {
                        downloadable.setFinalFileName(fileName);
                    }
                }
                int bytesRead = 0;
                final byte[] buffer = new byte[32767];
                final long loaded = totalLinkBytesLoaded;
                while ((bytesRead = meteredThrottledInputStream.read(buffer)) != -1) {
                    if (abort.get()) {
                        break;
                    }
                    if (bytesRead > 0) {
                        totalLinkBytesLoaded += bytesRead;
                        localIO = true;
                        outputPartFileRaf.write(buffer, 0, bytesRead);
                        localIO = false;
                        totalLinkBytesLoadedLive.addAndGet(bytesRead);
                    }
                }
                if (bodyInputStream instanceof YEncInputStream) {
                    final YEncInputStream yEnc = (YEncInputStream) bodyInputStream;
                    totalLinkBytesLoaded = loaded + yEnc.getPartSize();
                    totalLinkBytesLoadedLive.set(totalLinkBytesLoaded);
                    outputPartFileRaf.seek(totalLinkBytesLoaded);
                }
            }
        } catch (IOException e) {
            if (localIO) {
                throw new SkipReasonException(SkipReason.DISK_FULL);
            }
            throw e;
        } finally {
            downloadable.setDownloadBytesLoaded(totalLinkBytesLoaded);
            if (meteredThrottledInputStream != null) {
                connectionHandler.removeThrottledConnection(meteredThrottledInputStream);
            }
        }
    }

    @Override
    public boolean startDownload() throws Exception {
        try {
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
                            downloadable.lockFiles(outputCompleteFile, outputFinalCompleteFile, outputPartFile);
                        } catch (FileIsLockedException e) {
                            downloadable.unlockFiles(outputCompleteFile, outputFinalCompleteFile, outputPartFile);
                            throw new PluginException(LinkStatus.ERROR_ALREADYEXISTS);
                        }
                    }
                }, null)) {
                    throw new SkipReasonException(SkipReason.INVALID_DESTINATION);
                }
                startTimeStamp = System.currentTimeMillis();
                downloadPluginProgress = new DownloadPluginProgress(downloadable, this, Color.GREEN.darker());
                downloadable.addPluginProgress(downloadPluginProgress);
                downloadable.setAvailable(AvailableStatus.TRUE);
                download();
            } finally {
                try {
                    downloadable.free(reservation);
                } catch (final Throwable e) {
                    LogSource.exception(logger, e);
                }
                try {
                    downloadable.addDownloadTime(System.currentTimeMillis() - getStartTimeStamp());
                } catch (final Throwable e) {
                }
                downloadable.removePluginProgress(downloadPluginProgress);
            }
            if (isDownloadComplete()) {
                logger.info("Download is complete");
                final HashResult hashResult = getHashResult(downloadable, outputPartFile);
                if (hashResult != null) {
                    logger.info(hashResult.toString());
                }
                downloadable.setHashResult(hashResult);
                if (hashResult == null || hashResult.match()) {
                    downloadable.setVerifiedFileSize(outputPartFile.length());

                } else {
                    if (hashResult.getHashInfo().isTrustworthy()) {
                        throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, _JDT._.system_download_doCRC2_failed(hashResult.getHashInfo().getType()));
                    }
                }
                finalizeDownload(outputPartFile, outputCompleteFile);
                downloadable.setLinkStatus(LinkStatus.FINISHED);
                return true;
            }
            if (externalDownloadStop() == false) {
                throw new PluginException(LinkStatus.ERROR_DOWNLOAD_INCOMPLETE, _JDT._.download_error_message_incomplete());
            }
            return false;
        } finally {
            downloadable.unlockFiles(outputCompleteFile, outputFinalCompleteFile, outputPartFile);
            cleanupDownladInterface();
        }
    }

    protected boolean isDownloadComplete() {
        final long verifiedFileSize = downloadable.getVerifiedFileSize();
        if (verifiedFileSize >= 0) {
            return totalLinkBytesLoaded == verifiedFileSize;
        }
        if (externalDownloadStop() == false) {
            return true;
        }
        return false;
    }

    protected void finalizeDownload(File outputPartFile, File outputCompleteFile) throws Exception {
        if (downloadable.rename(outputPartFile, outputCompleteFile)) {
            try { /* set current timestamp as lastModified timestamp */
                outputCompleteFile.setLastModified(System.currentTimeMillis());
            } catch (final Throwable ignore) {
                LogSource.exception(logger, ignore);
            }
        } else {
            throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, _JDT._.system_download_errors_couldnotrename(), LinkStatus.VALUE_LOCAL_IO_ERROR);
        }
    }

    protected void cleanupDownladInterface() {
        try {
            downloadable.removeConnectionHandler(this.getManagedConnetionHandler());
        } catch (final Throwable e) {
        }
        try {
            client.quit();
        } catch (Throwable e) {
        }
        closeOutputChannel();
    }

    private void closeOutputChannel() {
        try {
            RandomAccessFile loutputPartFileRaf = outputPartFileRaf;
            if (loutputPartFileRaf != null) {
                logger.info("Close File. Let AV programs run");
                loutputPartFileRaf.close();
            }
        } catch (Throwable e) {
            LogSource.exception(logger, e);
        } finally {
            outputPartFileRaf = null;
        }
    }

    protected long getFileSize() {
        return downloadable.getVerifiedFileSize();
    }

    @Override
    public URLConnectionAdapter getConnection() {
        throw new WTFException("SimpleUseNetDownloadInterface");
    }

    @Override
    public void stopDownload() {
        if (abort.getAndSet(true) == false) {
            logger.info("externalStop recieved");
            terminate();
        }
    }

    protected void terminate() {
        if (terminated.getAndSet(true) == false) {
            if (!externalDownloadStop()) {
                logger.severe("A critical Downloaderror occured. Terminate...");
            }
        }
    }

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
    }

    @Override
    public Downloadable getDownloadable() {
        return downloadable;
    }

    @Override
    public boolean isResumedDownload() {
        return resumed;
    }

}

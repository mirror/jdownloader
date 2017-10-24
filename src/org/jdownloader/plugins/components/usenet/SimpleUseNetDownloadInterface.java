package org.jdownloader.plugins.components.usenet;

import java.awt.Color;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

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
import jd.plugins.PluginProgress;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.DownloadLinkDownloadable;
import jd.plugins.download.Downloadable;
import jd.plugins.download.HashInfo;
import jd.plugins.download.HashInfo.TYPE;
import jd.plugins.download.HashResult;
import jd.plugins.download.SparseFile;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.NullInputStream;
import org.appwork.utils.net.socketconnection.SocketConnection;
import org.appwork.utils.net.throttledconnection.MeteredThrottledInputStream;
import org.appwork.utils.net.usenet.MessageBodyNotFoundException;
import org.appwork.utils.net.usenet.SimpleUseNet;
import org.appwork.utils.net.usenet.YEncInputStream;
import org.appwork.utils.speedmeter.AverageSpeedMeter;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.controlling.FileCreationManager.DeleteOption;
import org.jdownloader.plugins.DownloadPluginProgress;
import org.jdownloader.plugins.HashCheckPluginProgress;
import org.jdownloader.plugins.SkipReason;
import org.jdownloader.plugins.SkipReasonException;
import org.jdownloader.translate._JDT;

public class SimpleUseNetDownloadInterface extends DownloadInterface {
    private final Downloadable                      downloadable;
    private final ManagedThrottledConnectionHandler connectionHandler;
    private final LogInterface                      logger;
    private final AtomicBoolean                     abort                = new AtomicBoolean(false);
    private final AtomicBoolean                     terminated           = new AtomicBoolean(false);
    private File                                    outputCompleteFile;
    private File                                    outputFinalCompleteFile;
    private File                                    outputPartFile;
    protected final AtomicLong                      totalLinkBytesLoaded = new AtomicLong(0);
    private long                                    startTimeStamp       = -1;
    private boolean                                 resumed              = false;
    private final SimpleUseNet                      client;
    private final UsenetFile                        usenetFile;
    private final DownloadLink                      downloadLink;

    public SimpleUseNetDownloadInterface(final SimpleUseNet client, final DownloadLink downloadLink, final UsenetFile usenetFile) {
        connectionHandler = new ManagedThrottledConnectionHandler();
        this.usenetFile = usenetFile;
        final boolean resumeable = usenetFile.getNumSegments() > 1 || usenetFile.getSegments().size() > 1;
        final String host = SocketConnection.getHostName(client.getSocket().getRemoteSocketAddress());
        this.downloadLink = downloadLink;
        downloadable = new DownloadLinkDownloadable(downloadLink) {
            @Override
            public boolean isResumable() {
                return resumeable;
            }

            @Override
            public String getHost() {
                return host;
            }

            @Override
            public HashInfo getHashInfo() {
                final HashInfo ret = super.getHashInfo();
                if (ret == null) {
                    return usenetFile._getHashInfo();
                } else {
                    return ret;
                }
            }

            @Override
            public HashResult getHashResult(HashInfo hashInfo, File outputPartFile) {
                if (hashInfo != null) {
                    return super.getHashResult(hashInfo, outputPartFile);
                } else {
                    HashInfo.TYPE type = null;
                    for (final UsenetFileSegment segment : usenetFile.getSegments()) {
                        if (segment.getHash() != null) {
                            final HashInfo segmentHashInfo = segment._getHashInfo();
                            if (segmentHashInfo != null) {
                                if (type == null) {
                                    type = segmentHashInfo.getType();
                                } else if (type != segmentHashInfo.getType()) {
                                    type = null;
                                    break;
                                }
                            }
                        }
                    }
                    if (type != null) {
                        final PluginProgress hashProgress = new HashCheckPluginProgress(outputPartFile, Color.YELLOW.darker(), type);
                        hashProgress.setProgressSource(this);
                        try {
                            addPluginProgress(hashProgress);
                            final HashInfo fileHashInfo;
                            switch (type) {
                            case CRC32:
                                final FileInputStream fis = new FileInputStream(outputPartFile);
                                final long checksum;
                                try {
                                    final byte[] b = new byte[128 * 1024];
                                    int read = 0;
                                    long cur = 0;
                                    final CheckedInputStream cis = new CheckedInputStream(fis, new CRC32());
                                    while ((read = cis.read(b)) >= 0) {
                                        cur += read;
                                        hashProgress.setCurrent(cur);
                                    }
                                    checksum = cis.getChecksum().getValue();
                                    cis.close();
                                } finally {
                                    fis.close();
                                }
                                fileHashInfo = new HashInfo(HexFormatter.byteArrayToHex(new byte[] { (byte) (checksum >>> 24), (byte) (checksum >>> 16), (byte) (checksum >>> 8), (byte) checksum }), TYPE.CRC32);
                                break;
                            default:
                                fileHashInfo = null;
                                break;
                            }
                            if (fileHashInfo != null) {
                                return new HashResult(fileHashInfo, fileHashInfo.getHash());
                            }
                        } catch (final Throwable e) {
                            logger.log(e);
                        } finally {
                            removePluginProgress(hashProgress);
                        }
                    }
                }
                return null;
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

    private void createOutputFiles() throws SkipReasonException {
        try {
            final String fileOutput = downloadable.getFileOutput();
            logger.info("createOutputChannel for " + fileOutput);
            final String finalFileOutput = downloadable.getFinalFileOutput();
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
        return getTotalLinkBytesLoaded();
    }

    private long getTotalLinkBytesLoaded() {
        return totalLinkBytesLoaded.get();
    }

    private void drainInputStream(final InputStream is) throws IOException {
        final byte[] drainBuffer = new byte[1024];
        while (is.read(drainBuffer) != -1) {
        }
    }

    protected void download() throws Exception {
        final RandomAccessFile raf;
        try {
            raf = IO.open(outputPartFile, "rw");
        } catch (final IOException e) {
            throw new SkipReasonException(SkipReason.INVALID_DESTINATION, e);
        }
        boolean localIO = false;
        totalLinkBytesLoaded.set(0);
        final MeteredThrottledInputStream meteredThrottledInputStream = new MeteredThrottledInputStream(new NullInputStream(), new AverageSpeedMeter(10));
        boolean writeUsenetFile = false;
        try {
            final ArrayList<UsenetFileSegment> segments = new ArrayList<UsenetFileSegment>(usenetFile.getSegments());
            if (downloadable.isResumable() && raf.length() > 0) {
                final long partFileSize = raf.length();
                final Iterator<UsenetFileSegment> it = segments.iterator();
                long resumePosition = 0;
                while (it.hasNext()) {
                    final UsenetFileSegment next = it.next();
                    if (next.getPartBegin() > 0 && next.getPartEnd() > 0) {
                        if (partFileSize > next.getPartEnd()) {
                            resumePosition = next.getPartEnd();
                            it.remove();
                            continue;
                        }
                    }
                    break;
                }
                if (resumePosition > 0) {
                    resumed = true;
                }
            }
            connectionHandler.addThrottledConnection(meteredThrottledInputStream);
            final byte[] buffer = new byte[32767];
            segmentLoop: for (final UsenetFileSegment segment : segments) {
                if (abort.get()) {
                    break segmentLoop;
                } else {
                    final InputStream bodyInputStream = client.requestMessageBodyAsInputStream(segment.getMessageID());
                    meteredThrottledInputStream.setInputStream(bodyInputStream);
                    if (bodyInputStream instanceof YEncInputStream) {
                        final YEncInputStream yEnc = (YEncInputStream) bodyInputStream;
                        final long partSize = yEnc.getPartSize();
                        if (partSize >= 0) {
                            segment.setPartBegin(yEnc.getPartBegin());
                            segment.setPartEnd(yEnc.getPartEnd());
                            final long writePosition = yEnc.getPartBegin() - 1;
                            // update file-pointer and totalLinkBytesLoaded
                            raf.seek(writePosition);
                            totalLinkBytesLoaded.set(writePosition);
                            writeUsenetFile = true;
                        }
                        meteredThrottledInputStream.setInputStream(new CheckedInputStream(bodyInputStream, new CRC32()));
                    }
                    int bytesRead = 0;
                    while ((bytesRead = meteredThrottledInputStream.read(buffer)) != -1) {
                        if (abort.get()) {
                            // so we can quit normally
                            drainInputStream(bodyInputStream);
                            break segmentLoop;
                        }
                        if (bytesRead > 0) {
                            localIO = true;
                            raf.write(buffer, 0, bytesRead);
                            localIO = false;
                            totalLinkBytesLoaded.addAndGet(bytesRead);
                        }
                    }
                    if (bodyInputStream instanceof YEncInputStream) {
                        final YEncInputStream yEnc = (YEncInputStream) bodyInputStream;
                        if (yEnc.getPartCRC32() != null && meteredThrottledInputStream.getInputStream() instanceof CheckedInputStream) {
                            final HashInfo hashInfo = new HashInfo(yEnc.getPartCRC32(), HashInfo.TYPE.CRC32);
                            final long checksum = ((CheckedInputStream) meteredThrottledInputStream.getInputStream()).getChecksum().getValue();
                            if (!new HashResult(hashInfo, HexFormatter.byteArrayToHex(new byte[] { (byte) (checksum >>> 24), (byte) (checksum >>> 16), (byte) (checksum >>> 8), (byte) checksum })).match()) {
                                throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, _JDT.T.system_download_doCRC2_failed(HashInfo.TYPE.CRC32));
                            }
                            segment._setHashInfo(hashInfo);
                            writeUsenetFile = true;
                        }
                        if (usenetFile.getHash() == null && yEnc.getFileCRC32() != null) {
                            usenetFile._setHashInfo(new HashInfo(yEnc.getFileCRC32(), HashInfo.TYPE.CRC32, true));
                            writeUsenetFile = true;
                        }
                    }
                }
            }
        } catch (IOException e) {
            if (localIO) {
                throw new SkipReasonException(SkipReason.DISK_FULL);
            }
            throw e;
        } finally {
            close(raf);
            cleanupDownladInterface();
            if (writeUsenetFile) {
                usenetFile._write(downloadLink);
            }
            downloadable.setDownloadBytesLoaded(getTotalLinkBytesLoaded());
            if (meteredThrottledInputStream != null) {
                connectionHandler.removeThrottledConnection(meteredThrottledInputStream);
            }
        }
    }

    @Override
    public boolean startDownload() throws Exception {
        boolean deletePartFile = false;
        try {
            downloadable.setConnectionHandler(this.getManagedConnetionHandler());
            final DiskSpaceReservation reservation = downloadable.createDiskSpaceReservation();
            DownloadPluginProgress downloadPluginProgress = null;
            try {
                if (!downloadable.checkIfWeCanWrite(new ExceptionRunnable() {
                    @Override
                    public void run() throws Exception {
                        downloadable.checkAndReserve(reservation);
                        createOutputFiles();
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
            } catch (MessageBodyNotFoundException e) {
                deletePartFile = true;
                throw e;
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
            if (isDownloadComplete()) {
                logger.info("Download is complete");
                final HashResult hashResult = getHashResult(downloadable, outputPartFile);
                if (hashResult != null) {
                    logger.info(hashResult.toString());
                    downloadable.setHashResult(hashResult);
                }
                if (hashResult == null || hashResult.match()) {
                    downloadable.setVerifiedFileSize(outputPartFile.length());
                } else {
                    if (hashResult.getHashInfo().isTrustworthy()) {
                        throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, _JDT.T.system_download_doCRC2_failed(hashResult.getHashInfo().getType()));
                    }
                }
                finalizeDownload(outputPartFile, outputCompleteFile);
                downloadable.setLinkStatus(LinkStatus.FINISHED);
                return true;
            }
            if (externalDownloadStop() == false) {
                throw new PluginException(LinkStatus.ERROR_DOWNLOAD_INCOMPLETE, _JDT.T.download_error_message_incomplete());
            }
            return false;
        } finally {
            downloadable.unlockFiles(outputCompleteFile, outputFinalCompleteFile, outputPartFile);
            cleanupDownladInterface();
            if (deletePartFile) {
                FileCreationManager.getInstance().delete(outputPartFile, DeleteOption.NULL);
            }
        }
    }

    private void close(Closeable closable) {
        try {
            if (closable != null) {
                closable.close();
            }
        } catch (Throwable e) {
        }
    }

    private boolean isTerminated() {
        return terminated.get();
    }

    private boolean isDownloadComplete() {
        final long verifiedFileSize = downloadable.getVerifiedFileSize();
        if (verifiedFileSize >= 0) {
            return getTotalLinkBytesLoaded() == verifiedFileSize;
        }
        if (externalDownloadStop() == false && isTerminated() == false) {
            // no stop and not terminated, normal finish
            return true;
        }
        return false;
    }

    private void finalizeDownload(final File outputPartFile, final File outputCompleteFile) throws Exception {
        if (downloadable.rename(outputPartFile, outputCompleteFile)) {
            try { /* set current timestamp as lastModified timestamp */
                outputCompleteFile.setLastModified(System.currentTimeMillis());
            } catch (final Throwable ignore) {
                LogSource.exception(logger, ignore);
            }
        } else {
            throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, _JDT.T.system_download_errors_couldnotrename(), LinkStatus.VALUE_LOCAL_IO_ERROR);
        }
    }

    private void cleanupDownladInterface() {
        try {
            downloadable.removeConnectionHandler(this.getManagedConnetionHandler());
        } catch (final Throwable e) {
        }
        try {
            client.quit();
        } catch (Throwable e) {
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

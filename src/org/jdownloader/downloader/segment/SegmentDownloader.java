package org.jdownloader.downloader.segment;

import java.awt.Color;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.controlling.downloadcontroller.DiskSpaceReservation;
import jd.controlling.downloadcontroller.ExceptionRunnable;
import jd.controlling.downloadcontroller.FileIsLockedException;
import jd.controlling.downloadcontroller.ManagedThrottledConnectionHandler;
import jd.http.Browser;
import jd.http.Request;
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
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.NullInputStream;
import org.appwork.utils.net.URLHelper;
import org.appwork.utils.net.httpconnection.HTTPConnectionUtils;
import org.appwork.utils.net.throttledconnection.MeteredThrottledInputStream;
import org.appwork.utils.speedmeter.AverageSpeedMeter;
import org.jdownloader.plugins.DownloadPluginProgress;
import org.jdownloader.plugins.SkipReason;
import org.jdownloader.plugins.SkipReasonException;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.translate._JDT;

//http://tools.ietf.org/html/draft-pantos-http-live-streaming-13
public class SegmentDownloader extends DownloadInterface {
    protected volatile long                         bytesWritten      = 0l;
    private final Downloadable                      downloadable;
    private final DownloadLink                      link;
    private long                                    startTimeStamp    = -1;
    private final LogInterface                      logger;
    private volatile URLConnectionAdapter           currentConnection;
    private final ManagedThrottledConnectionHandler connectionHandler = new ManagedThrottledConnectionHandler();
    private File                                    outputCompleteFile;
    private File                                    outputFinalCompleteFile;
    private File                                    outputPartFile;
    private PluginException                         caughtPluginException;
    protected final Browser                         obr;
    protected final List<Segment>                   segments          = new ArrayList<Segment>();
    protected Long                                  lastModified      = null;

    @Deprecated
    public static List<Segment> buildSegments(URL baseURL, String[] segments) {
        final List<Segment> ret = new ArrayList<Segment>();
        for (final String segment : segments) {
            ret.add(new Segment(URLHelper.parseLocation(baseURL, segment)));
        }
        return ret;
    }

    @Deprecated
    public SegmentDownloader(final PluginForHost plugin, final DownloadLink link, Downloadable dashDownloadable, Browser br, URL baseURL, String[] segments) {
        this(plugin, link, dashDownloadable, br, buildSegments(baseURL, segments));
    }

    public SegmentDownloader(final PluginForHost plugin, final DownloadLink link, Downloadable dashDownloadable, Browser br2, List<Segment> segments) {
        this.obr = br2.cloneBrowser();
        this.link = link;
        logger = plugin.getLogger();
        this.segments.addAll(segments);
        if (dashDownloadable == null) {
            this.downloadable = new DownloadLinkDownloadable(link) {
                @Override
                public boolean isResumable() {
                    // TODO: maybe resume at last written Segment, save index and position
                    return false;
                }
            };
        } else {
            this.downloadable = dashDownloadable;
        }
        downloadable.setDownloadInterface(this);
    }

    protected void terminate() {
        if (terminated.getAndSet(true) == false) {
            if (!externalDownloadStop()) {
                logger.severe("A critical Downloaderror occured. Terminate...");
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

    protected boolean retrySegmentConnection(Browser br, Segment segment, int segmentRetryCounter) throws InterruptedException, PluginException {
        final Boolean retry = segment.retrySegmentConnection(br, segment, segmentRetryCounter);
        if (retry != null) {
            return retry.booleanValue();
        }
        final URLConnectionAdapter con = br.getHttpConnection();
        if (!externalDownloadStop() && segmentRetryCounter == 0 && (con.getResponseCode() == 404 || con.getResponseCode() == 502)) {
            return true;
        } else {
            return false;
        }
    }

    protected boolean isSegmentConnectionValid(Segment segment, URLConnectionAdapter con) throws IOException, PluginException {
        return segment.isConnectionValid(con);
    }

    protected URLConnectionAdapter openSegmentConnection(Segment segment) throws IOException, PluginException, InterruptedException {
        int segmentRetryCounter = 0;
        while (!externalDownloadStop()) {
            final Browser br = obr.cloneBrowser();
            final Request request = createSegmentRequest(segment);
            final URLConnectionAdapter ret = segment.open(br, request);
            if (!isSegmentConnectionValid(segment, ret)) {
                br.followConnection(true);
                if (retrySegmentConnection(br, segment, segmentRetryCounter)) {
                    segmentRetryCounter++;
                } else {
                    throw new IOException("Invalid responseCode:" + ret.getResponseCode() + "|RetryCounter:" + segmentRetryCounter + "|Segment:" + segment.getUrl());
                }
            } else {
                return ret;
            }
        }
        throw new PluginException(LinkStatus.ERROR_RETRY, "externalDownloadStop");
    }

    protected InputStream getInputStream(Segment segment, URLConnectionAdapter connection) throws IOException, PluginException {
        return connection.getInputStream();
    }

    protected int writeSegment(Segment segment, URLConnectionAdapter con, RandomAccessFile outputStream, byte[] buf, int index, int len) throws IOException {
        outputStream.write(buf, index, len);
        segment.getChunkRange().incLoaded(len);
        return len;
    }

    protected int readSegment(Segment segment, URLConnectionAdapter con, InputStream is, byte[] buf) throws IOException {
        return is.read(buf);
    }

    protected void onSegmentClose(RandomAccessFile outputStream, Segment segment, URLConnectionAdapter con) throws IOException {
    }

    protected long onSegmentStart(RandomAccessFile outputStream, Segment segment, URLConnectionAdapter con) throws IOException {
        final long[] range = HTTPConnectionUtils.parseRequestRange(con);
        if (range != null && range[0] != -1) {
            outputStream.seek(range[0]);
            bytesWritten = range[0];
        }
        return bytesWritten;
    }

    public void run() throws Exception {
        link.setDownloadSize(-1);
        final boolean isResumedDownload = isResumedDownload();
        final RandomAccessFile outputStream;
        try {
            outputStream = new RandomAccessFile(outputPartFile, "rw");
        } catch (IOException e) {
            throw new SkipReasonException(SkipReason.INVALID_DESTINATION, e);
        }
        final MeteredThrottledInputStream meteredThrottledInputStream = new MeteredThrottledInputStream(new NullInputStream(), new AverageSpeedMeter(10));
        boolean localIO = false;
        try {
            if (!isResumedDownload) {
                outputStream.getChannel().truncate(0);
            }
            final String cust = link.getCustomExtension();
            link.setCustomExtension(null);
            link.setCustomExtension(cust);
            final byte[] readWriteBuffer = new byte[512 * 1024];
            if (connectionHandler != null) {
                connectionHandler.addThrottledConnection(meteredThrottledInputStream);
            }
            for (final Segment segment : segments) {
                if (!externalDownloadStop()) {
                    try {
                        currentConnection = openSegmentConnection(segment);
                        if (lastModified == null) {
                            final Date last = TimeFormatter.parseDateString(currentConnection.getHeaderField("Last-Modified"));
                            if (last != null) {
                                lastModified = last.getTime();
                            }
                        }
                        segment.getChunkRange().setValidLoaded(true);
                        meteredThrottledInputStream.setInputStream(getInputStream(segment, currentConnection));
                        bytesWritten = onSegmentStart(outputStream, segment, currentConnection);
                        while (!externalDownloadStop()) {
                            final int read = readSegment(segment, currentConnection, meteredThrottledInputStream, readWriteBuffer);
                            if (read > 0) {
                                localIO = true;
                                bytesWritten += writeSegment(segment, currentConnection, outputStream, readWriteBuffer, 0, read);
                                localIO = false;
                                downloadable.setDownloadBytesLoaded(bytesWritten);
                            } else if (read == -1) {
                                break;
                            }
                        }
                    } finally {
                        final URLConnectionAdapter lCurrentConnection = currentConnection;
                        currentConnection = null;
                        if (lCurrentConnection != null) {
                            try {
                                onSegmentClose(outputStream, segment, lCurrentConnection);
                            } finally {
                                lCurrentConnection.disconnect();
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            if (localIO) {
                throw new SkipReasonException(SkipReason.DISK_FULL);
            } else {
                throw e;
            }
        } catch (InterruptedException e) {
            throw e;
        } catch (PluginException e) {
            throw e;
        } catch (Throwable e) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, e.getMessage(), -1, e);
        } finally {
            close(outputStream);
            if (connectionHandler != null && meteredThrottledInputStream != null) {
                connectionHandler.removeThrottledConnection(meteredThrottledInputStream);
            }
        }
    }

    protected Request createSegmentRequest(final Segment seg) throws IOException {
        final Request ret = seg.createRequest();
        return ret;
    }

    public long getBytesLoaded() {
        return bytesWritten;
    }

    @Override
    public ManagedThrottledConnectionHandler getManagedConnetionHandler() {
        return connectionHandler;
    }

    @Override
    public URLConnectionAdapter connect(Browser br) throws Exception {
        throw new WTFException("Not needed");
    }

    @Override
    public long getTotalLinkBytesLoadedLive() {
        return getBytesLoaded();
    }

    @Override
    public boolean startDownload() throws Exception {
        try {
            String fileOutput = downloadable.getFileOutput();
            String finalFileOutput = downloadable.getFinalFileOutput();
            outputCompleteFile = new File(fileOutput);
            outputFinalCompleteFile = outputCompleteFile;
            if (!fileOutput.equals(finalFileOutput)) {
                outputFinalCompleteFile = new File(finalFileOutput);
            }
            outputPartFile = new File(downloadable.getFileOutputPart());
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
                run();
                return onDownloadReady();
            } finally {
                try {
                    downloadable.free(reservation);
                } catch (final Throwable e) {
                    logger.log(e);
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
        } finally {
            downloadable.unlockFiles(outputCompleteFile, outputFinalCompleteFile, outputPartFile);
            cleanupDownladInterface();
        }
    }

    protected void error(PluginException pluginException) {
        synchronized (this) {
            /* if we recieved external stop, then we dont have to handle errors */
            if (externalDownloadStop()) {
                return;
            }
            logger.log(pluginException);
            if (caughtPluginException == null) {
                caughtPluginException = pluginException;
            }
        }
        terminate();
    }

    protected boolean onDownloadReady() throws Exception {
        cleanupDownladInterface();
        if (handleErrors(outputPartFile) == false) {
            return false;
        } else {
            final boolean renameOkay = finalizeDownload(outputPartFile, outputCompleteFile, lastModified);
            // last modofied date noch setzen
            // final Date last = TimeFormatter.parseDateString(connection.getHeaderField("Last-Modified"));
            // if (last != null && JsonConfig.create(GeneralSettings.class).isUseOriginalLastModified()) {
            // /* set original lastModified timestamp */
            // outputCompleteFile.setLastModified(last.getTime());
            if (!renameOkay) {
                error(new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, _JDT.T.system_download_errors_couldnotrename(), LinkStatus.VALUE_LOCAL_IO_ERROR));
                return false;
            } else {
                return true;
            }
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

    protected void checkComplete(final List<Segment> segments, final long fileSize) throws PluginException {
        for (final Segment segment : segments) {
            if (!segment.isLoaded()) {
                // ignore index>0 as it is not supported yet
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Segment:" + segment.getUrl() + " not loaded");
            }
        }
        if (downloadable.getVerifiedFileSize() > 0 && downloadable.getVerifiedFileSize() != fileSize) {
            throw new PluginException(LinkStatus.ERROR_DOWNLOAD_INCOMPLETE, "Verified:" + downloadable.getVerifiedFileSize() + " != Filesize:" + fileSize);
        }
    }

    protected boolean handleErrors(final File file) throws PluginException {
        if (externalDownloadStop()) {
            return false;
        } else {
            if (caughtPluginException == null) {
                if (!file.isFile()) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    final long fileSize = file.length();
                    checkComplete(segments, fileSize);
                    downloadable.setDownloadBytesLoaded(fileSize);
                    downloadable.setVerifiedFileSize(fileSize);
                    downloadable.setLinkStatus(LinkStatus.FINISHED);
                    return true;
                }
            } else {
                throw caughtPluginException;
            }
        }
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
        return currentConnection;
    }

    @Override
    public void stopDownload() {
        if (abort.getAndSet(true) == false) {
            logger.info("externalStop recieved");
            terminate();
        }
        final URLConnectionAdapter lCurrentConnection = currentConnection;
        if (lCurrentConnection != null) {
            lCurrentConnection.disconnect();
        }
    }

    private final AtomicBoolean abort      = new AtomicBoolean(false);
    private final AtomicBoolean terminated = new AtomicBoolean(false);

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
        final URLConnectionAdapter lCurrentConnection = currentConnection;
        if (lCurrentConnection != null) {
            lCurrentConnection.disconnect();
        }
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

package org.jdownloader.downloader.segment;

import java.awt.Color;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.controlling.downloadcontroller.DiskSpaceReservation;
import jd.controlling.downloadcontroller.ExceptionRunnable;
import jd.controlling.downloadcontroller.FileIsLockedException;
import jd.controlling.downloadcontroller.ManagedThrottledConnectionHandler;
import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.http.requests.GetRequest;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.DownloadLinkDownloadable;
import jd.plugins.download.Downloadable;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.NullInputStream;
import org.appwork.utils.net.throttledconnection.MeteredThrottledInputStream;
import org.appwork.utils.speedmeter.AverageSpeedMeter;
import org.jdownloader.plugins.DownloadPluginProgress;
import org.jdownloader.plugins.SkipReason;
import org.jdownloader.plugins.SkipReasonException;
import org.jdownloader.translate._JDT;

//http://tools.ietf.org/html/draft-pantos-http-live-streaming-13
public class SegmentDownloader extends DownloadInterface {

    private volatile long                     bytesWritten = 0l;
    private Downloadable                      downloadable;
    private final DownloadLink                link;
    private long                              startTimeStamp;
    private final LogInterface                logger;
    private volatile URLConnectionAdapter     currentConnection;
    private ManagedThrottledConnectionHandler connectionHandler;
    private File                              outputCompleteFile;
    private File                              outputFinalCompleteFile;
    private File                              outputPartFile;

    private PluginException                   caughtPluginException;

    private final Browser                     obr;

    private final List<Segment>               segments     = new ArrayList<Segment>();

    public SegmentDownloader(final DownloadLink link, Downloadable dashDownloadable, Browser br2, String baseUrl, String[] segments) {
        for (final String segment : segments) {
            if (StringUtils.startsWithCaseInsensitive(segment, "http://") || StringUtils.startsWithCaseInsensitive(segment, "https://")) {
                this.segments.add(new Segment(segment));
            } else {
                this.segments.add(new Segment(baseUrl, segment));
            }
        }
        this.downloadable = dashDownloadable;
        this.obr = br2.cloneBrowser();
        this.link = link;
        logger = initLogger(link);
    }

    public LogInterface initLogger(final DownloadLink link) {
        PluginForHost plg = link.getLivePlugin();
        if (plg == null) {
            plg = link.getDefaultPlugin();
        }
        return plg == null ? null : plg.getLogger();
    }

    protected void terminate() {
        if (terminated.getAndSet(true) == false) {
            if (!externalDownloadStop()) {
                if (logger != null) {
                    logger.severe("A critical Downloaderror occured. Terminate...");
                }
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

    public void run() throws Exception {
        link.setDownloadSize(-1);
        final MeteredThrottledInputStream meteredThrottledInputStream = new MeteredThrottledInputStream(new NullInputStream(), new AverageSpeedMeter(10));
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(new File(downloadable.getFileOutputPart()));
        } catch (IOException e) {
            throw new SkipReasonException(SkipReason.INVALID_DESTINATION, e);
        }
        boolean localIO = false;
        try {
            final String cust = link.getCustomExtension();
            link.setCustomExtension(null);
            link.setCustomExtension(cust);
            final byte[] readWriteBuffer = new byte[512 * 1024];
            if (connectionHandler != null) {
                connectionHandler.addThrottledConnection(meteredThrottledInputStream);
            }
            for (final Segment seg : segments) {
                final Browser br = obr.cloneBrowser();
                final Request getRequest = createSegmentRequest(seg);
                try {
                    currentConnection = br.openRequestConnection(getRequest);
                    meteredThrottledInputStream.setInputStream(currentConnection.getInputStream());
                    while (true) {
                        final int len = meteredThrottledInputStream.read(readWriteBuffer);
                        if (len > 0) {
                            localIO = true;
                            outputStream.write(readWriteBuffer, 0, len);
                            localIO = false;
                            bytesWritten += len;
                            downloadable.setDownloadBytesLoaded(bytesWritten);
                        } else if (len == -1) {
                            break;
                        }
                    }
                } finally {
                    if (currentConnection != null) {
                        currentConnection.disconnect();
                    }
                    currentConnection = null;
                }
            }
        } catch (IOException e) {
            if (localIO) {
                throw new SkipReasonException(SkipReason.DISK_FULL);
            }
            throw e;
        } catch (Throwable e) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, e.getMessage());
        } finally {
            close(outputStream);
            if (connectionHandler != null && meteredThrottledInputStream != null) {
                connectionHandler.removeThrottledConnection(meteredThrottledInputStream);
            }
        }
    }

    private GetRequest createSegmentRequest(Segment seg) throws IOException {
        return new GetRequest(seg.getUrl());
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
            connectionHandler = new ManagedThrottledConnectionHandler();
            if (downloadable == null) {
                downloadable = new DownloadLinkDownloadable(link) {
                    @Override
                    public boolean isResumable() {
                        return false;
                    }

                    @Override
                    public void setResumeable(boolean value) {
                        // link.setProperty("RESUME", value);
                        super.setResumeable(value);
                    }
                };
            }
            downloadable.setDownloadInterface(this);

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
                run();
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
            onDownloadReady();
            return handleErrors();
            // } catch (Throwable e) {
            // e.printStackTrace();
            // return false;
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
            LogSource.exception(logger, pluginException);
            if (caughtPluginException == null) {
                caughtPluginException = pluginException;
            }
        }
        terminate();
    }

    protected void onDownloadReady() throws Exception {

        cleanupDownladInterface();
        if (!handleErrors()) {
            return;
        }
        // link.setVerifiedFileSize(bytesWritten);

        boolean renameOkay = downloadable.rename(outputPartFile, outputCompleteFile);
        if (!renameOkay) {

            error(new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, _JDT.T.system_download_errors_couldnotrename(), LinkStatus.VALUE_LOCAL_IO_ERROR));
        }

    }

    protected void cleanupDownladInterface() {
    }

    private boolean handleErrors() throws PluginException {
        if (externalDownloadStop()) {
            return false;
        }
        if (caughtPluginException == null) {
            downloadable.setLinkStatus(LinkStatus.FINISHED);
            downloadable.setVerifiedFileSize(outputCompleteFile.length());
            return true;
        } else {
            throw caughtPluginException;
        }
    }

    private void createOutputChannel() throws SkipReasonException {
        try {
            final String fileOutput = downloadable.getFileOutput();
            if (logger != null) {
                logger.info("createOutputChannel for " + fileOutput);
            }
            final String finalFileOutput = downloadable.getFinalFileOutput();
            outputCompleteFile = new File(fileOutput);
            outputFinalCompleteFile = outputCompleteFile;
            if (!fileOutput.equals(finalFileOutput)) {
                outputFinalCompleteFile = new File(finalFileOutput);
            }
        } catch (Exception e) {
            LogSource.exception(logger, e);
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
            if (logger != null) {
                logger.info("externalStop recieved");
            }
            terminate();
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

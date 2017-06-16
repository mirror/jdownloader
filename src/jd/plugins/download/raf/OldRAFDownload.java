package jd.plugins.download.raf;

//jDownloader - Downloadmanager
//Copyright (C) 2008  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.
import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import jd.controlling.downloadcontroller.DiskSpaceReservation;
import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.ExceptionRunnable;
import jd.controlling.downloadcontroller.FileIsLockedException;
import jd.controlling.downloadcontroller.ManagedThrottledConnectionHandler;
import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginProgress;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.DownloadLinkDownloadable;
import jd.plugins.download.Downloadable;
import jd.plugins.download.HashInfo;
import jd.plugins.download.HashResult;
import jd.plugins.download.SparseFile;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Application;
import org.appwork.utils.Exceptions;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.plugins.DownloadPluginProgress;
import org.jdownloader.plugins.HashCheckPluginProgress;
import org.jdownloader.plugins.SkipReason;
import org.jdownloader.plugins.SkipReasonException;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.translate._JDT;
import org.jdownloader.updatev2.InternetConnectionSettings;

public class OldRAFDownload extends DownloadInterface {
    private final AtomicReference<RandomAccessFile> outputPartFileRaf        = new AtomicReference<RandomAccessFile>(null);
    private volatile File                           outputCompleteFile;
    private volatile File                           outputFinalCompleteFile;
    private volatile File                           outputPartFile;
    private final AtomicBoolean                     connected                = new AtomicBoolean(false);
    private final CopyOnWriteArrayList<RAFChunk>    chunks                   = new CopyOnWriteArrayList<RAFChunk>();
    protected volatile long                         totalLinkBytesLoaded     = 0;
    protected AtomicLong                            totalLinkBytesLoadedLive = new AtomicLong(0);
    private int                                     readTimeout              = 100000;
    private int                                     requestTimeout           = 100000;
    private final AtomicBoolean                     terminated               = new AtomicBoolean(false);
    private final AtomicBoolean                     abort                    = new AtomicBoolean(false);
    protected int                                   chunkNum                 = 1;
    private boolean                                 resume                   = false;
    protected boolean                               dlAlreadyFinished        = false;
    protected Browser                               browser;
    protected URLConnectionAdapter                  connection;
    protected Downloadable                          downloadable;
    protected PluginException                       caughtPluginException    = null;
    public LogInterface                             logger;
    public static final String                      PROPERTY_DOFILESIZECHECK = "DOFILESIZECHECK";
    protected Request                               request                  = null;
    protected ManagedThrottledConnectionHandler     connectionHandler        = null;
    private long                                    startTimeStamp           = -1;
    private boolean                                 resumedDownload;

    /**
     * Gibt die Anzahl der Chunks an die dieser Download verwenden soll. Chu8nks koennen nur vor dem Downloadstart gesetzt werden!
     */
    public void setChunkNum(int num) {
        if (num <= 0) {
            logger.severe("Chunks value must be >=1");
            return;
        }
        chunkNum = num;
    }

    public boolean isResumable() {
        return downloadable.isResumable();
    }

    @Override
    public ManagedThrottledConnectionHandler getManagedConnetionHandler() {
        return connectionHandler;
    }

    /**
     * Ist resume aktiv?
     */
    public boolean isRangeRequestSupported() {
        return resume;
    }

    public OldRAFDownload(Downloadable downloadLink, Request request) throws IOException, PluginException {
        init(downloadLink, request);
    }

    protected void init(Downloadable downloadLink, Request request) {
        connectionHandler = new ManagedThrottledConnectionHandler();
        this.downloadable = downloadLink;
        logger = downloadLink.getLogger();
        browser = downloadLink.getContextBrowser();
        InternetConnectionSettings config = JsonConfig.create(InternetConnectionSettings.PATH, InternetConnectionSettings.class);
        requestTimeout = config.getHttpConnectTimeout();
        readTimeout = config.getHttpReadTimeout();
        this.request = request;
        /* setDownloadInstance after all variables are set! */
        downloadLink.setDownloadInterface(this);
    }

    /**
     * File soll resumed werden
     */
    public void setResume(boolean value) {
        resume = value;
        if (value && !checkResumabled()) {
            logger.warning("Resumepoint not valid");
        }
        downloadable.setResumeable(value);
    }

    /**
     * @return the startTimeStamp
     */
    public long getStartTimeStamp() {
        return startTimeStamp;
    }

    @Override
    public URLConnectionAdapter connect(Browser br) throws Exception {
        /* reset timeouts here, because it can be they got not set yet */
        setReadTimeout(br.getReadTimeout());
        setRequestTimeout(br.getConnectTimeout());
        request.setConnectTimeout(getRequestTimeout());
        request.setReadTimeout(getReadTimeout());
        br.setRequest(request);
        URLConnectionAdapter ret = connect();
        return ret;
    }

    public URLConnectionAdapter connect() throws Exception {
        if (connected.getAndSet(true) == true) {
            throw new IllegalStateException("Already connected");
        }
        logger.finer("Connect...");
        if (request == null) {
            throw new IllegalStateException("Wrong Mode. Instance is in direct Connection mode");
        }
        boolean resumed = false;
        if (this.isRangeRequestSupported() && this.checkResumabled()) {
            /* we can continue to resume the download */
            logger.finer(".....connectResumable");
            resumed = connectResumable();
        } else {
            long verifiedFileSize = downloadable.getVerifiedFileSize();
            if (verifiedFileSize > 0 && getChunkNum() > 1) {
                /* check if we have to adapt the number of chunks */
                int tmp = Math.min(Math.max(1, (int) (verifiedFileSize / RAFChunk.MIN_CHUNKSIZE)), getChunkNum());
                if (tmp != getChunkNum()) {
                    logger.finer("Corrected Chunknum: " + getChunkNum() + " -->" + tmp);
                    setChunkNum(tmp);
                }
            }
            if (this.isRangeRequestSupported()) {
                /* range requests are supported! */
                logger.finer(".....connectFirstRange");
                connectFirstRange();
            } else {
                logger.finer(".....connectRangeless");
                /* our connection happens rangeless */
                request.getHeaders().remove("Range");
                /* Workaround for rayfile.com */
                if (this.downloadable.isServerComaptibleForByteRangeRequest()) {
                    if ("rayfile.com".contains(this.downloadable.getHost())) {
                        request.getHeaders().put("Range", "bytes=" + (0) + "-");
                    }
                }
                browser.openRequestConnection(request, false);
            }
        }
        if (downloadable.isDebug()) {
            logger.finest("\r\n" + request.printHeaders());
        }
        connection = request.getHttpConnection();
        if (request.getLocation() != null) {
            throw new PluginException(LinkStatus.ERROR_DOWNLOAD_INCOMPLETE, BrowserAdapter.ERROR_REDIRECTED);
        }
        if (downloadable.getVerifiedFileSize() < 0) {
            /* only set DownloadSize if we do not have a verified FileSize yet */
            String contentType = connection.getContentType();
            boolean trustFileSize = contentType == null || (!contentType.contains("html") && !contentType.contains("text"));
            logger.info("Trust FileSize: " + trustFileSize + " " + contentType);
            if (connection.getRange() != null && trustFileSize) {
                /* we have a range response, let's use it */
                if (connection.getRange()[2] > 0) {
                    this.downloadable.setDownloadTotalBytes(connection.getRange()[2]);
                }
            } else if (resumed == false && connection.getLongContentLength() > 0 && connection.isOK() && trustFileSize) {
                this.downloadable.setDownloadTotalBytes(connection.getLongContentLength());
            }
        }
        if (connection.getResponseCode() == 416 && resumed == true && downloadable.getChunksProgress() != null && downloadable.getChunksProgress().length == 1 && downloadable.getVerifiedFileSize() == downloadable.getChunksProgress()[0] + 1) {
            logger.info("Faking Content-Disposition for already finished downloads");
            /* we requested a finished loaded file, got 416 and content-range with * and one chunk only */
            /* we fake a content disposition connection so plugins work normal */
            if (connection.isContentDisposition() == false) {
                List<String> list = new ArrayList<String>();
                list.add("fakeContent");
                connection.getHeaderFields().put("Content-Disposition", list);
            }
            List<String> list = new ArrayList<String>();
            list.add("application/octet-stream");
            connection.getHeaderFields().put("Content-Type", list);
            dlAlreadyFinished = true;
        }
        return connection;
    }

    /**
     * Gibt den aktuellen readtimeout zurueck
     */
    public int getReadTimeout() {
        return Math.max(10000, readTimeout);
    }

    /**
     * Gibt den requesttimeout zurueck
     */
    public int getRequestTimeout() {
        return Math.max(10000, requestTimeout);
    }

    /**
     * Validiert das Chunk Progress array
     */
    protected boolean checkResumabled() {
        long[] chunksP = downloadable.getChunksProgress();
        if (chunksP == null || chunksP.length == 0 || (chunksP.length == 1 && chunksP[0] == 0)) {
            return false;
        }
        final long fileSize = getFileSize();
        int chunks = chunksP.length;
        final long part = fileSize / chunks;
        long dif;
        long last = -1;
        logger.info("FileSize: " + fileSize + " Chunks: " + chunks + " PartSize: " + part);
        for (int i = 0; i < chunks; i++) {
            dif = chunksP[i] - i * part;
            if (dif < 0) {
                logger.info("Invalid Chunk " + i + ": " + chunksP[i] + " dif= " + dif);
                return false;
            }
            if (chunksP[i] <= last) {
                logger.info("Invalid Chunk " + i + ": " + chunksP[i] + " <= " + last);
                return false;
            }
            if (chunksP[i] >= (i + 1) * part - 1) {
                logger.info("Fix Chunk " + i + ": " + chunksP[i] + " to " + (((i + 1) * part) - 1));
                chunksP[i] = Math.max(0, ((i + 1) * part) - 1024);
            } else {
                logger.info("Valid Chunk " + i + ": " + chunksP[i]);
            }
            last = chunksP[i];
        }
        downloadable.setChunksProgress(chunksP);
        if (chunks > 0) {
            if (chunks <= this.getChunkNum()) {
                /* downloadchunks are less or equal to allowed chunks */
                setChunkNum(chunks);
            } else {
                /*
                 * downloadchunks are more than allowed chunks, need to repartition the download
                 */
                logger.info("Download has " + chunks + " Chunks but only " + getChunkNum() + " allowed! Change to 1!");
                setChunkNum(1);
                downloadable.setChunksProgress(new long[] { chunksP[0] });
            }
            return true;
        }
        return false;
    }

    protected void connectFirstRange() throws IOException {
        long fileSize = getFileSize();
        long part = fileSize;
        boolean verifiedSize = downloadable.getVerifiedFileSize() > 0;
        boolean openRangeRequested = false;
        boolean rangeRequested = false;
        final boolean preferOpenRangeFirst;
        if (downloadable instanceof DownloadLinkDownloadable) {
            preferOpenRangeFirst = ((DownloadLinkDownloadable) downloadable).getDownloadLink().getBooleanProperty("oldraf_preferOpenRangeFirst", Boolean.TRUE);
        } else {
            preferOpenRangeFirst = true;
        }
        if (preferOpenRangeFirst || verifiedSize == false || this.getChunkNum() == 1) {
            /* we only request a single range */
            openRangeRequested = true;
            /* Workaround for server responses != 206 */
            if (this.downloadable.isServerComaptibleForByteRangeRequest()) {
                rangeRequested = true;
                request.getHeaders().put("Range", "bytes=" + (0) + "-");
            }
        } else {
            /* we request multiple ranges */
            openRangeRequested = false;
            rangeRequested = true;
            part = fileSize / this.getChunkNum();
            request.getHeaders().put("Range", "bytes=" + (0) + "-" + (part - 1));
        }
        browser.openRequestConnection(request, false);
        if (request.getHttpConnection().getResponseCode() == 416) {
            logger.warning("HTTP/1.1 416 Requested Range Not Satisfiable");
            if (downloadable.isDebug()) {
                logger.finest("\r\n" + request.printHeaders());
            }
            throw new IllegalStateException("HTTP/1.1 416 Requested Range Not Satisfiable");
        } else if (request.getHttpConnection().getRange() == null) {
            if (openRangeRequested && rangeRequested == false) {
                logger.warning("FirstRange was openRange without any RangeRequest!");
            } else {
                logger.warning("No Chunkload");
                setChunkNum(1);
            }
        } else {
            long[] range = request.getHttpConnection().getRange();
            if (range[0] != 0) {
                /* first range MUST start at zero */
                throw new IllegalStateException("Range Error. Requested " + request.getHeaders().get("Range") + ". Got range: " + request.getHttpConnection().getHeaderField("Content-Range"));
            } else if (verifiedSize && range[1] < (part - 1)) {
                /* response range != requested range */
                throw new IllegalStateException("Range Error. Requested " + request.getHeaders().get("Range") + " Got range: " + request.getHttpConnection().getHeaderField("Content-Range"));
            } else if (!openRangeRequested && range[1] == range[2] - 1 && getChunkNum() > 1) {
                logger.warning(" Chunkload Protection.. Requested " + request.getHeaders().get("Range") + " Got range: " + request.getHttpConnection().getHeaderField("Content-Range"));
                setChunkNum(1);
            } else if (verifiedSize && range[1] > (part - 1)) {
                /* response range is bigger than requested range */
                if (verifiedSize && range[1] == part) {
                    logger.severe("Workaround for buggy http server: rangeEND=contentEND, it must be rangeEND-1=contentEND as 0 is first byte!");
                    return;
                }
                if (request.getHttpConnection().getResponseCode() == 200 && rangeRequested == false && verifiedSize && fileSize == range[2]) {
                    logger.severe("Workaround for buggy http server: no range requested, but got content-range response with 200 header");
                    return;
                }
                throw new IllegalStateException("Range Error. Requested " + request.getHeaders().get("Range") + " Got range: " + request.getHttpConnection().getHeaderField("Content-Range"));
            }
        }
    }

    /**
     * Setzt im Downloadlink und PLugin die entsprechende Fehlerids
     */
    public boolean handleErrors() throws PluginException {
        final boolean isExternalStop = externalDownloadStop();
        final long verifiedFileSize = getVerifiedFileSize();
        final long fileSize = getFileSize();
        logger.severe("ExternalStop: " + isExternalStop + "|VerifiedFilesize: " + verifiedFileSize + "|FileSize: " + fileSize + "|Loaded: " + totalLinkBytesLoaded);
        if (verifiedFileSize >= 0) {
            if (totalLinkBytesLoaded >= verifiedFileSize) {
                logger.severe("VerifiedFilesize: " + verifiedFileSize + " Loaded: " + totalLinkBytesLoaded);
                downloadable.setLinkStatus(LinkStatus.FINISHED);
                return true;
            }
            if (isExternalStop) {
                return false;
            }
            if (caughtPluginException != null) {
                throw caughtPluginException;
            }
            throw new PluginException(LinkStatus.ERROR_DOWNLOAD_INCOMPLETE, _JDT.T.download_error_message_incomplete());
        }
        if (fileSize >= 0) {
            if (totalLinkBytesLoaded >= fileSize || isExternalStop == false && caughtPluginException == null) {
                logger.severe("Filesize: " + fileSize + " Loaded: " + totalLinkBytesLoaded);
                downloadable.setLinkStatus(LinkStatus.FINISHED);
                return true;
            }
            if (isExternalStop) {
                return false;
            }
            if (caughtPluginException != null) {
                throw caughtPluginException;
            }
            throw new PluginException(LinkStatus.ERROR_DOWNLOAD_INCOMPLETE, _JDT.T.download_error_message_incomplete());
        }
        if (externalDownloadStop()) {
            return false;
        } else if (caughtPluginException == null) {
            downloadable.setLinkStatus(LinkStatus.FINISHED);
            return true;
        } else {
            throw caughtPluginException;
        }
    }

    /**
     * ueber error() kann ein fehler gemeldet werden. DIe Methode entscheided dann ob dieser fehler zu einem Abbruch fuehren muss
     */
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
        terminate(false);
    }

    /**
     * Wartet bis alle Chunks fertig sind, aktuelisiert den downloadlink regelmaesig und fordert beim Controller eine aktualisierung des
     * links an
     */
    protected void onChunkFinished(RAFChunk chunk) {
        synchronized (chunks) {
            chunks.notifyAll();
        }
    }

    protected synchronized boolean writeBytes(RAFChunk chunk) {
        return writeChunkBytes(chunk);
    }

    /**
     * Startet den Download. Nach dem Aufruf dieser Funktion koennen keine Downlaodparameter mehr gesetzt werden bzw bleiben wirkungslos.
     *
     * @return
     * @throws Exception
     */
    public boolean startDownload() throws Exception {
        try {
            try {
                downloadable.validateLastChallengeResponse();
            } catch (final Throwable e) {
                LogSource.exception(logger, e);
            }
            logger.finer("Start Download");
            if (this.dlAlreadyFinished == true) {
                downloadable.setAvailable(AvailableStatus.TRUE);
                logger.finer("DownloadAlreadyFinished workaround");
                downloadable.setLinkStatus(LinkStatus.FINISHED);
                return true;
            }
            if (connected.get() == false) {
                connect();
            }
            if (connection != null) {
                final String contentEncoding = connection.getHeaderField("Content-Encoding");
                final long[] responseRange = connection.getRange();
                boolean skipVerifyFileSizeCheck = true;
                if (downloadable instanceof DownloadLinkDownloadable) {
                    final String displayName = ((DownloadLinkDownloadable) downloadable).getDownloadLinkController().getProcessingPlugin().getLazyP().getDisplayName();
                    if (StringUtils.equalsIgnoreCase(displayName, "directhttp") || StringUtils.equalsIgnoreCase(displayName, "http links")) {
                        skipVerifyFileSizeCheck = false;
                    }
                }
                final long verifiedFileSize = downloadable.getVerifiedFileSize();
                final boolean verifiedFileSizeNotAvailable = verifiedFileSize < 0;
                if (StringUtils.containsIgnoreCase(contentEncoding, "gzip") && (verifiedFileSizeNotAvailable || skipVerifyFileSizeCheck)) {
                    /* GZIP Encoding kann weder chunk noch resume */
                    /* hier dann auch den final filesize check prüfen */
                    logger.info("Content-Encoding: 'gzip' detected! Disable Resume/Chunks because " + verifiedFileSizeNotAvailable + "|" + skipVerifyFileSizeCheck);
                    setResume(false);
                    setChunkNum(1);
                }
                final String transferEncoding = connection.getHeaderField("Transfer-Encoding");
                if (false && StringUtils.containsIgnoreCase(transferEncoding, "chunked") && (verifiedFileSizeNotAvailable || skipVerifyFileSizeCheck)) {
                    setChunkNum(1);
                    if (responseRange != null && connection.isOK()) {
                        logger.info("Transfer-Encoding: 'chunked' detected! 'Set Chunks=1' because (" + verifiedFileSizeNotAvailable + "|" + skipVerifyFileSizeCheck + ") and ResponseCode:" + connection.getResponseCode() + "|Range:" + Arrays.toString(responseRange));
                    } else {
                        logger.info("Transfer-Encoding: 'chunked' detected! 'Set Chunks=1/Disable Resume' because (" + verifiedFileSizeNotAvailable + "|" + skipVerifyFileSizeCheck + ")");
                        setResume(false);
                    }
                }
            }
            // Erst hier Dateinamen holen, somit umgeht man das Problem das bei
            // mehrfachAufruf von connect entstehen kann
            if (this.downloadable.getFinalFileName() == null && ((connection != null && connection.isContentDisposition()) || this.allowFilenameFromURL)) {
                String name = Plugin.getFileNameFromHeader(connection);
                if (this.fixWrongContentDispositionHeader) {
                    this.downloadable.setFinalFileName(Encoding.htmlDecode(name));
                } else {
                    this.downloadable.setFinalFileName(name);
                }
            }
            if (connection == null || !connection.isOK()) {
                if (connection != null) {
                    logger.finest(connection.toString());
                }
                try {
                    connection.disconnect();
                } catch (final Throwable e) {
                }
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
            }
            if (connection.getRequest().getLocation() != null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (downloadable.getVerifiedFileSize() < 0) {
                /* we don't have a verified filesize yet, let's check if we have it now! */
                if (connection.getRange() != null) {
                    if (connection.getRange()[2] >= 0) {
                        downloadable.setVerifiedFileSize(connection.getRange()[2]);
                    }
                } else if (connection.getRequestProperty("Range") == null && connection.getLongContentLength() >= 0 && connection.isOK()) {
                    downloadable.setVerifiedFileSize(connection.getLongContentLength());
                }
            }
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
                    setupChunks();
                    /* download in progress so file should be online ;) */
                    downloadable.setAvailable(AvailableStatus.TRUE);
                    waitForChunks();
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
                HashResult result = onChunksReady();
                if (result != null) {
                    logger.info(result.getHashInfo().getType() + "-Check: " + (result.match() ? "ok" : "failed"));
                    if (result.match()) {
                        downloadable.setLinkStatusText(_JDT.T.system_download_doCRC2_success(result.getHashInfo().getType()));
                    } else {
                        throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, _JDT.T.system_download_doCRC2_failed(result.getHashInfo().getType()));
                    }
                }
                return handleErrors();
            } finally {
                downloadable.unlockFiles(outputCompleteFile, outputFinalCompleteFile, outputPartFile);
                cleanupDownladInterface();
            }
        } catch (PluginException e) {
            error(e);
            throw e;
        } catch (Exception e) {
            if (e instanceof FileNotFoundException) {
                this.error(new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, _JDT.T.download_error_message_localio(e.getMessage()), LinkStatus.VALUE_LOCAL_IO_ERROR));
            } else {
                LogSource.exception(logger, e);
            }
            throw e;
        } finally {
            cleanupDownladInterface();
        }
    }

    /**
     * Gibt die Anzahl der verwendeten Chunks zurueck
     */
    public int getChunkNum() {
        return chunkNum;
    }

    /**
     * Gibt eine bestmoegliche abschaetzung der Dateigroesse zurueck
     */
    protected long getFileSize() {
        final long size = getVerifiedFileSize();
        if (size >= 0) {
            return size;
        }
        if (connection != null) {
            if (connection.getRange() != null) {
                /* we have a range response, let's use it */
                if (connection.getRange()[2] > 0) {
                    return connection.getRange()[2];
                }
            }
            if (connection.getRequestProperty("Range") == null && connection.getLongContentLength() > 0 && connection.isOK()) {
                /* we have no range request and connection is okay, so we can use the content-length */
                return connection.getLongContentLength();
            }
        }
        if (downloadable.getDownloadTotalBytes() > 0) {
            return downloadable.getDownloadTotalBytes();
        }
        return -1;
    }

    protected long getVerifiedFileSize() {
        return downloadable.getVerifiedFileSize();
    }

    public Request getRequest() {
        return this.request;
    }

    @Override
    public URLConnectionAdapter getConnection() {
        return this.connection;
    }

    @Override
    public boolean externalDownloadStop() {
        return abort.get();
    }

    /** signal that we stopped download external */
    public void stopDownload() {
        if (abort.getAndSet(true) == false) {
            logger.info("externalStop recieved");
            terminate(false);
        }
    }

    protected void waitForChunks() {
        try {
            logger.finer("Wait for chunks");
            mainLoop: while (true) {
                try {
                    synchronized (chunks) {
                        for (RAFChunk chunk : chunks) {
                            if (chunk.isAlive() && chunk.isRunning()) {
                                chunks.wait(1000);
                                continue mainLoop;
                            }
                        }
                    }
                    break;
                } catch (InterruptedException e) {
                    terminate(true);
                }
            }
            /* set the *real loaded* bytes here */
            downloadable.setDownloadBytesLoaded(totalLinkBytesLoaded);
        } catch (Throwable e) {
            LogSource.exception(logger, e);
        }
    }

    /**
     * terminate this DownloadInterface, abort all running chunks
     */
    protected void terminate(boolean forcedWaiting) {
        if (terminated.getAndSet(true) == false || forcedWaiting) {
            if (!externalDownloadStop()) {
                logger.severe("A critical Downloaderror occured. Terminate...");
            }
            while (true) {
                synchronized (chunks) {
                    boolean wait = false;
                    for (RAFChunk chunk : chunks) {
                        if (chunk.isRunning()) {
                            chunk.closeConnections();
                            if (chunk == Thread.currentThread()) {
                                /**
                                 * a thread should not wait for its own death :)
                                 */
                                continue;
                            } else {
                                wait = true;
                            }
                        }
                    }
                    if (wait) {
                        try {
                            chunks.wait(1000);
                        } catch (InterruptedException e) {
                            LogSource.exception(logger, e);
                            break;
                        }
                    } else {
                        break;
                    }
                }
            }
        }
    }

    protected boolean connectResumable() throws IOException {
        // TODO: endrange pruefen
        long[] chunkProgress = downloadable.getChunksProgress();
        final long verifiedFileSize = downloadable.getVerifiedFileSize();
        final long fileSize = getFileSize();
        String start = "";
        String end = "";
        boolean rangeRequested = false;
        logger.info("chunksProgress: " + Arrays.toString(chunkProgress));
        if (chunkProgress[0] == 0) {
            start = Long.toString(0);
        } else {
            start = Long.toString(chunkProgress[0] + 1);
        }
        if (verifiedFileSize > 0) {
            if (chunkProgress.length == 1) {
                // end = Long.toString(verifiedFileSize - 1);
                end = ""; // prefer open end
            } else {
                end = Long.toString(verifiedFileSize / chunkProgress.length);
            }
            logger.info("VerifiedFileSize: " + verifiedFileSize + "|Start:" + start + "|End:" + end);
        } else {
            if (chunkProgress.length == 1) {
                end = "";// open end
            } else {
                end = Long.toString(chunkProgress[1] + 1);// overlap by 1
            }
            logger.info("FileSize: " + fileSize + "|Start:" + start + "|End:" + end);
        }
        if (start.equals("0") && (verifiedFileSize < 0 || "".equals(end))) {
            logger.info("rangeless resumable connect");
            rangeRequested = false;
            request.getHeaders().remove("Range");
        } else {
            rangeRequested = true;
            if (start.equalsIgnoreCase(end)) {
                logger.info("WTF, start equals end! Workaround: maybe manipulating the startRange?! it's about time for new downloadcore!");
            }
            request.getHeaders().put("Range", "bytes=" + start + "-" + end);
        }
        browser.openRequestConnection(request, false);
        return rangeRequested;
    }

    /**
     * Setzt den aktuellen readtimeout(nur vor dem dl start)
     */
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    /**
     * Setzt vor ! dem download dden requesttimeout. Sollte nicht zu niedrig sein weil sonst das automatische kopieren der Connections fehl
     * schlaegt.,
     */
    public void setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public static final ArrayList<AtomicBoolean> HASHCHECK_QEUEU = new ArrayList<AtomicBoolean>();

    protected HashResult getHashResult(File file) throws InterruptedException {
        if (JsonConfig.create(GeneralSettings.class).isHashCheckEnabled() && downloadable.isHashCheckEnabled()) {
            AtomicBoolean hashCheckLock = new AtomicBoolean(false);
            synchronized (HASHCHECK_QEUEU) {
                HASHCHECK_QEUEU.add(hashCheckLock);
                hashCheckLock.set(HASHCHECK_QEUEU.indexOf(hashCheckLock) != 0);
            }
            try {
                if (hashCheckLock.get()) {
                    synchronized (hashCheckLock) {
                        if (hashCheckLock.get()) {
                            final PluginProgress hashProgress = new HashCheckPluginProgress(null, Color.YELLOW.darker().darker(), null);
                            try {
                                downloadable.addPluginProgress(hashProgress);
                                hashCheckLock.wait();
                            } finally {
                                downloadable.removePluginProgress(hashProgress);
                            }
                        }
                    }
                }
                HashInfo hashInfo = downloadable.getHashInfo();
                HashResult hashResult = downloadable.getHashResult(hashInfo, file);
                if (hashResult != null) {
                    logger.info(hashResult.toString());
                }
                return hashResult;
            } finally {
                synchronized (HASHCHECK_QEUEU) {
                    boolean callNext = HASHCHECK_QEUEU.indexOf(hashCheckLock) == 0;
                    HASHCHECK_QEUEU.remove(hashCheckLock);
                    if (HASHCHECK_QEUEU.size() > 0 && callNext) {
                        hashCheckLock = HASHCHECK_QEUEU.get(0);
                    } else {
                        hashCheckLock = null;
                    }
                }
                if (hashCheckLock != null) {
                    synchronized (hashCheckLock) {
                        hashCheckLock.set(false);
                        hashCheckLock.notifyAll();
                    }
                }
            }
        }
        return null;
    }

    protected HashResult onChunksReady() throws Exception {
        logger.info("Close connections if they are not closed yet");
        HashResult result = null;
        try {
            for (RAFChunk c : chunks) {
                c.closeConnections();
            }
        } finally {
            cleanupDownladInterface();
        }
        if (!handleErrors()) {
            return result;
        }
        final HashResult hashResult = getHashResult(outputPartFile);
        downloadable.setHashResult(hashResult);
        if (hashResult == null || hashResult.match()) {
            downloadable.setVerifiedFileSize(outputPartFile.length());
        } else {
            if (hashResult.getHashInfo().isTrustworthy()) {
                throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, _JDT.T.system_download_doCRC2_failed(hashResult.getHashInfo().getType()));
            }
        }
        boolean renameOkay = downloadable.rename(outputPartFile, outputCompleteFile);
        if (renameOkay) {
            /* save absolutepath as final location property */
            // downloadable.setFinalFileOutput(outputCompleteFile.getAbsolutePath());
            try {
                Date last = TimeFormatter.parseDateString(connection.getHeaderField("Last-Modified"));
                if (last != null && JsonConfig.create(GeneralSettings.class).isUseOriginalLastModified()) {
                    /* set original lastModified timestamp */
                    outputCompleteFile.setLastModified(last.getTime());
                } else {
                    /* set current timestamp as lastModified timestamp */
                    outputCompleteFile.setLastModified(System.currentTimeMillis());
                }
            } catch (final Throwable e) {
                LogSource.exception(logger, e);
            }
        } else {
            error(new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, _JDT.T.system_download_errors_couldnotrename(), LinkStatus.VALUE_LOCAL_IO_ERROR));
        }
        return result;
    }

    protected boolean checkResumeConnection() {
        final String requestedRange = connection.getRequestProperty("Range");
        if (requestedRange != null) {
            // range requested
            final long[] responseRange = connection.getRange();
            if (responseRange == null) {
                // no range response
                logger.info("RequestRange: " + requestedRange + "|No ResponseRange but connection has responseCode=" + connection.getResponseCode() + " and content-length=" + connection.getCompleteContentLength());
                setChunkNum(1);
                return false;
            } else {
                // range response
                logger.info("RequestRange: " + requestedRange + "|ResponseRange:" + Arrays.toString(responseRange));
                return true;
            }
        } else {
            return true;
        }
    }

    protected void setupChunks() throws Exception {
        try {
            if (isRangeRequestSupported() && checkResumabled() && checkResumeConnection()) {
                logger.finer("Setup resume");
                this.resumedDownload = true;
                this.setupResume();
            } else {
                logger.finer("Setup virgin download");
                this.resumedDownload = false;
                this.setupVirginStart();
            }
        } catch (Exception e) {
            throw e;
        }
    }

    public boolean isResumedDownload() {
        return resumedDownload;
    }

    private void setupVirginStart() throws FileNotFoundException {
        RAFChunk chunk;
        totalLinkBytesLoaded = 0;
        downloadable.setDownloadBytesLoaded(0);
        long fileSize = getFileSize();
        long partSize = 0;
        int chunks = getChunkNum();
        if (fileSize < 0) {
            /* unknown filesize handling */
            if (chunks > 1) {
                logger.warning("Unknown FileSize->reset chunks to 1 and start at beginning");
                chunks = 1;
            } else {
                logger.warning("Unknown FileSize->start at beginning");
            }
        } else if (fileSize == 0) {
            /* zero filesize handling */
            if (chunks > 1) {
                logger.warning("Zero FileSize->reset chunks to 1 and start at beginning");
                chunks = 1;
            } else {
                logger.warning("Zero FileSize->start at beginning");
            }
        } else {
            partSize = fileSize / chunks;
            if (connection.getRange() != null) {
                if ((connection.getRange()[1] == connection.getRange()[2] - 1) || (connection.getRange()[1] == connection.getRange()[2])) {
                    logger.warning("Chunkload protection. this may cause traffic errors");
                    partSize = fileSize / chunks;
                } else {
                    // Falls schon der 1. range angefordert wurde.... werden die
                    // restlichen chunks angepasst
                    partSize = (fileSize - connection.getLongContentLength()) / Math.max(1, (chunks - 1));
                }
            }
            if (partSize <= 0) {
                logger.warning("Filesize is " + fileSize + " but partSize is " + partSize + "-> reset chunks to 1");
                chunks = 1;
            }
        }
        setChunkNum(chunks);
        logger.finer("Start Download in " + chunks + " chunks. Chunksize: " + partSize);
        downloadable.setChunksProgress(new long[chunkNum]);
        int start = 0;
        long rangePosition = 0;
        int id = 0;
        if (connection.getRange() != null && connection.getRange()[1] != connection.getRange()[2] - 1) {
            // Erster range schon angefordert
            chunk = new RAFChunk(0, rangePosition = connection.getRange()[1], connection, this, downloadable, id++);
            rangePosition++;
            logger.finer("Setup chunk 0: " + chunk);
            addChunk(chunk);
            start++;
        }
        for (int i = start; i < chunks; i++) {
            if (i == chunks - 1) {
                chunk = new RAFChunk(rangePosition, -1, connection, this, downloadable, id++);
            } else {
                chunk = new RAFChunk(rangePosition, rangePosition + partSize - 1, connection, this, downloadable, id++);
                rangePosition = rangePosition + partSize;
            }
            logger.finer("Setup chunk " + i + ": " + chunk);
            addChunk(chunk);
        }
    }

    /**
     * Fuegt einen Chunk hinzu und startet diesen
     *
     * @param chunk
     */
    protected void addChunk(RAFChunk chunk) {
        synchronized (chunks) {
            chunks.add(chunk);
            chunk.startChunk();
        }
    }

    protected void addChunk(Chunk chunk) {
        throw new WTFException("This should not happen!");
    }

    protected boolean writeChunkBytes(final Chunk chunk) {
        throw new WTFException("This should not happen!");
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
            outputPartFileRaf.set(IO.open(outputPartFile, "rw"));
        } catch (Exception e) {
            LogSource.exception(logger, e);
            throw new SkipReasonException(SkipReason.INVALID_DESTINATION, e);
        }
    }

    private void setupResume() throws FileNotFoundException {
        long parts = getFileSize() / getChunkNum();
        logger.info("Resume: " + getFileSize() + " partsize: " + parts);
        RAFChunk chunk;
        int id = 0;
        for (int i = 0; i < getChunkNum(); i++) {
            if (i == getChunkNum() - 1) {
                chunk = new RAFChunk(downloadable.getChunksProgress()[i] == 0 ? 0 : downloadable.getChunksProgress()[i] + 1, -1, connection, this, downloadable, id++);
                chunk.setLoaded((downloadable.getChunksProgress()[i] - i * parts + 1));
            } else {
                chunk = new RAFChunk(downloadable.getChunksProgress()[i] == 0 ? 0 : downloadable.getChunksProgress()[i] + 1, (i + 1) * parts - 1, connection, this, downloadable, id++);
                chunk.setLoaded((downloadable.getChunksProgress()[i] - i * parts + 1));
            }
            logger.finer("Setup chunk " + i + ": " + chunk);
            addChunk(chunk);
        }
    }

    @Override
    public long getTotalLinkBytesLoadedLive() {
        return totalLinkBytesLoadedLive.get();
    }

    public synchronized void setTotalLinkBytesLoaded(long loaded) {
        totalLinkBytesLoaded = loaded;
        totalLinkBytesLoadedLive.set(loaded);
    }

    protected synchronized void addToTotalLinkBytesLoaded(long block, boolean updateLiveData) {
        totalLinkBytesLoaded += block;
        if (updateLiveData) {
            totalLinkBytesLoadedLive.addAndGet(block);
        }
    }

    protected boolean writeChunkBytes(RAFChunk chunk) {
        try {
            synchronized (outputPartFile) {
                final RandomAccessFile raf = outputPartFileRaf.get();
                raf.seek(chunk.getWritePosition());
                raf.write(chunk.buffer.getInternalBuffer(), 0, chunk.buffer.size());
                if (chunk.getID() >= 0) {
                    downloadable.getChunksProgress()[chunk.getID()] = chunk.getCurrentBytesPosition() - 1;
                }
            }
            DownloadController.getInstance().requestSaving();
            return true;
        } catch (Exception e) {
            LogSource.exception(logger, e);
            error(new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, Exceptions.getStackTrace(e), LinkStatus.VALUE_LOCAL_IO_ERROR));
            return false;
        }
    }

    public void cleanupDownladInterface() {
        try {
            try {
                downloadable.removeConnectionHandler(this.getManagedConnetionHandler());
            } catch (final Throwable e) {
            }
            try {
                this.connection.disconnect();
            } catch (Throwable e) {
            }
        } finally {
            closeOutputChannel();
        }
    }

    private void closeOutputChannel() {
        try {
            final RandomAccessFile loutputPartFileRaf = outputPartFileRaf.getAndSet(null);
            if (loutputPartFileRaf != null) {
                logger.info("Close File. Let AV programs run");
                loutputPartFileRaf.close();
            }
        } catch (Throwable e) {
            LogSource.exception(logger, e);
        }
    }

    public LogInterface getLogger() {
        return logger;
    }

    @Override
    public void close() {
    }

    @Override
    public Downloadable getDownloadable() {
        return downloadable;
    }
}
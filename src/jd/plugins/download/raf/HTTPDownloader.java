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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import jd.controlling.downloadcontroller.DiskSpaceReservation;
import jd.controlling.downloadcontroller.DownloadSession;
import jd.controlling.downloadcontroller.ExceptionRunnable;
import jd.controlling.downloadcontroller.FileIsLockedException;
import jd.controlling.downloadcontroller.ManagedThrottledConnectionHandler;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.Downloadable;
import jd.plugins.download.HashInfo;
import jd.plugins.download.HashInfo.TYPE;
import jd.plugins.download.HashResult;
import jd.plugins.download.raf.BytesMappedFile.BytesMappedFileCallback;
import jd.plugins.download.raf.FileBytesMap.FileBytesMapView;
import jd.plugins.download.raf.HTTPChunk.ERROR;

import org.appwork.exceptions.WTFException;
import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.HTTPHeader;
import org.jdownloader.plugins.DownloadPluginProgress;
import org.jdownloader.plugins.SkipReason;
import org.jdownloader.plugins.SkipReasonException;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.translate._JDT;
import org.jdownloader.updatev2.InternetConnectionSettings;

public class HTTPDownloader extends DownloadInterface implements FileBytesCacheFlusher, BytesMappedFileCallback {
    public static enum STATEFLAG {
        RUN,
        STOP,
        KILL
    }

    private final AtomicReference<BytesMappedFile> bytesMappedFile = new AtomicReference<BytesMappedFile>(null);
    private volatile File                          outputCompleteFile;
    private volatile File                          outputFinalCompleteFile;
    private volatile File                          outputPartFile;
    private final AtomicBoolean                    connectedFlag   = new AtomicBoolean(false);
    private final AtomicBoolean                    downloadingFlag = new AtomicBoolean(false);
    private final CopyOnWriteArrayList<HTTPChunk>  activeChunks    = new CopyOnWriteArrayList<HTTPChunk>();
    private final ArrayList<Runnable>              queuedTasks     = new ArrayList<Runnable>();
    private int                                    readTimeout     = 100000;
    private int                                    requestTimeout  = 100000;
    private final AtomicReference<STATEFLAG>       stateFlag       = new AtomicReference<STATEFLAG>(STATEFLAG.RUN);

    protected AtomicReference<STATEFLAG> getStateFlag() {
        return stateFlag;
    }

    protected int                                         chunksNum       = 1;
    protected int                                         maxChunks       = 0;
    private boolean                                       tryRangeRequest = false;
    protected Browser                                     browser;
    protected volatile URLConnectionAdapter               connection;
    protected final Downloadable                          downloadable;
    protected final LogInterface                          logger;
    protected final ManagedThrottledConnectionHandler     connectionHandler;
    private long                                          startTimeStamp  = -1;
    private boolean                                       resumedDownload;
    private final FileBytesCache                          downloadWriteCache;
    private final FileBytesMap                            cacheMap        = new FileBytesMap();
    private final HashMap<HTTPChunk.ERROR, AtomicInteger> errorMap        = new HashMap<HTTPChunk.ERROR, AtomicInteger>();

    protected synchronized int addError(HTTPChunk.ERROR error) {
        AtomicInteger ret = errorMap.get(error);
        if (ret == null) {
            ret = new AtomicInteger(0);
            errorMap.put(error, ret);
        }
        return ret.incrementAndGet();
    }

    protected synchronized int removeError(HTTPChunk.ERROR error) {
        AtomicInteger ret = errorMap.remove(error);
        if (ret != null) {
            return ret.get();
        }
        return 0;
    }

    protected synchronized boolean hasErrors() {
        return errorMap.isEmpty();
    }

    protected synchronized int getErrors(HTTPChunk.ERROR error) {
        AtomicInteger ret = errorMap.get(error);
        if (ret != null) {
            return ret.get();
        }
        return 0;
    }

    protected synchronized void clearErrors() {
        errorMap.clear();
    }

    protected FileBytesMap getCacheMap() {
        return cacheMap;
    }

    private ChunkStrategy           chunkStrategy = null;
    private final HTTPDownloadHints rafHints;

    protected HTTPDownloadHints getRafHints() {
        return rafHints;
    }

    public void setChunkNum(final int chunksNum) {
        if (checkAccess()) {
            if (getMaxChunks() > 0 && chunksNum > getMaxChunks()) {
                return;
            }
            this.chunksNum = (Math.max(1, chunksNum));
            logger.info("setChunkNum: " + this.chunksNum);
            synchronized (activeChunks) {
                activeChunks.notifyAll();
            }
        } else {
            synchronized (queuedTasks) {
                if (downloadingFlag.get()) {
                    queuedTasks.add(new Runnable() {
                        @Override
                        public void run() {
                            setChunkNum(chunksNum);
                        }
                    });
                }
            }
            synchronized (activeChunks) {
                activeChunks.notifyAll();
            }
        }
    }

    public int setMaxChunksNum(int maxChunks) {
        if (checkAccess()) {
            this.maxChunks = (Math.max(0, maxChunks));
            logger.info("setMaxChunks: " + this.maxChunks);
            if (maxChunks > 0 && chunksNum > maxChunks) {
                setChunkNum(maxChunks);
            }
            synchronized (activeChunks) {
                activeChunks.notifyAll();
            }
            return maxChunks;
        }
        return -1;
    }

    public int getMaxChunks() {
        Integer maxChunksSupported = rafHints.getMaxChunksSupported();
        if (maxChunksSupported != null) {
            return Math.max(maxChunks, maxChunksSupported);
        }
        return maxChunks;
    }

    public int getChunkNum() {
        return chunksNum;
    }

    public int getActiveChunks() {
        return activeChunks.size();
    }

    @Override
    public ManagedThrottledConnectionHandler getManagedConnetionHandler() {
        return connectionHandler;
    }

    public HTTPDownloader(Downloadable downloadLink, Request request) throws IOException, PluginException {
        connectionHandler = new ManagedThrottledConnectionHandler();
        this.downloadable = downloadLink;
        logger = downloadLink.getLogger();
        browser = downloadLink.getContextBrowser();
        InternetConnectionSettings config = JsonConfig.create(InternetConnectionSettings.PATH, InternetConnectionSettings.class);
        setRequestTimeout(config.getHttpConnectTimeout());
        setReadTimeout(config.getHttpReadTimeout());
        setInitialRequest(request);
        /* setDownloadInstance after all variables are set! */
        downloadLink.setDownloadInterface(this);
        downloadWriteCache = DownloadSession.getDownloadWriteCache();
        chunkStrategy = new MaxJumpStrategy(this, 500 * 1024 * 1024l, 512 * 1024);
        rafHints = downloadable.getDataBindingInterface(HTTPDownloadHints.class);
    }

    protected boolean checkAccess() {
        Thread thread = Thread.currentThread();
        return thread instanceof SingleDownloadController && ((SingleDownloadController) thread).getDownloadInstance() == this;
    }

    public void setResume(boolean value) {
        if (checkAccess()) {
            logger.info("setResume: " + value);
            tryRangeRequest = value;
            downloadable.setResumeable(value);
        }
    }

    protected boolean tryRangeRequest() {
        Boolean isRangeRequestSupported = rafHints.isRangeRequestSupported();
        if (Boolean.FALSE.equals(isRangeRequestSupported)) {
            return isRangeRequestSupported;
        }
        return tryRangeRequest || downloadable.isServerComaptibleForByteRangeRequest();
    }

    /**
     * @return the startTimeStamp
     */
    public long getStartTimeStamp() {
        return startTimeStamp;
    }

    protected Browser getBrowser() {
        return browser;
    }

    public URLConnectionAdapter connect() throws IOException, PluginException {
        if (connectedFlag.compareAndSet(false, true)) {
            boolean okayFlag = false;
            try {
                Browser br = getBrowser();
                setReadTimeout(br.getReadTimeout());
                setRequestTimeout(br.getConnectTimeout());
                initialRequest.setConnectTimeout(getRequestTimeout());
                initialRequest.setReadTimeout(getReadTimeout());
                final File partFile = new File(downloadable.getFileOutputPart());
                BytesMappedFile lockedBytesMappedFile = bytesMappedFile.get();
                if (lockedBytesMappedFile == null || BytesMappedFileManager.getInstance().get(partFile) != lockedBytesMappedFile) {
                    closeBytesMappedFile();
                    lockedBytesMappedFile = BytesMappedFileManager.getInstance().lock(partFile);
                    bytesMappedFile.set(lockedBytesMappedFile);
                }
                Boolean validation = validateFileBytesMapView(lockedBytesMappedFile.getFileBytesMap());
                if (validation != null && validation) {
                    logger.info("Valid CacheMap available");
                    cacheMap.set(lockedBytesMappedFile.getFileBytesMap());
                } else {
                    logger.info("No CacheMap available");
                    cacheMap.reset();
                }
                connection = openConnection(null);
                if (initialRequest.getLocation() != null) {
                    return connection;
                }
                if (tryRangeRequest()) {
                    if (connection.getHeaderField("X-Mod-H264-Streaming") != null && connection.getRequestProperty(HTTPConstants.HEADER_REQUEST_RANGE) == null) {
                        logger.info("Try workaround for X-Mod-H264-Streaming");
                        connection.disconnect();
                        connection = openConnection(true);
                    }
                    if (connection.getResponseCode() == 416) {
                        addError(ERROR.RANGE);
                        logger.info("Try again to avoid 416");
                        connection.disconnect();
                        connection = openConnection(false);
                    }
                }
                okayFlag = true;
                return connection;
            } finally {
                connectedFlag.set(okayFlag);
            }
        } else {
            throw new IllegalStateException("Already connecting/connected");
        }
    }

    public boolean validateConnection(URLConnectionAdapter validatingConnection) {
        final URLConnectionAdapter initialConnection = connection;
        if (initialConnection == null) {
            return false;
        }
        // Server: Microsoft-IIS/8.5, removes ContentDisposition and changes Content-Type to text/html on range requests
        final boolean validationWorkaround = StringUtils.contains(validatingConnection.getHeaderField(HTTPConstants.HEADER_RESPONSE_SERVER), "IIS");
        if (!validationWorkaround) {
            if (initialConnection.isContentDisposition() && validatingConnection.isContentDisposition()) {
                final String aFileName = Plugin.getFileNameFromDispositionHeader(initialConnection);
                final String bFileName = Plugin.getFileNameFromDispositionHeader(validatingConnection);
                if (!StringUtils.equals(aFileName, bFileName)) {
                    logger.severe("sameContent: FALSE|Filename:'" + aFileName + "'<->'" + bFileName + "'");
                    return false;
                }
            }
            final String aContentType = initialConnection.getHeaderField(HTTPConstants.HEADER_REQUEST_CONTENT_TYPE);
            final String bContentType = validatingConnection.getHeaderField(HTTPConstants.HEADER_REQUEST_CONTENT_TYPE);
            if (!StringUtils.equals(aContentType, bContentType)) {
                logger.severe("sameContent: FALSE|ContentType:'" + aContentType + "'<->'" + bContentType + "'");
                return false;
            }
            final long verifiedFileSize = getVerifiedFileSize();
            if (verifiedFileSize >= 0) {
                final long connectionLength = getCompleteContentLength(validatingConnection, true);
                if (connectionLength >= 0 && verifiedFileSize != connectionLength) {
                    logger.severe("sameContent: FALSE|verifiedFileSize:'" + verifiedFileSize + "'<->'" + connectionLength + "'");
                    return false;
                } else if (connectionLength < 0 && getCompleteContentLength(initialConnection, true) >= 0) {
                    logger.severe("sameContent: FALSE|missingContentLength");
                    return false;
                }
            }
            logger.severe("sameContent: TRUE");
            return true;
        } else {
            logger.severe("sameContent: TRUE(IIS Workaround)");
            return true;
        }
    }

    @Override
    public URLConnectionAdapter connect(Browser br) throws Exception {
        if (initialRequest == null) {
            throw new IllegalStateException("Wrong Mode. Instance is in direct Connection mode");
        }
        logger.finer("Connecting...");
        if (this.browser != br) {
            logger.info("Different Browser Instance for openDownload and connect!");
            this.browser = br;
        }
        return connect();
    }

    protected String getRange(Long from, Long to) {
        if (from == null || from < 0) {
            return null;
        } else {
            if (to == null || to < 0) {
                return "bytes=" + from + "-";
            } else {
                return "bytes=" + from + "-" + to;
            }
        }
    }

    protected long[] parseRange(String bytes) {
        final String from = new Regex(bytes, "bytes\\s*=\\s*(\\d*)-").getMatch(0);
        final String to = new Regex(bytes, "bytes\\s*=\\s*.*?-\\s*(\\d*)").getMatch(0);
        final long[] ret = new long[] { -1l, -1l };
        if (StringUtils.isNotEmpty(from)) {
            ret[0] = Long.parseLong(from);
        }
        if (StringUtils.isNotEmpty(to)) {
            ret[1] = Long.parseLong(to);
        }
        return ret;
    }

    /**
     * our first connection is either the complete file(without range or 0-end) or resume at first unMarkedBegin(x-end) till end of file
     *
     * @param allowRangeRequest
     * @return
     * @throws IOException
     */
    protected URLConnectionAdapter openConnection(Boolean requestRangeIfPossible) throws IOException {
        final boolean tryRangeRequest = tryRangeRequest() && !Boolean.FALSE.equals(requestRangeIfPossible);
        final List<ChunkRange> unMarkedAreas = chunkStrategy.getUnMarkedAreas();
        final long verifiedFileSize = downloadable.getVerifiedFileSize();
        final String rangeRequest;
        if (tryRangeRequest && (unMarkedAreas.size() > 0 && unMarkedAreas.get(0).getFrom() > 0) || Boolean.TRUE.equals(requestRangeIfPossible)) {
            ChunkRange chunkRange = unMarkedAreas.get(0);
            rangeRequest = getRange(chunkRange.getFrom(), null);
        } else {
            rangeRequest = null;
        }
        /* encoding can cause problems because indices no longer match real file indices */
        initialRequest.getHeaders().put(new HTTPHeader(HTTPConstants.HEADER_REQUEST_ACCEPT_ENCODING, "identity", false));
        if (rangeRequest != null) {
            initialRequest.getHeaders().put(new HTTPHeader(HTTPConstants.HEADER_REQUEST_RANGE, rangeRequest, false));
            URLConnectionAdapter connection = browser.openRequestConnection(initialRequest, false);
            if (connection.getRequest().getLocation() != null) {
                return connection;
            }
            if (connection.getResponseCode() == 200 || connection.getResponseCode() == 206) {
                long[] contentRange = connection.getRange();
                if (contentRange != null && verifiedFileSize >= 0 && contentRange[2] != verifiedFileSize) {
                    logger.info("Strange Response: verifiedFileSize does match contentRange/Length!");
                }
            }
            return connection;
        } else {
            initialRequest.getHeaders().put(new HTTPHeader(HTTPConstants.HEADER_REQUEST_RANGE, null, false));
            URLConnectionAdapter connection = browser.openRequestConnection(initialRequest, false);
            if (connection.getRequest().getLocation() != null) {
                return connection;
            }
            long[] contentRange = connection.getRange();
            if (contentRange != null) {
                logger.info("Strange Response: received contentRange/Length and not having asked for a range!");
                if (contentRange[0] != 0) {
                    //
                    throw new IOException("Invalid Response: contentRange does not start at 0!");
                }
                if (verifiedFileSize >= 0) {
                    if (contentRange[2] == verifiedFileSize) {
                        logger.info("Strange Response: verifiedFileSize does match contentRange/Length!");
                    } else {
                        if (connection.getResponseCode() == 200 || connection.getResponseCode() == 206) {
                            logger.severe("Invalid Response: verifiedFileSize does not match contentRange/Length!");
                        }
                    }
                }
            }
            return connection;
        }
    }

    protected Boolean validateFileBytesMapView(FileBytesMap fileBytesMap) {
        if (fileBytesMap == null || fileBytesMap.getMarkedBytes() == 0) {
            return false;
        }
        final long verifiedFileSize = downloadable.getVerifiedFileSize();
        if (verifiedFileSize >= 0) {
            final long finalFileSize = fileBytesMap.getFinalSize();
            if (finalFileSize >= 0) {
                return finalFileSize == verifiedFileSize;
            } else {
                long currentSize = fileBytesMap.getSize();
                return verifiedFileSize >= currentSize;
            }
        }
        return false;
    }

    protected int getReadTimeout() {
        return Math.max(10000, readTimeout);
    }

    protected int getRequestTimeout() {
        return Math.max(10000, requestTimeout);
    }

    protected boolean isFileComplete() {
        BytesMappedFile bytesMappedFile = this.bytesMappedFile.get();
        if (bytesMappedFile != null) {
            final long markedSize = bytesMappedFile.getFileBytesMap().getMarkedBytes();
            final long expectedFileSize = bytesMappedFile.getFileBytesMap().getFinalSize();
            if (expectedFileSize >= 0) {
                final boolean ret = markedSize == expectedFileSize;
                logger.info("isFileComplete: ExpectedSize:" + expectedFileSize + "|MarkedSize:" + markedSize + "=" + ret);
                return ret;
            }
            final long verifiedFileSize = getVerifiedFileSize();
            if (verifiedFileSize >= 0) {
                final boolean ret = markedSize == verifiedFileSize;
                logger.info("isFileComplete: VerifiedFileSize:" + verifiedFileSize + "|MarkedSize:" + markedSize + "=" + ret);
                return ret;
            }
        }
        return false;
    }

    protected boolean isDownloadComplete() {
        if (isFileComplete()) {
            return true;
        }
        if (externalDownloadStop() == false && hasErrors()) {
            logger.info("isDownloadComplete: errorFree=true");
            return true;
        }
        logger.info("isDownloadComplete: false");
        return false;
    }

    /**
     * Wartet bis alle Chunks fertig sind, aktuelisiert den downloadlink regelmaesig und fordert beim Controller eine aktualisierung des
     * links an
     */
    protected void onChunkFinished(HTTPChunk chunk) {
        if (activeChunks.contains(chunk)) {
            synchronized (activeChunks) {
                activeChunks.notifyAll();
            }
        }
    }

    protected byte[] getChunkBuffer(HTTPChunk chunk) {
        return new byte[32 * 1024];
    }

    private void setRangeRequestSupported(Boolean b) {
        logger.info("setRangeRequestSupported: " + b);
        rafHints.setRangeRequestSupported(b);
    }

    private void setMaxChunksSupported(Integer maxChunks) {
        logger.info("setMaxChunksSupported: " + maxChunks);
        rafHints.setMaxChunksSupported(maxChunks);
        if (maxChunks != null) {
            setMaxChunksNum(maxChunks);
        }
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
            logger.finer("Start Downloading");
            if (connectedFlag.get() == false) {
                connect(getBrowser());
                if (connection != null && connection.getRequest().getLocation() != null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Redirectloop");
                }
            }
            if (connection == null || !connection.isOK()) {
                if (connection != null) {
                    logger.finest(connection.toString());
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            downloadable.setAvailable(AvailableStatus.TRUE);
            downloadable.updateFinalFileName();
            final long contentLength = getCompleteContentLength(connection, true);
            final String requestContentRange = connection.getRequestProperty(HTTPConstants.HEADER_REQUEST_RANGE);
            final long[] requestedRange = parseRange(requestContentRange);
            final String responseContentEncoding = connection.getHeaderField(HTTPConstants.HEADER_RESPONSE_CONTENT_ENCODING);
            final String responseAcceptRanges = connection.getHeaderField(HTTPConstants.HEADER_RESPONSE_CONTENT_LENGTH);
            final long[] responseContentRange = connection.getRange();
            if (connection.getHeaderField("Cf-Bgj") != null) {
                logger.info("Cloudflare Image Compression detected!");
            }
            if (requestContentRange != null) {
                logger.info("InitialConnection with RangeRequest: " + requestContentRange);
                if (responseContentRange != null) {
                    if (responseContentRange[0] != requestedRange[0]) {
                        throw new WTFException("FIXME: RangeError(From)");
                    }
                    if (requestedRange[1] >= 0 && responseContentRange[1] != requestedRange[1]) {
                        throw new WTFException("FIXME: RangeError(To)");
                    }
                } else {
                    if (!"bytes".equalsIgnoreCase(responseAcceptRanges)) {
                        logger.info("It seems RangeRequest is not supported: " + responseAcceptRanges);
                        setRangeRequestSupported(false);
                    }
                }
            } else {
                logger.info("InitialConnection without RangeRequest");
                if (getErrors(ERROR.RANGE) > 0) {
                    removeError(ERROR.RANGE);
                    logger.info("It seems RangeRequest is not supported because it failed: " + responseAcceptRanges);
                    setRangeRequestSupported(false);
                }
            }
            /*
             * unsupported range-request handling for content-encoded responses, because either we get new encoded response(not resumable)
             * or just range of complete encoded content(gzip inputstream fails)
             */
            if (!isNoneContentEncoding(responseContentEncoding)) {
                logger.severe("Content-Encoding(" + responseContentEncoding + ") prevents RangeRequest!");
                setRangeRequestSupported(false);
                if (responseContentRange != null) {
                    if (responseContentRange[0] > 0) {
                        throw new WTFException("UNSUPPORTED: resume content-Encoding");
                    }
                    if (contentLength >= 0 && responseContentRange[1] + 1 != contentLength) {
                        throw new WTFException("UNSUPPORTED: chunked content-Encoding");
                    }
                } else {
                    if (requestedRange[0] >= 0 && requestedRange[0] != 0) {
                        throw new WTFException("UNSUPPORTED: resume content-Encoding");
                    }
                }
            } else if (requestContentRange == null && getCompleteContentLength(connection, false) >= 0) {
                /* only trust contentHash from noneRanged & noneEncoded */
                final String contentSHA1 = connection.getHeaderField("Content-SHA1");
                HashInfo hashInfo = null;
                if (contentSHA1 != null && contentSHA1.matches("^[a-fA-F0-9]{40}$")) {
                    hashInfo = new HashInfo(contentSHA1, TYPE.SHA1);
                }
                final String contentMD5 = connection.getHeaderField("Content-MD5");
                if (hashInfo == null && contentMD5 != null && contentMD5.matches("^[a-fA-F0-9]{32}$")) {
                    hashInfo = new HashInfo(contentMD5, TYPE.MD5);
                }
                if (hashInfo != null) {
                    downloadable.setHashInfo(hashInfo);
                }
            }
            // TODO:
            // https://cloud.google.com/storage/docs/hashes-etags
            // https://cloud.google.com/storage/docs/reference-headers#xgooghash
            final List<String> xGoogHashes = connection.getHeaderFields("X-Goog-Hash");
            if (xGoogHashes != null && xGoogHashes.size() > 0) {
                // crc32c https://github.com/googlearchive/crc32c-java/blob/master/src/com/google/cloud/Crc32c.java
                // md5
                // base64 encoded
            }
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
                BytesMappedFile lockedBytesMappedFile = bytesMappedFile.get();
                if (lockedBytesMappedFile == null || BytesMappedFileManager.getInstance().get(outputPartFile) != lockedBytesMappedFile) {
                    if (lockedBytesMappedFile != null) {
                        logger.warning("WARNING: BytesMappedFile changed!");
                    }
                    closeBytesMappedFile();
                    lockedBytesMappedFile = BytesMappedFileManager.getInstance().lock(outputPartFile);
                    bytesMappedFile.set(lockedBytesMappedFile);
                }
                BytesMappedFileManager.getInstance().open(lockedBytesMappedFile, this);
                final FileBytesMap fileBytesMap = lockedBytesMappedFile.getFileBytesMap();
                final long verifiedFileSize = downloadable.getVerifiedFileSize();
                final long compareSize = fileBytesMap.getFinalSize();
                if (fileBytesMap.getMarkedBytes() > 0) {
                    if (contentLength >= 0) {
                        if (fileBytesMap.getSize() > contentLength) {
                            throw new WTFException("FIXME: sizeMissmatch of BytesMappedFile(" + fileBytesMap.getSize() + ") and Content-Length(" + contentLength + ")");
                        } else if (fileBytesMap.getFinalSize() < 0) {
                            logger.info("Set Content-Length(" + contentLength + ") for BytesMappedFile");
                            fileBytesMap.setFinalSize(contentLength);
                        }
                    } else if (verifiedFileSize >= 0) {
                        if (fileBytesMap.getSize() > verifiedFileSize) {
                            throw new WTFException("FIXME: sizeMissmatch of BytesMappedFile(" + fileBytesMap.getSize() + ") and VerifiedFileSize(" + verifiedFileSize + ")");
                        } else if (fileBytesMap.getFinalSize() < 0) {
                            logger.info("Set VerifiedFileSize(" + verifiedFileSize + ") for BytesMappedFile");
                            fileBytesMap.setFinalSize(verifiedFileSize);
                        }
                    }
                } else {
                    if (contentLength >= 0) {
                        logger.info("Use Content-Length(" + contentLength + ") for BytesMappedFile");
                        fileBytesMap.setFinalSize(contentLength);
                        if (verifiedFileSize >= 0 && contentLength != verifiedFileSize) {
                            logger.info("Warning Content-Length(" + contentLength + ") does not match VerifiedFileSize(" + verifiedFileSize + ")");
                        }
                    } else if (verifiedFileSize >= 0) {
                        logger.info("Use VerifiedFileSize(" + verifiedFileSize + ") for BytesMappedFile");
                        fileBytesMap.setFinalSize(verifiedFileSize);
                    }
                }
                if (fileBytesMap.getFinalSize() != downloadable.getVerifiedFileSize() || fileBytesMap.getFinalSize() != compareSize) {
                    final long mapSize = fileBytesMap.getFinalSize();
                    logger.info("Update BytesMappedFile(" + mapSize + ")");
                    BytesMappedFileManager.getInstance().write(lockedBytesMappedFile);
                    if (mapSize >= 0) {
                        logger.info("Update VerifiedFileSize(" + mapSize + ")");
                        downloadable.setVerifiedFileSize(mapSize);
                    }
                }
                cacheMap.set(fileBytesMap);
                if (!tryRangeRequest()) {
                    logger.info("Range-Request is not supported: reset cacheMap");
                    cacheMap.reset();
                    cacheMap.setFinalSize(fileBytesMap.getFinalSize());
                }
                resumedDownload = cacheMap.getMarkedBytes() > 0;
                if (download()) {
                    logger.info("Download is complete");
                    HashResult hashResult = getHashResult(downloadable, outputPartFile);
                    if (hashResult != null) {
                        logger.info(hashResult.toString());
                    }
                    downloadable.setHashResult(hashResult);
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
                    if (getErrors(HTTPChunk.ERROR.NOT_ENOUGH_SPACE_ON_DISK) > 0) {
                        /* not easy to differ ioExceptions from disk full as the exception is localized */
                        throw new SkipReasonException(SkipReason.DISK_FULL);
                    }
                    if (getErrors(HTTPChunk.ERROR.FLUSHING) > 0) {
                        /* not easy to differ ioExceptions from disk full as the exception is localized */
                        throw new SkipReasonException(SkipReason.DISK_FULL);
                    }
                    if (getErrors(ERROR.CONNECTING) > 0 || getErrors(ERROR.DOWNLOADING) > 0) {
                        throw new PluginException(LinkStatus.ERROR_DOWNLOAD_INCOMPLETE, LinkStatus.VALUE_NETWORK_IO_ERROR);
                    }
                    throw new PluginException(LinkStatus.ERROR_DOWNLOAD_INCOMPLETE, _JDT.T.download_error_message_incomplete());
                }
                return false;
            } finally {
                closeBytesMappedFile();
                try {
                    downloadable.free(reservation);
                } catch (final Throwable ignore) {
                    LogSource.exception(logger, ignore);
                }
                downloadable.unlockFiles(outputCompleteFile, outputFinalCompleteFile, outputPartFile);
            }
        } finally {
            cleanupDownladInterface();
        }
    }

    private boolean isNoneContentEncoding(String contentEncoding) {
        return contentEncoding == null || "none".equalsIgnoreCase(contentEncoding);
    }

    protected long getCompleteContentLength(URLConnectionAdapter connection, boolean forceTrustContentLength) {
        if (connection != null) {
            final String contentEncoding = connection.getHeaderField(HTTPConstants.HEADER_RESPONSE_CONTENT_ENCODING);
            final String h264StreamingMod = connection.getHeaderField("X-Mod-H264-Streaming");
            if (forceTrustContentLength || (isNoneContentEncoding(contentEncoding) && h264StreamingMod == null)) {
                long contentRange[] = connection.getRange();
                if (contentRange != null) {
                    /* we have a range response, let's use it */
                    if (contentRange[2] >= 0) {
                        return contentRange[2];
                    }
                }
                final String range = connection.getRequestProperty(HTTPConstants.HEADER_REQUEST_RANGE);
                long[] parsedRange = parseRange(range);
                if ((range == null || (parsedRange[0] == 0 && parsedRange[1] < 0)) && connection.getLongContentLength() >= 0 && connection.isOK()) {
                    /* we have no range request/complete file request and connection is okay, so we can use the content-length */
                    return connection.getLongContentLength();
                }
            } else {
                logger.info("Don't trust ContentLength:contentEncoding=" + contentEncoding + "|h264StreamingMod=" + h264StreamingMod);
            }
        }
        return -1;
    }

    protected long getVerifiedFileSize() {
        final long verifiedFileSize = downloadable.getVerifiedFileSize();
        if (verifiedFileSize >= 0) {
            return verifiedFileSize;
        }
        if (connection != null) {
            return getCompleteContentLength(connection, false);
        }
        return -1;
    }

    public Request getRequest() {
        return this.initialRequest;
    }

    @Override
    public URLConnectionAdapter getConnection() {
        if (connection == null && initialRequest != null) {
            return initialRequest.getHttpConnection();
        }
        return this.connection;
    }

    @Override
    public boolean externalDownloadStop() {
        return stateFlag.get() == STATEFLAG.STOP;
    }

    /** signal that we stopped download external */
    public void stopDownload() {
        if (stateFlag.compareAndSet(STATEFLAG.RUN, STATEFLAG.STOP)) {
            logger.info("stopDownload");
            synchronized (activeChunks) {
                activeChunks.notifyAll();
            }
        }
    }

    protected int getChunksInArea(ChunkRange chunkRange) {
        int ret = 0;
        for (HTTPChunk chunk : activeChunks) {
            if (chunk.isRunning() == false) {
                continue;
            }
            if (chunk.getChunkRange().getLength() >= 0) {
                if (chunkRange.getFrom() >= chunk.getChunkRange().getFrom() && chunkRange.getFrom() <= chunk.getChunkRange().getTo()) {
                    ret++;
                } else if (chunkRange.getLength() >= 0 && chunkRange.getTo() >= chunk.getChunkRange().getFrom() && chunkRange.getTo() <= chunk.getChunkRange().getTo()) {
                    ret++;
                }
            } else {
                if (chunkRange.getFrom() >= chunk.getChunkRange().getFrom()) {
                    ret++;
                }
            }
        }
        return ret;
    }

    protected boolean hasChunkFrom(ChunkRange chunkRange) {
        for (HTTPChunk chunk : activeChunks) {
            if (chunk.isRunning() == false) {
                continue;
            }
            if (chunk.getChunkRange().getFrom() == chunkRange.getFrom()) {
                return true;
            }
        }
        return false;
    }

    protected boolean download() {
        DownloadPluginProgress downloadPluginProgress = null;
        try {
            if (isFileComplete()) {
                return true;
            }
            startTimeStamp = System.currentTimeMillis();
            downloadable.setConnectionHandler(this.getManagedConnetionHandler());
            downloadPluginProgress = new DownloadPluginProgress(downloadable, this, Color.GREEN.darker());
            downloadable.addPluginProgress(downloadPluginProgress);
            final ArrayList<HTTPChunk> finishedChunks = new ArrayList<HTTPChunk>();
            downloadingFlag.set(true);
            downloadable.setDownloadBytesLoaded(cacheMap.getMarkedBytes());
            while (true) {
                synchronized (queuedTasks) {
                    for (Runnable task : queuedTasks) {
                        try {
                            task.run();
                        } catch (Throwable e) {
                            LogSource.exception(logger, e);
                        }
                    }
                    queuedTasks.clear();
                }
                if (getStateFlag().get() == STATEFLAG.RUN) {
                    List<HTTPChunk> nextChunks = chunkStrategy.getNextChunks(finishedChunks);
                    if (nextChunks != null && nextChunks.size() > 0) {
                        for (HTTPChunk nextChunk : nextChunks) {
                            if (stateFlag.get() == STATEFLAG.RUN) {
                                activeChunks.add(nextChunk);
                                nextChunk.start();
                            }
                        }
                    }
                }
                if (activeChunks.size() == 0) {
                    break;
                }
                synchronized (activeChunks) {
                    finishedChunks.clear();
                    for (HTTPChunk chunk : activeChunks) {
                        if (!chunk.isRunning()) {
                            finishedChunks.add(chunk);
                            activeChunks.remove(chunk);
                        } else if (getStateFlag().get() != STATEFLAG.RUN) {
                            chunk.closeConnections();
                            activeChunks.remove(chunk);
                        }
                    }
                    if (getStateFlag().get() == STATEFLAG.RUN && finishedChunks.size() == 0) {
                        try {
                            if (activeChunks.size() >= getChunkNum()) {
                                activeChunks.wait();
                            } else {
                                activeChunks.wait(1000);
                            }
                        } catch (InterruptedException e) {
                            LogSource.exception(logger, e);
                        }
                    }
                }
            }
            flushWriteCache();
            return isDownloadComplete();
        } finally {
            flushWriteCache();
            try {
                final long startTimeStamp = getStartTimeStamp();
                if (startTimeStamp > 0) {
                    downloadable.addDownloadTime(System.currentTimeMillis() - getStartTimeStamp());
                }
            } catch (final Throwable ignore) {
            }
            try {
                downloadable.removeConnectionHandler(this.getManagedConnetionHandler());
            } catch (final Throwable ignore) {
            }
            downloadable.removePluginProgress(downloadPluginProgress);
            close();
        }
    }

    private void flushWriteCache() {
        if (downloadingFlag.get()) {
            downloadWriteCache.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (downloadingFlag.get()) {
                            downloadWriteCache.flushIfContains(HTTPDownloader.this);
                        }
                    } finally {
                        synchronized (queuedTasks) {
                            downloadingFlag.set(false);
                            queuedTasks.clear();
                        }
                        downloadable.setDownloadBytesLoaded(bytesMappedFile.get().getFileBytesMap().getMarkedBytes());
                    }
                }
            });
        }
    }

    /**
     * terminate this DownloadInterface, abort all running chunks
     */
    protected boolean terminate() {
        if (stateFlag.compareAndSet(STATEFLAG.RUN, STATEFLAG.KILL)) {
            logger.info("terminate");
            synchronized (activeChunks) {
                activeChunks.notifyAll();
            }
            return true;
        }
        return false;
    }

    public void setReadTimeout(int readTimeout) {
        if (checkAccess()) {
            this.readTimeout = readTimeout;
        }
    }

    public void setRequestTimeout(int requestTimeout) {
        if (checkAccess()) {
            this.requestTimeout = requestTimeout;
        }
    }

    protected void finalizeDownload(File outputPartFile, File outputCompleteFile) throws Exception {
        if (downloadable.rename(outputPartFile, outputCompleteFile)) {
            try {
                Date last = TimeFormatter.parseDateString(connection.getHeaderField("Last-Modified"));
                if (last != null && JsonConfig.create(GeneralSettings.class).isUseOriginalLastModified()) {
                    /* set original lastModified timestamp */
                    outputCompleteFile.setLastModified(last.getTime());
                } else {
                    /* set current timestamp as lastModified timestamp */
                    outputCompleteFile.setLastModified(System.currentTimeMillis());
                }
            } catch (final Throwable ignore) {
                LogSource.exception(logger, ignore);
            }
        } else {
            throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, _JDT.T.system_download_errors_couldnotrename(), LinkStatus.VALUE_LOCAL_IO_ERROR);
        }
    }

    public boolean isResumedDownload() {
        return resumedDownload;
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
        } catch (Exception e) {
            LogSource.exception(logger, e);
            throw new SkipReasonException(SkipReason.INVALID_DESTINATION, e);
        }
    }

    @Override
    public long getTotalLinkBytesLoadedLive() {
        return cacheMap.getMarkedBytesLive();
    }

    public void cleanupDownladInterface() {
        try {
            if (initialRequest != null && initialRequest.getHttpConnection() != null) {
                initialRequest.getHttpConnection().disconnect();
            }
        } catch (final Throwable ignore) {
        }
        try {
            if (connection != null) {
                this.connection.disconnect();
            }
        } catch (Throwable ignore) {
        }
    }

    private void closeBytesMappedFile() {
        try {
            BytesMappedFile lbytesMappedFile = bytesMappedFile.getAndSet(null);
            if (lbytesMappedFile != null) {
                Boolean ret = BytesMappedFileManager.getInstance().unlock(lbytesMappedFile);
                if (ret != null) {
                    if (ret) {
                        logger.info("File released: " + lbytesMappedFile.getFile());
                    } else {
                        logger.info("Close File?: " + lbytesMappedFile.getFile());
                        ret = BytesMappedFileManager.getInstance().close(lbytesMappedFile, this);
                        if (ret != null && ret) {
                            logger.info("File closed: " + lbytesMappedFile.getFile());
                        }
                    }
                }
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
        cleanupDownladInterface();
        closeBytesMappedFile();
    }

    @Override
    public Downloadable getDownloadable() {
        return downloadable;
    }

    /**
     * return true to signal the writer to stop writing
     *
     * @param readBuffer
     * @param length
     * @param fileWritePosition
     * @return
     */
    protected long write(HTTPChunk httpChunk, byte[] readBuffer, int length, long fileWritePosition) {
        final BytesMappedFile file = getBytesMappedFile();
        if (stateFlag.get() != STATEFLAG.RUN || file == null) {
            return 0;
        }
        final long skippable = file.getFileBytesMap().skippable(fileWritePosition, length);
        if (skippable < length) {
            downloadWriteCache.write(this, fileWritePosition, readBuffer, length);
        }
        final long overlap = cacheMap.mark(fileWritePosition, length);
        if (overlap != length) {
            return overlap;
        } else {
            return length;
        }
    }

    @Override
    public void flush(byte[] writeCache, int writeCachePosition, int length, long fileWritePosition) {
        final BytesMappedFile file = getBytesMappedFile();
        if (downloadingFlag.get() && file != null) {
            file.flush(writeCache, writeCachePosition, length, fileWritePosition);
        }
    }

    protected BytesMappedFile getBytesMappedFile() {
        return bytesMappedFile.get();
    }

    @Override
    public void flushed() {
        final BytesMappedFile file = getBytesMappedFile();
        if (downloadingFlag.get() && file != null) {
            file.flushed();
        }
    }

    @Override
    public FileBytesMapView getCacheMapView() {
        return new FileBytesMapView(cacheMap);
    }

    @Override
    public void onFlush(BytesMappedFile bytesMappedFile, IOException ioException) {
        if (ioException != null) {
            LogSource.exception(logger, ioException);
            if (terminate()) {
                if (StringUtils.containsIgnoreCase(ioException.getMessage(), "There is not enough space on the disk") || StringUtils.containsIgnoreCase(ioException.getMessage(), "No space left on device")) {
                    addError(HTTPChunk.ERROR.NOT_ENOUGH_SPACE_ON_DISK);
                } else {
                    addError(HTTPChunk.ERROR.FLUSHING);
                }
            }
        }
    }

    protected void updateCacheMapSize(final URLConnectionAdapter connection) {
        final long verifiedFileSize = downloadable.getVerifiedFileSize();
        if (verifiedFileSize < 0) {
            final long completeContentLength = getCompleteContentLength(connection, false);
            if (completeContentLength > 0 && completeContentLength >= cacheMap.getMarkedBytes()) {
                logger.info("Update VerifiedFileSize(" + completeContentLength + ") from URLConnection:\r\n" + connection);
                cacheMap.setFinalSize(completeContentLength);
                downloadable.setVerifiedFileSize(completeContentLength);
                final BytesMappedFile bytesMappedFile = getBytesMappedFile();
                if (bytesMappedFile != null) {
                    bytesMappedFile.getFileBytesMap().setFinalSize(completeContentLength);
                }
            }
        }
    }
}
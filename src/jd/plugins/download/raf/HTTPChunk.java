package jd.plugins.download.raf;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.plugins.download.Downloadable;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.utils.Exceptions;
import org.appwork.utils.NullsafeAtomicReference;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.net.LimitedInputStream;
import org.appwork.utils.net.throttledconnection.MeteredThrottledInputStream;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.speedmeter.AverageSpeedMeter;

public class HTTPChunk extends Thread {
    public static enum ERROR {
        NONE,
        /* got aborted */
        ABORT,
        /* encountered too many redirects */
        REDIRECT,
        /* content is different from initialConnection content */
        INVALID_CONTENT,
        /* no valid response code */
        INVALID_RESPONSE,
        /* encountered range error */
        RANGE,
        /* encountered error during connecting */
        CONNECTING,
        /* encountered error during downloading */
        DOWNLOADING,
        /* flushing writing on disk */
        FLUSHING,
        /* disk full */
        NOT_ENOUGH_SPACE_ON_DISK
    }

    private final AtomicBoolean                  connectionClosedFlag = new AtomicBoolean(false);
    private final HTTPDownloader                 dl;
    private final Downloadable                   downloadable;
    private final LogInterface                   logger;
    private final AtomicBoolean                  runningFlag          = new AtomicBoolean(true);
    private final URLConnectionAdapter           reuseConnection;
    private volatile URLConnectionAdapter        currentConnection    = null;
    private final NullsafeAtomicReference<ERROR> error                = new NullsafeAtomicReference<ERROR>(ERROR.NONE);
    private volatile Throwable                   errorThrowable       = null;

    protected ERROR getError() {
        return error.get();
    }

    protected Throwable getErrorThrowable() {
        return errorThrowable;
    }

    protected ChunkRange getChunkRange() {
        return chunkRange;
    }

    private final ChunkRange chunkRange;

    protected boolean isRunning() {
        return runningFlag.get();
    }

    private void setError(ERROR error, Throwable errorThrowable) {
        if (isExternalyAborted() == false && this.error.compareAndSet(ERROR.NONE, error)) {
            switch (error) {
            case CONNECTING:
                logger.severe("Connecting failed! " + Exceptions.getStackTrace(errorThrowable));
                break;
            case DOWNLOADING:
                logger.severe("Downloading failed! " + Exceptions.getStackTrace(errorThrowable));
                break;
            case RANGE:
                logger.severe("Range error!");
                break;
            case ABORT:
                logger.severe("Aborted!");
                break;
            case REDIRECT:
                logger.severe("Redirect error!");
                break;
            case INVALID_CONTENT:
                logger.severe("Invalid_Content error!");
                break;
            case NONE:
                break;
            }
            this.errorThrowable = errorThrowable;
        }
    }

    /**
     * Die Connection wird entsprechend der start und endbytes neu aufgebaut.
     *
     * @param startByte
     * @param endByte
     * @param initialConnection
     */
    public HTTPChunk(ChunkRange chunkRange, URLConnectionAdapter reuseConnection, HTTPDownloader dl, Downloadable link) {
        super("DownloadChunkRAF:" + link.getName() + "|" + chunkRange);
        this.chunkRange = chunkRange;
        this.reuseConnection = reuseConnection;
        this.dl = dl;
        this.downloadable = link;
        this.logger = dl.getLogger();
        if (CrossSystem.isWindows()) {
            /* workaround for windows multimedia stuff. it reduces priority for non active(in background) stuff */
            try {
                this.setPriority(NORM_PRIORITY + 2);
            } catch (final Throwable e) {
            }
        }
    }

    private URLConnectionAdapter openConnection() {
        URLConnectionAdapter con = reuseConnection;
        if (con != null) {
            long[] responseRange = con.getRange();
            if (responseRange != null) {
                if (responseRange[0] != chunkRange.getFrom()) {
                    setError(ERROR.RANGE, new IOException("RangeError(From)"));
                    return null;
                }
                if (chunkRange.getTo() != null && chunkRange.getTo() >= 0 && responseRange[1] < chunkRange.getTo()) {
                    setError(ERROR.RANGE, new IOException("RangeError(To)"));
                    return null;
                }
                if (dl.getVerifiedFileSize() >= 0 && responseRange[2] != dl.getVerifiedFileSize()) {
                    if (responseRange[2] == -1) {
                        logger.info("RangeWarning(Size)");
                    } else {
                        setError(ERROR.RANGE, new IOException("RangeError(Size)"));
                        return null;
                    }
                }
            } else if (chunkRange.getFrom() > 0) {
                setError(ERROR.RANGE, new IOException("RangeError(Missing)"));
                return null;
            }
            return con;
        }
        try {
            Browser br = downloadable.getContextBrowser();
            br.setLogger(logger);
            Request request = dl.getRequest().cloneRequest();
            request.getHeaders().put(new HTTPHeader(HTTPConstants.HEADER_REQUEST_ACCEPT_ENCODING, "identity", false));
            request.setConnectTimeout(dl.getRequestTimeout());
            request.setReadTimeout(dl.getReadTimeout());
            if (!br.getHeaders().contains(HTTPConstants.HEADER_REQUEST_REFERER)) {
                /* only forward referer if referer already has been sent! */
                br.setCurrentURL(null);
            }
            boolean returnConnection = false;
            try {
                final String requestedRange;
                if (chunkRange.isRangeRequested()) {
                    requestedRange = dl.getRange(chunkRange.getFrom(), chunkRange.getTo());
                } else {
                    requestedRange = null;
                }
                if (requestedRange != null) {
                    request.getHeaders().put(new HTTPHeader(HTTPConstants.HEADER_REQUEST_RANGE, requestedRange, false));
                } else {
                    request.getHeaders().put(new HTTPHeader(HTTPConstants.HEADER_REQUEST_RANGE, null, false));
                }
                try {
                    downloadable.waitForNextConnectionAllowed();
                } catch (InterruptedException e1) {
                    if (downloadable.isInterrupted()) {
                        setError(ERROR.ABORT, e1);
                    }
                    return null;
                }
                if (isExternalyAborted()) {
                    setError(ERROR.ABORT, null);
                    return null;
                }
                con = br.openRequestConnection(request, false);
                int maxRedirects = 10;
                while (con.getRequest().getLocation() != null && maxRedirects-- > 0) {
                    con.disconnect();
                    request = br.createRedirectFollowingRequest(request);
                    try {
                        downloadable.waitForNextConnectionAllowed();
                    } catch (InterruptedException e1) {
                        if (downloadable.isInterrupted()) {
                            setError(ERROR.ABORT, e1);
                        }
                        return null;
                    }
                    if (isExternalyAborted()) {
                        setError(ERROR.ABORT, null);
                        return null;
                    }
                    con = br.openRequestConnection(request, false);
                }
                if (con.getRequest().getLocation() != null) {
                    setError(ERROR.REDIRECT, null);
                    return null;
                }
                final boolean sameContent = dl.validateConnection(con);
                long[] contentRange = con.getRange();
                if (sameContent) {
                    if (con.getResponseCode() == 200 || con.getResponseCode() == 206) {
                        if (requestedRange != null) {
                            if (contentRange != null) {
                                if (contentRange[0] != chunkRange.getFrom()) {
                                    setError(ERROR.RANGE, new IOException("RangeError(From)"));
                                    return null;
                                } else if (chunkRange.getTo() != null && chunkRange.getTo() >= 0 && contentRange[1] < chunkRange.getTo()) {
                                    setError(ERROR.RANGE, new IOException("RangeError(To)"));
                                    return null;
                                } else if (dl.getVerifiedFileSize() >= 0 && contentRange[2] != dl.getVerifiedFileSize()) {
                                    if (contentRange[2] == -1) {
                                        logger.info("RangeWarning(Size)");
                                    } else {
                                        setError(ERROR.RANGE, new IOException("RangeError(Size)"));
                                        return null;
                                    }
                                }
                            } else {
                                if (chunkRange.getFrom() > 0) {
                                    setError(ERROR.RANGE, new IOException("RangeError(Missing)"));
                                    return null;
                                }
                            }
                        } else {
                            new IOException("FIXME");
                        }
                        returnConnection = true;
                        return con;
                    }
                } else {
                    if (con.getResponseCode() == 200 || con.getResponseCode() == 206) {
                        setError(ERROR.INVALID_CONTENT, null);
                    } else {
                        setError(ERROR.INVALID_RESPONSE, null);
                    }
                    String contentType = con.getContentType();
                    if (contentType != null && contentType.contains("text") || contentType.contains("html")) {
                        br.followConnection();
                    }
                    return null;
                }
            } finally {
                if (!returnConnection) {
                    try {
                        /* always close connections that got opened */
                        con.disconnect();
                    } catch (Throwable e) {
                    }
                }
            }
        } catch (Throwable e) {
            setError(ERROR.CONNECTING, e);
        }
        return null;
    }

    private void downloadConnection(URLConnectionAdapter connection) {
        MeteredThrottledInputStream inputStream = null;
        try {
            connection.setReadTimeout(dl.getReadTimeout());
            connection.setConnectTimeout(dl.getRequestTimeout());
            InputStream is = connection.getInputStream();
            if (chunkRange.getLength() >= 0) {
                is = new LimitedInputStream(is, chunkRange.getLength());
            }
            inputStream = new MeteredThrottledInputStream(is, new AverageSpeedMeter(10)) {
                public void close() throws IOException {
                };
            };
            dl.getManagedConnetionHandler().addThrottledConnection(inputStream);
            int bytesRead = 0;
            final byte[] readBuffer = dl.getChunkBuffer(this);
            while (!isExternalyAborted()) {
                bytesRead = inputStream.read(readBuffer);
                if (bytesRead > 0) {
                    long overlap = dl.write(this, readBuffer, bytesRead, chunkRange.getPosition());
                    chunkRange.incLoaded(bytesRead);
                    if (overlap != bytesRead) {
                        logger.finer("Overlap Chunk: " + chunkRange + " (" + overlap + ")");
                        break;
                    }
                } else if (bytesRead == -1) {
                    chunkRange.setValidLoaded(true);
                    break;
                }
            }
        } catch (Throwable e) {
            setError(ERROR.DOWNLOADING, e);
        } finally {
            try {
                if (inputStream != null) {
                    try {
                        inputStream.setHandler(null);
                    } finally {
                        dl.getManagedConnetionHandler().removeThrottledConnection(inputStream);
                    }
                }
            } catch (final Throwable ignore) {
            }
        }
    }

    private boolean isExternalyAborted() {
        return dl.externalDownloadStop() || connectionClosedFlag.get();
    }

    /**
     * Thread runner
     */
    public void run() {
        try {
            if (runningFlag.get()) {
                run0();
            }
        } finally {
            runningFlag.set(false);
            dl.onChunkFinished(this);
        }
    }

    public void run0() {
        URLConnectionAdapter connection = null;
        try {
            logger.finer("Start Chunk: " + chunkRange);
            connection = openConnection();
            currentConnection = connection;
            if (connection != null && !isExternalyAborted()) {
                dl.updateCacheMapSize(connection);
                downloadConnection(connection);
            }
        } finally {
            currentConnection = null;
            logger.finer("Stop Chunk: " + chunkRange + " (ExternalAbort: " + isExternalyAborted() + "|ERROR:" + getError() + ")");
            try {
                if (connection != null && connection != reuseConnection) {
                    connection.disconnect();
                }
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public String toString() {
        return chunkRange.toString();
    }

    public void closeConnections() {
        if (runningFlag.get() && connectionClosedFlag.compareAndSet(false, true)) {
            if (isAlive() == false) {
                runningFlag.set(false);
            } else {
                setError(ERROR.ABORT, null);
            }
            try {
                URLConnectionAdapter lCurrentConnection = currentConnection;
                if (lCurrentConnection != null) {
                    lCurrentConnection.disconnect();
                }
            } catch (Throwable e) {
            }
        }
    }
}

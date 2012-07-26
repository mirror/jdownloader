package jd.plugins.download.raf;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedByInterruptException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Exceptions;
import org.appwork.utils.ReusableByteArrayOutputStreamPool;
import org.appwork.utils.ReusableByteArrayOutputStreamPool.ReusableByteArrayOutputStream;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.httpconnection.HTTPConnection.RequestMethod;
import org.appwork.utils.net.throttledconnection.MeteredThrottledInputStream;
import org.appwork.utils.speedmeter.AverageSpeedMeter;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.translate._JDT;

public class RAFChunk extends Thread {
    /**
     * Wird durch die Speedbegrenzung ein chunk uter diesen Wert geregelt, so wird er weggelassen. Sehr niedrig geregelte chunks haben einen kleinen Buffer und
     * eine sehr hohe Intervalzeit. Das fuehrt zu verstaerkt intervalartigem laden und ist ungewuenscht
     */
    public static final long                MIN_CHUNKSIZE        = 1 * 1024 * 1024;

    protected ReusableByteArrayOutputStream buffer               = null;

    private long                            chunkBytesLoaded     = 0;

    private URLConnectionAdapter            connection;

    private long                            endByte;

    private int                             id                   = -1;

    private MeteredThrottledInputStream     inputStream;

    private long                            startByte;
    private long                            bytes2Do             = -1;

    private boolean                         connectionclosed     = false;

    private boolean                         addedtoStartedChunks = false;

    private boolean                         clonedconnection     = false;

    private long                            requestedEndByte;

    private OldRAFDownload                  dl;

    private DownloadLink                    downloadLink;

    private Logger                          logger;

    private PluginForHost                   plugin;

    private LinkStatus                      linkStatus;

    /**
     * Die Connection wird entsprechend der start und endbytes neu aufgebaut.
     * 
     * @param startByte
     * @param endByte
     * @param connection
     */
    public RAFChunk(long startByte, long endByte, URLConnectionAdapter connection, OldRAFDownload dl, DownloadLink link) {
        super("DownloadChunkRAF");
        this.startByte = startByte;
        this.endByte = endByte;
        this.requestedEndByte = endByte;
        this.connection = connection;
        this.clonedconnection = false;
        this.dl = dl;
        this.downloadLink = link;
        this.plugin = link.getLivePlugin();
        this.logger = dl.getLogger();
        this.linkStatus = link.getLinkStatus();
    }

    @Deprecated
    public int getMaximalSpeed() {
        return 0;
    }

    @Deprecated
    public void setMaximalSpeed(final int i) {
    }

    private void addChunkBytesLoaded(long limit) {
        chunkBytesLoaded += limit;
    }

    /**
     * is this Chunk using the root connection or a cloned one
     * 
     * @return
     */
    public boolean isClonedConnection() {
        return clonedconnection;
    }

    private void setChunkStartet() {
        /* Chunk kann nur einmal gestartet werden */
        if (addedtoStartedChunks) return;
        dl.getChunksStarted().incrementAndGet();
        addedtoStartedChunks = true;
    }

    /**
     * Gibt Fortschritt in % an (10000 entspricht 100%))
     * 
     * @return
     */
    public int getPercent() {
        return (int) (10000 * chunkBytesLoaded / Math.max(1, Math.max(chunkBytesLoaded, (endByte - startByte))));
    }

    /**
     * Kopiert die Verbindung. Es wird bis auf die Range und timeouts exakt die selbe Verbindung nochmals aufgebaut.
     * 
     * @param connection
     * @return
     */
    private URLConnectionAdapter copyConnection(URLConnectionAdapter connection) {
        try {
            while (downloadLink.getLivePlugin().waitForNextConnectionAllowed()) {
            }
        } catch (InterruptedException e) {
            return null;
        } catch (NullPointerException e) {
            if (downloadLink.getLivePlugin() == null) return null;
            throw e;
        }
        downloadLink.getLivePlugin().putLastConnectionTime(System.currentTimeMillis());
        long start = startByte;
        String end = (endByte > 0 ? endByte + 1 : "") + "";

        if (start == 0) {
            logger.finer("Takeover 0 Connection");
            return connection;
        }
        if (connection.getRange() != null && connection.getRange()[0] == (start)) {
            logger.finer("Takeover connection at " + connection.getRange()[0]);
            return connection;
        }

        try {
            /* only forward referer if referer already has been sent! */
            boolean forwardReferer = plugin.getBrowser().getHeaders().contains("Referer");
            Browser br = plugin.getBrowser().cloneBrowser();
            br.setReadTimeout(dl.getReadTimeout());
            br.setConnectTimeout(dl.getRequestTimeout());
            /* set requested range */

            Map<String, String> request = connection.getRequestProperties();
            if (request != null) {
                String value;
                for (Entry<String, String> next : request.entrySet()) {
                    if (next.getValue() == null) continue;
                    value = next.getValue().toString();
                    br.getHeaders().put(next.getKey(), value);
                }
            }
            if (!forwardReferer) {
                /* only forward referer if referer already has been sent! */
                br.setCurrentURL(null);
            }
            URLConnectionAdapter con = null;
            clonedconnection = true;
            if (connection.getRequestMethod() == RequestMethod.POST) {
                connection.getRequest().getHeaders().put("Range", "bytes=" + start + "-" + end);
                con = br.openRequestConnection(connection.getRequest());
            } else {
                br.getHeaders().put("Range", "bytes=" + start + "-" + end);
                con = br.openGetConnection(connection.getURL() + "");
            }
            if (!con.isOK()) {
                try {
                    /* always close connections that got opened */
                    con.disconnect();
                } catch (Throwable e) {
                }
                if (con.getResponseCode() != 416) {
                    dl.error(LinkStatus.ERROR_DOWNLOAD_FAILED, "Server: " + con.getResponseMessage());
                } else {
                    logger.warning("HTTP 416, maybe finished last chunk?");
                }
                return null;
            }
            if (con.getHeaderField("Location") != null) {
                try {
                    /* always close connections that got opened */
                    con.disconnect();
                } catch (Throwable e) {
                }
                dl.error(LinkStatus.ERROR_DOWNLOAD_FAILED, "Server: Redirect");
                return null;
            }
            return con;
        } catch (Exception e) {
            dl.addException(e);
            dl.error(LinkStatus.ERROR_RETRY, Exceptions.getStackTrace(e));
            LogSource.exception(logger, e);
        }
        return null;
    }

    /** Die eigentliche Downloadfunktion */
    private void download() {
        int flushLevel = 0;
        int flushTimeout = JsonConfig.create(GeneralSettings.class).getFlushBufferTimeout();
        try {
            int maxbuffersize = JsonConfig.create(GeneralSettings.class).getMaxBufferSize() * 1024;
            buffer = ReusableByteArrayOutputStreamPool.getReusableByteArrayOutputStream(Math.max(maxbuffersize, 10240), false);
            /*
             * now we calculate the max fill level when to force buffer flushing
             */
            flushLevel = Math.max((maxbuffersize / 100 * JsonConfig.create(GeneralSettings.class).getFlushBufferLevel()), 1);
        } catch (Throwable e) {
            dl.error(LinkStatus.ERROR_FATAL, _JDT._.download_error_message_outofmemory());
            return;
        }
        /* +1 because of startByte also gets loaded (startbyte till endbyte) */
        if (endByte > 0) bytes2Do = (endByte - startByte) + 1;
        try {
            connection.setReadTimeout(dl.getReadTimeout());
            connection.setConnectTimeout(dl.getRequestTimeout());
            inputStream = new MeteredThrottledInputStream(connection.getInputStream(), new AverageSpeedMeter(10));
            dl.getManagedConnetionHandler().addThrottledConnection(inputStream);
            int towrite = 0;
            int read = 0;
            boolean reachedEOF = false;
            long lastFlush = 0;
            while (!isExternalyAborted()) {
                try {
                    buffer.reset();
                    if (reachedEOF == true) {
                        /* we already reached EOF, so nothing more to read */
                        towrite = -1;
                    } else {
                        /* lets try to read some data */
                        towrite = 0;
                    }
                    lastFlush = System.currentTimeMillis();
                    while (!reachedEOF && buffer.free() > 0 && buffer.size() <= flushLevel) {
                        if (endByte > 0) {
                            /* read only as much as needed */
                            read = inputStream.read(buffer.getInternalBuffer(), buffer.size(), (int) Math.min(bytes2Do, (buffer.getInternalBuffer().length - buffer.size())));
                            if (read > 0) {
                                bytes2Do -= read;
                                if (bytes2Do == 0) {
                                    /* we reached our artificial EOF */
                                    reachedEOF = true;
                                }
                            }
                        } else {
                            /* read as much as possible */
                            read = inputStream.read(buffer.getInternalBuffer(), buffer.size(), (buffer.getInternalBuffer().length - buffer.size()));
                        }
                        if (read > 0) {
                            /* we read some data */
                            towrite += read;
                            dl.totalLinkBytesLoadedLive.getAndAdd(read);
                            buffer.setUsed(towrite);
                        } else if (read == -1) {
                            /* we reached EOF */
                            reachedEOF = true;
                        } else {
                            /*
                             * wait a moment, give system chance to fill up its buffers
                             */
                            synchronized (this) {
                                this.wait(500);
                            }
                        }
                        if (System.currentTimeMillis() - lastFlush > flushTimeout) {
                            /* we reached our flush timeout */
                            break;
                        }
                    }
                } catch (NullPointerException e) {
                    if (inputStream == null) {
                        /* connection is closed and steam is null */
                        if (!isExternalyAborted() && !connectionclosed) throw e;
                        towrite = -1;
                        break;
                    }
                    throw e;
                } catch (SocketException e2) {
                    if (!isExternalyAborted()) throw e2;
                    towrite = -1;
                    break;
                } catch (ClosedByInterruptException e) {
                    if (!isExternalyAborted()) {
                        logger.severe("Timeout detected");
                        dl.error(LinkStatus.ERROR_TIMEOUT_REACHED, null);
                    }
                    towrite = -1;
                    break;
                } catch (AsynchronousCloseException e3) {
                    if (!isExternalyAborted() && !connectionclosed) throw e3;
                    towrite = -1;
                    break;
                } catch (IOException e4) {
                    if (!isExternalyAborted() && !connectionclosed) throw e4;
                    towrite = -1;
                    break;
                }
                if (towrite == -1 || isExternalyAborted() || connectionclosed) {
                    break;
                }
                if (towrite > 0) {
                    dl.addToTotalLinkBytesLoaded(towrite, false);
                    addChunkBytesLoaded(towrite);
                    dl.writeBytes(this);
                }
                /* enough bytes loaded */
                if (bytes2Do == 0 && endByte > 0) break;
                if (getCurrentBytesPosition() > endByte && endByte > 0) {
                    break;
                }
            }
            if (getCurrentBytesPosition() < endByte && endByte > 0 || getCurrentBytesPosition() <= 0) {
                logger.warning("Download not finished. Loaded until now: " + getCurrentBytesPosition() + "/" + endByte);
                dl.error(LinkStatus.ERROR_DOWNLOAD_FAILED, _JDT._.download_error_message_incomplete());
            }
        } catch (FileNotFoundException e) {
            LogSource.exception(logger, e);
            logger.severe("file not found. " + e.getLocalizedMessage());
            dl.error(LinkStatus.ERROR_FILE_NOT_FOUND, null);
        } catch (SecurityException e) {
            LogSource.exception(logger, e);
            logger.severe("not enough rights to write the file. " + e.getLocalizedMessage());
            dl.error(LinkStatus.ERROR_LOCAL_IO, _JDT._.download_error_message_iopermissions());
        } catch (UnknownHostException e) {
            LogSource.exception(logger, e);
            linkStatus.setValue(10 * 60000l);
            dl.error(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, _JDT._.download_error_message_unavailable());
        } catch (IOException e) {
            LogSource.exception(logger, e);
            if (e.getMessage() != null && e.getMessage().contains("reset")) {
                logger.info("Connection reset: network problems!");
                linkStatus.setValue(1000l * 60 * 5);
                dl.error(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, _JDT._.download_error_message_networkreset());
            } else if (e.getMessage() != null && e.getMessage().indexOf("timed out") >= 0) {
                LogSource.exception(logger, e);
                logger.severe("Read timeout: network problems! (too many connections?, firewall/antivirus?)");
                dl.error(LinkStatus.ERROR_TIMEOUT_REACHED, _JDT._.download_error_message_networkreset());
            } else {
                LogSource.exception(logger, e);
                if (e.getMessage() != null && e.getMessage().contains("503")) {
                    linkStatus.setValue(10 * 60000l);
                    dl.error(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, _JDT._.download_error_message_unavailable());
                } else {
                    logger.severe("error occurred while writing to file. " + e.getMessage());
                    dl.error(LinkStatus.ERROR_LOCAL_IO, _JDT._.download_error_message_iopermissions());
                }
            }
        } catch (Exception e) {
            LogSource.exception(logger, e);
            dl.error(LinkStatus.ERROR_RETRY, Exceptions.getStackTrace(e));
            dl.addException(e);
        } finally {
            ReusableByteArrayOutputStreamPool.reuseReusableByteArrayOutputStream(buffer);
            buffer = null;
            try {
                inputStream.close();
            } catch (Throwable e) {
            } finally {
                inputStream = null;
            }
            try {
                if (this.clonedconnection) {
                    /* cloned connection, we can disconnect now */
                    this.connection.disconnect();
                    this.connection = null;
                }
            } catch (Throwable e) {
            }
        }
    }

    /**
     * Gibt die Geladenen ChunkBytes zurueck
     * 
     * @return
     */
    public long getBytesLoaded() {
        return getCurrentBytesPosition() - startByte;
    }

    public long getChunkSize() {
        return endByte - startByte + 1;
    }

    /**
     * Gibt die Aktuelle Endposition in der gesamtfile zurueck. Diese Methode gibt die Endposition unahaengig davon an Ob der aktuelle BUffer schon geschrieben
     * wurde oder nicht.
     * 
     * @return
     */
    public long getCurrentBytesPosition() {
        return startByte + chunkBytesLoaded;
    }

    public long getEndByte() {
        return endByte;
    }

    public int getID() {
        if (id < 0) {
            synchronized (dl.getChunks()) {
                id = dl.getChunks().indexOf(this);
            }
        }
        return id;
    }

    public long getStartByte() {
        return startByte;
    }

    /**
     * Gibt die Schreibposition des Chunks in der gesamtfile zurueck
     * 
     * @throws Exception
     */
    public long getWritePosition() throws Exception {
        long c = getCurrentBytesPosition();
        long l = buffer.size();
        return c - l;
    }

    /**
     * Gibt zurueck ob der chunk von einem externen eregniss unterbrochen wurde
     * 
     * @return
     */
    private boolean isExternalyAborted() {
        DownloadInterface dli = downloadLink.getDownloadInstance();
        SingleDownloadController sdc = downloadLink.getDownloadLinkController();
        return isInterrupted() || (dli != null && dli.externalDownloadStop()) || (sdc != null && sdc.isAborted());
    }

    /**
     * Thread runner
     */
    public void run() {
        try {
            run0();
            while (true) {
                /* wait for all chunks being started */
                if (dl.getChunksStarted().get() == dl.getChunkNum()) {
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
                /* external abort, proceed to close connection */
                if (this.isExternalyAborted()) break;
            }
        } finally {
            try {
                dl.addToChunksInProgress(-1);
            } catch (Throwable e) {
                LogSource.exception(logger, e);
            }
            dl.onChunkFinished();
        }
    }

    public void run0() {
        try {
            logger.finer("Start Chunk " + getID() + " : " + startByte + " - " + endByte);
            if (startByte >= endByte && endByte > 0 || startByte >= dl.getFileSize() && endByte > 0) return;
            if (dl.getChunkNum() > 1) {
                /* we requested multiple chunks */
                connection = copyConnection(connection);
                if (connection == null) {
                    /* copy failed!, lets check if this is the last chunk */
                    if (startByte >= dl.getFileSize() && dl.getFileSize() > 0) {
                        downloadLink.getLinkStatus().removeStatus(LinkStatus.ERROR_DOWNLOAD_FAILED);
                        logger.finer("Is no error. Last chunk is just already finished");
                        return;
                    }
                    dl.error(LinkStatus.ERROR_DOWNLOAD_FAILED, _JDT._.download_error_message_connectioncopyerror());
                    if (!this.isExternalyAborted()) logger.severe("ERROR Chunk (connection copy failed) " + getID());
                    return;
                }
            } else if (startByte > 0) {
                connection = copyConnection(connection);
                // workaround fuer fertigen endchunk
                if (startByte >= dl.getFileSize() && dl.getFileSize() > 0) {
                    downloadLink.getLinkStatus().removeStatus(LinkStatus.ERROR_DOWNLOAD_FAILED);
                    logger.finer("Is no error. Last chunk is just already finished");
                    return;
                }
                if (connection == null) {
                    dl.error(LinkStatus.ERROR_DOWNLOAD_FAILED, _JDT._.download_error_message_connectioncopyerror());
                    if (!this.isExternalyAborted()) logger.severe("ERROR Chunk (connection copy failed) " + getID());
                    return;
                }

                if (startByte > 0 && (connection.getHeaderField("Content-Range") == null || connection.getHeaderField("Content-Range").length() == 0)) {
                    dl.error(LinkStatus.ERROR_DOWNLOAD_FAILED, _JDT._.download_error_message_rangeheaders());
                    logger.severe("ERROR Chunk (no range header response)" + getID() + connection.toString());
                    // logger.finest(connection.toString());
                    return;
                }
            }
            long[] ContentRange = connection.getRange();
            if (startByte >= 0) {
                /* startByte >0, we should have a Content-Range in response! */
                if (ContentRange != null && ContentRange.length == 3) {
                    endByte = ContentRange[1];
                } else if (dl.getChunkNum() > 1) {
                    /* WTF? no Content-Range response available! */
                    if (connection.getLongContentLength() == startByte) {
                        /*
                         * Content-Length equals startByte -> Chunk is Complete!
                         */
                        return;
                    }
                    logger.severe("ERROR Chunk (range header parse error)" + getID() + connection.toString());
                    dl.error(LinkStatus.ERROR_DOWNLOAD_FAILED, _JDT._.download_error_message_rangeheaderparseerror() + connection.getHeaderField("Content-Range"));
                    return;
                } else {
                    /* only one chunk requested, set correct endByte */
                    endByte = connection.getLongContentLength() - 1;
                }
            } else if (ContentRange != null) {
                /*
                 * we did not request a range but got a content-range response,WTF?!
                 */
                logger.severe("No Range Request->Content-Range Response?!");
                endByte = ContentRange[1];
            }
            if (endByte <= 0) {
                /* endByte not yet set!, use Content-Length */
                endByte = connection.getLongContentLength() - 1;
            }
            long cRequestedEndByte = requestedEndByte + 1;
            if (cRequestedEndByte > 0 && endByte > cRequestedEndByte) {
                if (this.getID() == 0) {
                    logger.info("First Connection->Content-Range(" + endByte + ") is larger than requested (" + cRequestedEndByte + ")! Truncate it");
                } else {
                    logger.info(this.getID() + ". Connection->Content-Range(" + endByte + ") is larger than requested (" + cRequestedEndByte + ")! Truncate it");
                }
                endByte = cRequestedEndByte;
            }
            dl.addChunksDownloading(+1);
            setChunkStartet();
            download();
            dl.addChunksDownloading(-1);
            logger.finer("Chunk finished " + getID() + " " + getBytesLoaded() + " bytes");
        } finally {
            setChunkStartet();
            try {
                /* we can close cloned connections here */
                if (this.clonedconnection) {
                    connection.disconnect();
                    connection = null;
                }
            } catch (Throwable e) {
            }
        }
    }

    /**
     * Setzt die anzahl der schon geladenen partbytes. Ist fuer resume wichtig.
     * 
     * @param loaded
     */
    public void setLoaded(long loaded) {
        loaded = Math.max(0, loaded);
        dl.addToTotalLinkBytesLoaded(loaded, true);
    }

    public void startChunk() {
        start();
    }

    public void closeConnections() {
        connectionclosed = true;
        try {
            inputStream.close();
        } catch (Throwable e) {
        } finally {
            inputStream = null;
        }
        try {
            connection.disconnect();
        } catch (Throwable e) {
        } finally {
            connection = null;
        }
    }

    public MeteredThrottledInputStream getInputStream() {
        return inputStream;
    }

    @Deprecated
    public void setInProgress(boolean b) {
    }

}

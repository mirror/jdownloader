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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.DownloadWatchDog.DISKSPACECHECK;
import jd.controlling.downloadcontroller.ManagedThrottledConnectionHandler;
import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.RAFDownload;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Exceptions;
import org.appwork.utils.Hash;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.statistics.StatsManager;
import org.jdownloader.translate._JDT;

public class OldRAFDownload extends DownloadInterface {

    public static final Object HASHCHECKLOCK            = new Object();
    private RandomAccessFile   outputPartFile;
    private File               outputCompleteFile;
    private boolean            connected;
    private Vector<RAFChunk>   chunks                   = new Vector<RAFChunk>();
    private int                chunksDownloading        = 0;

    private int                chunksInProgress         = 0;

    protected long             totalLinkBytesLoaded     = 0;
    protected AtomicLong       totalLinkBytesLoadedLive = new AtomicLong(0);

    private AtomicInteger      chunksStarted            = new AtomicInteger(0);

    public AtomicInteger getChunksStarted() {
        return chunksStarted;
    }

    private Vector<Integer>                     errors                   = new Vector<Integer>();

    private Vector<Exception>                   exceptions               = null;

    private int                                 readTimeout              = 100000;
    private int                                 requestTimeout           = 100000;

    /** normal stop of download (eg manually or reconnect request) */
    private volatile boolean                    externalStop             = false;

    private boolean                             waitFlag                 = true;

    private boolean                             fatalErrorOccured        = false;

    protected int                               chunkNum                 = 1;
    private boolean                             resume                   = false;
    private boolean                             resumable                = false;

    protected boolean                           dlAlreadyFinished        = false;
    protected Browser                           browser;
    protected URLConnectionAdapter              connection;

    protected DownloadLink                      downloadLink;

    protected LinkStatus                        linkStatus;
    public Logger                               logger;
    protected PluginForHost                     plugin;

    public static final String                  PROPERTY_DOFILESIZECHECK = "DOFILESIZECHECK";
    protected Request                           request                  = null;
    protected ManagedThrottledConnectionHandler connectionHandler        = null;
    private long                                startTimeStamp           = -1;
    private long                                sizeBefore               = 0;

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
        return this.resumable;
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

    public synchronized void addToChunksInProgress(long i) {
        chunksInProgress += i;
    }

    public OldRAFDownload(PluginForHost plugin, DownloadLink downloadLink, Request request) throws IOException, PluginException {
        downloadLink.setDownloadInstance(this);
        connectionHandler = new ManagedThrottledConnectionHandler(downloadLink);
        this.downloadLink = downloadLink;
        this.plugin = plugin;
        logger = plugin.getLogger();
        linkStatus = downloadLink.getLinkStatus();
        linkStatus.setStatusText(_JDT._.download_connection_normal());
        browser = plugin.getBrowser().cloneBrowser();
        requestTimeout = JsonConfig.create(GeneralSettings.class).getHttpConnectTimeout();
        readTimeout = JsonConfig.create(GeneralSettings.class).getHttpReadTimeout();
        this.request = request;
    }

    public Vector<Exception> getExceptions() {
        return exceptions;
    }

    /**
     * Public for dummy mode
     * 
     * @param i
     */
    public synchronized void addChunksDownloading(long i) {
        chunksDownloading += i;
    }

    /**
     * File soll resumed werden
     */
    public void setResume(boolean value) {
        resume = value;
        resumable = value;
        if (value && !checkResumabled()) {
            logger.warning("Resumepoint not valid");
        }
        downloadLink.setResumeable(value);
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
        /* we have to update cookie for used browser instance here */
        br.updateCookies(request);
        return ret;
    }

    public void setFilesizeCheck(boolean b) {
        this.downloadLink.setProperty(PROPERTY_DOFILESIZECHECK, b);
    }

    public URLConnectionAdapter connect() throws Exception {
        logger.finer("Connect...");
        if (request == null) throw new IllegalStateException("Wrong Mode. Instance is in direct Connection mode");
        this.connected = true;
        boolean resumed = false;
        if (this.isRangeRequestSupported() && this.checkResumabled()) {
            /* we can continue to resume the download */
            logger.finer(".....connectResumable");
            resumed = connectResumable();
        } else {
            long verifiedFileSize = downloadLink.getVerifiedFileSize();
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
                browser.connect(request);
            }
        }
        if (this.plugin.getBrowser().isDebug()) logger.finest("\r\n" + request.printHeaders());
        connection = request.getHttpConnection();
        if (request.getLocation() != null) throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, BrowserAdapter.ERROR_REDIRECTED);
        if (connection.getRange() != null) {
            /* we have a range response, let's use it */
            if (connection.getRange()[2] > 0) {
                this.setFilesizeCheck(true);
                this.downloadLink.setDownloadSize(connection.getRange()[2]);
            }
        } else if (resumed == false && connection.getLongContentLength() > 0 && connection.isOK()) {
            this.setFilesizeCheck(true);
            this.downloadLink.setDownloadSize(connection.getLongContentLength());
        }
        if (connection.getResponseCode() == 416 && resumed == true && downloadLink.getChunksProgress().length == 1 && downloadLink.getVerifiedFileSize() == downloadLink.getChunksProgress()[0] + 1) {
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
        if (downloadLink.getChunksProgress() == null || downloadLink.getChunksProgress().length == 0) return false;
        long fileSize = getFileSize();
        int chunks = downloadLink.getChunksProgress().length;
        long part = fileSize / chunks;
        long dif;
        long last = -1;
        for (int i = 0; i < chunks; i++) {
            dif = downloadLink.getChunksProgress()[i] - i * part;
            if (dif < 0) return false;
            if (downloadLink.getChunksProgress()[i] <= last) return false;
            last = downloadLink.getChunksProgress()[i];
        }
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
                downloadLink.setChunksProgress(new long[] { downloadLink.getChunksProgress()[0] });
            }
            return true;
        }
        return false;
    }

    protected void connectFirstRange() throws IOException {
        long fileSize = getFileSize();
        long part = fileSize / this.getChunkNum();
        boolean verifiedSize = downloadLink.getVerifiedFileSize() > 0;
        boolean openRangeRequested = false;
        if (verifiedSize == false || this.getChunkNum() == 1) {
            /* we only request a single range */
            openRangeRequested = true;
            request.getHeaders().put("Range", "bytes= " + (0) + "-");
        } else {
            /* we request multiple ranges */
            openRangeRequested = false;
            request.getHeaders().put("Range", "bytes=" + (0) + "-" + (part - 1));
        }
        browser.connect(request);
        if (request.getHttpConnection().getResponseCode() == 416) {
            logger.warning("HTTP/1.1 416 Requested Range Not Satisfiable");
            if (this.plugin.getBrowser().isDebug()) logger.finest("\r\n" + request.printHeaders());
            throw new IllegalStateException("HTTP/1.1 416 Requested Range Not Satisfiable");
        } else if (request.getHttpConnection().getRange() == null) {
            logger.warning("No Chunkload");
            setChunkNum(1);
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
                throw new IllegalStateException("Range Error. Requested " + request.getHeaders().get("Range") + " Got range: " + request.getHttpConnection().getHeaderField("Content-Range"));
            }
        }
    }

    /**
     * Setzt im Downloadlink und PLugin die entsprechende Fehlerids
     */
    public boolean handleErrors() {
        if (externalDownloadStop()) return false;
        if (doFilesizeCheck() && (totalLinkBytesLoaded <= 0 || totalLinkBytesLoaded != getFileSize() && getFileSize() > 0)) {
            if (totalLinkBytesLoaded > getFileSize()) {
                /*
                 * workaround for old bug deep in this downloadsystem. more data got loaded (maybe just counting bug) than filesize. but in most cases the file
                 * is okay! WONTFIX because new downloadsystem is on its way
                 */
                logger.severe("Filesize: " + getFileSize() + " Loaded: " + totalLinkBytesLoaded);
                if (!linkStatus.isFailed()) {
                    linkStatus.setStatus(LinkStatus.FINISHED);
                }
                return true;
            }
            logger.severe("Filesize: " + getFileSize() + " Loaded: " + totalLinkBytesLoaded);
            logger.severe("DOWNLOAD INCOMPLETE DUE TO FILESIZECHECK");
            error(LinkStatus.ERROR_DOWNLOAD_INCOMPLETE, _JDT._.download_error_message_incomplete());
            return false;
        }

        if (getExceptions() != null && getExceptions().size() > 0) {
            error(LinkStatus.ERROR_RETRY, _JDT._.download_error_message_incomplete());
            return false;
        }
        if (!linkStatus.isFailed()) {
            linkStatus.setStatus(LinkStatus.FINISHED);
        }
        return true;
    }

    protected void addException(Exception e) {
        if (exceptions == null) {
            exceptions = new Vector<Exception>();
        }
        exceptions.add(e);
    }

    /**
     * ueber error() kann ein fehler gemeldet werden. DIe Methode entscheided dann ob dieser fehler zu einem Abbruch fuehren muss
     */
    protected synchronized void error(int id, String string) {
        /* if we recieved external stop, then we dont have to handle errors */
        if (externalDownloadStop()) return;

        logger.severe("Error occured (" + id + "): ");

        if (errors.indexOf(id) < 0) errors.add(id);
        if (fatalErrorOccured) return;

        linkStatus.addStatus(id);

        linkStatus.setErrorMessage(string);
        switch (id) {
        case LinkStatus.ERROR_RETRY:
        case LinkStatus.ERROR_FATAL:
        case LinkStatus.ERROR_TIMEOUT_REACHED:
        case LinkStatus.ERROR_FILE_NOT_FOUND:
        case LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE:
        case LinkStatus.ERROR_LOCAL_IO:
        case LinkStatus.ERROR_NO_CONNECTION:
        case LinkStatus.ERROR_ALREADYEXISTS:
        case LinkStatus.ERROR_DOWNLOAD_FAILED:
            fatalErrorOccured = true;
            terminate();
        }
    }

    /**
     * Wartet bis alle Chunks fertig sind, aktuelisiert den downloadlink regelmaesig und fordert beim Controller eine aktualisierung des links an
     */
    protected void onChunkFinished() {
        synchronized (this) {
            if (waitFlag) {
                waitFlag = false;
                notify();
            }
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
            startTimeStamp = System.currentTimeMillis();
            sizeBefore = downloadLink.getDownloadCurrent();
            linkStatus.addStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS);
            try {
                downloadLink.getDownloadLinkController().getConnectionHandler().addConnectionHandler(this.getManagedConnetionHandler());
            } catch (final Throwable e) {
            }
            if (this.dlAlreadyFinished == false) {
                try {
                    boolean watchAsYouDownloadTypeLink = (Boolean) downloadLink.getFilePackage().getProperty(org.jdownloader.extensions.neembuu.NeembuuExtension.WATCH_AS_YOU_DOWNLOAD_KEY, false);
                    boolean initiatedByWatchAsYouDownloadAction = (Boolean) downloadLink.getFilePackage().getProperty(org.jdownloader.extensions.neembuu.NeembuuExtension.INITIATED_BY_WATCH_ACTION, false);
                    if (watchAsYouDownloadTypeLink && initiatedByWatchAsYouDownloadAction) {
                        org.jdownloader.extensions.neembuu.DownloadSession downloadSession = new org.jdownloader.extensions.neembuu.DownloadSession(downloadLink, this, this.plugin, this.getConnection(), this.browser.cloneBrowser());
                        if (org.jdownloader.extensions.neembuu.NeembuuExtension.tryHandle(downloadSession)) {
                            org.jdownloader.extensions.neembuu.WatchAsYouDownloadSession watchAsYouDownloadSession = downloadSession.getWatchAsYouDownloadSession();
                            try {
                                watchAsYouDownloadSession.waitForDownloadToFinish();
                            } catch (Exception a) {
                                logger.log(Level.SEVERE, "Exception in waiting for neembuu watch as you download", a);
                                // if we do not return, normal download would start.
                                return false;
                            }
                            return true;
                        }
                        int o = 0;
                        try {
                            o = Dialog.I().showConfirmDialog(Dialog.LOGIC_COUNTDOWN, org.jdownloader.extensions.neembuu.translate._NT._.neembuu_could_not_handle_title(), org.jdownloader.extensions.neembuu.translate._NT._.neembuu_could_not_handle_message());
                        } catch (Exception a) {
                            o = Dialog.RETURN_CANCEL;
                        }
                        if (o == Dialog.RETURN_CANCEL) return false;
                        logger.severe("Neembuu could not handle this link/filehost. Using default download system.");
                    } else if (watchAsYouDownloadTypeLink && !initiatedByWatchAsYouDownloadAction) {
                        // Neembuu downloads should start if and only if user clicks
                        // watch as you download
                        // action in the context menu. We don't want neembuu to
                        // start when user pressed
                        // forced download, or simple download button.
                        // We shall skip this link and disable all in this
                        // filepackage
                        for (DownloadLink dl : downloadLink.getFilePackage().getChildren()) {
                            dl.setEnabled(false);
                        }
                        /* TODO: change me */
                        throw new PluginException(LinkStatus.ERROR_FATAL);
                    }
                } catch (final Throwable e) {
                    logger.severe("Exception in neembuu watch as you download");
                    LogSource.exception(logger, e);
                }
            }
            logger.finer("Start Download");
            if (this.dlAlreadyFinished == true) {
                downloadLink.setAvailable(true);
                logger.finer("DownloadAlreadyFinished workaround");
                linkStatus.setStatus(LinkStatus.FINISHED);
                return true;
            }
            if (!connected) connect();
            if (connection != null && connection.getHeaderField("Content-Encoding") != null && connection.getHeaderField("Content-Encoding").equalsIgnoreCase("gzip")) {
                /* GZIP Encoding kann weder chunk noch resume */
                setResume(false);
                setChunkNum(1);
            }
            // Erst hier Dateinamen holen, somit umgeht man das Problem das bei
            // mehrfachAufruf von connect entstehen kann
            if (this.downloadLink.getFinalFileName() == null && ((connection != null && connection.isContentDisposition()) || this.allowFilenameFromURL)) {
                String name = Plugin.getFileNameFromHeader(connection);
                if (this.fixWrongContentDispositionHeader) {
                    this.downloadLink.setFinalFileName(Encoding.htmlDecode(name));
                } else {
                    this.downloadLink.setFinalFileName(name);
                }
            }
            downloadLink.getLinkStatus().setStatusText(null);
            if (connection == null || !connection.isOK()) {
                if (connection != null) logger.finest(connection.toString());
                try {
                    connection.disconnect();
                } catch (final Throwable e) {
                }
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
            }
            if (connection.getHeaderField("Location") != null) {
                error(LinkStatus.ERROR_PLUGIN_DEFECT, "Sent a redirect to Downloadinterface");
                return false;
            }
            if (downloadLink.getVerifiedFileSize() < 0) {
                /* we don't have a verified filesize yet, let's check if we have it now! */
                if (connection.getRange() != null) {
                    if (connection.getRange()[2] > 0) {
                        downloadLink.setVerifiedFileSize(connection.getRange()[2]);
                    }
                } else if (connection.getRequestProperty("Range") == null && connection.getLongContentLength() > 0 && connection.isOK()) {
                    downloadLink.setVerifiedFileSize(connection.getLongContentLength());
                }
            }
            if (DownloadWatchDog.preDownloadCheckFailed(downloadLink)) return false;
            setupChunks();
            /* download in progress so file should be online ;) */
            downloadLink.setAvailable(true);
            waitForChunks();
            onChunksReady();
            return handleErrors();
        } catch (Exception e) {
            if (e instanceof FileNotFoundException) {
                this.error(LinkStatus.ERROR_LOCAL_IO, _JDT._.download_error_message_localio(e.getMessage()));
            } else {
                LogSource.exception(logger, e);
            }
            handleErrors();
            return false;
        } finally {
            try {
                downloadLink.getDownloadLinkController().getConnectionHandler().removeConnectionHandler(this.getManagedConnetionHandler());
            } catch (final Throwable e) {
            }
            linkStatus.removeStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS);
            try {
                this.connection.disconnect();
            } catch (Throwable e) {
            }
            /* make sure file is really closed */
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
        long verifiedFileSize = downloadLink.getVerifiedFileSize();
        if (verifiedFileSize >= 0) return verifiedFileSize;
        if (connection != null) {
            if (connection.getRange() != null) {
                /* we have a range response, let's use it */
                if (connection.getRange()[2] > 0) return connection.getRange()[2];
            }
            if (connection.getRequestProperty("Range") == null && connection.getLongContentLength() > 0 && connection.isOK()) {
                /* we have no range request and connection is okay, so we can use the content-length */
                return connection.getLongContentLength();
            }
        }
        if (downloadLink.getDownloadSize() > 0) return downloadLink.getDownloadSize();
        return -1;
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
        return externalStop;
    }

    protected boolean doFilesizeCheck() {
        return this.downloadLink.getBooleanProperty(PROPERTY_DOFILESIZECHECK, true);
    }

    /** signal that we stopped download external */
    public synchronized void stopDownload() {
        if (externalStop) return;
        logger.info("externalStop recieved");
        externalStop = true;
        terminate();
    }

    protected void waitForChunks() {
        try {
            logger.finer("Wait for chunks");
            while (chunksInProgress > 0) {
                synchronized (this) {
                    if (waitFlag) {
                        try {
                            this.wait();
                        } catch (final InterruptedException e) {
                        } catch (Exception e) {
                            // logger.log(Level.SEVERE,"Exception occurred",e);
                            terminate();
                            return;
                        }
                    }
                }
                waitFlag = true;
            }
            /* set the *real loaded* bytes here */
            downloadLink.setDownloadCurrent(totalLinkBytesLoaded);
        } catch (Throwable e) {
            LogSource.exception(logger, e);
        }
    }

    /**
     * Bricht den Download komplett ab.
     */
    protected void terminate() {
        if (!externalDownloadStop()) logger.severe("A critical Downloaderror occured. Terminate...");
        int oldSize = -1;
        ArrayList<RAFChunk> stopChunks = new ArrayList<RAFChunk>();
        while (true) {
            oldSize = stopChunks.size();
            synchronized (chunks) {
                stopChunks = new ArrayList<RAFChunk>(chunks);
            }
            boolean allClosed = true;
            if (stopChunks.size() != oldSize) {
                /* new Chunks found in this loop */
                allClosed = false;
            }
            for (RAFChunk ch : stopChunks) {
                try {
                    if (ch.getInputStream() != null) allClosed = false;
                    ch.closeConnections();
                } catch (final Throwable e) {
                }
                ch.interrupt();
            }
            if (allClosed) break;
            try {
                Thread.sleep(200);
            } catch (final InterruptedException e) {
                break;
            }
        }
    }

    protected boolean connectResumable() throws IOException {
        // TODO: endrange pruefen
        long[] chunkProgress = downloadLink.getChunksProgress();
        String start, end;
        start = end = "";
        boolean rangeRequested = false;
        logger.info("chunksProgress: " + Arrays.toString(chunkProgress));
        if (downloadLink.getVerifiedFileSize() > 0) {
            start = chunkProgress[0] == 0 ? "0" : (chunkProgress[0] + 1) + "";
            end = (getFileSize() / chunkProgress.length) + "";
        } else {
            start = chunkProgress[0] == 0 ? "0" : (chunkProgress[0] + 1) + "";
            end = chunkProgress.length > 1 ? (chunkProgress[1] + 1) + "" : "";
        }
        if (downloadLink.getVerifiedFileSize() < 0 && start.equals("0")) {
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
        browser.connect(request);
        return rangeRequested;
    }

    /**
     * Setzt den aktuellen readtimeout(nur vor dem dl start)
     */
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    /**
     * Setzt vor ! dem download dden requesttimeout. Sollte nicht zu niedrig sein weil sonst das automatische kopieren der Connections fehl schlaegt.,
     */
    public void setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public Vector<RAFChunk> getChunks() {
        return chunks;
    }

    protected void onChunksReady() {
        logger.info("Close connections if they are not closed yet");
        try {
            for (RAFChunk c : this.getChunks()) {
                c.closeConnections();
            }
        } finally {
            logger.info("Close File. Let AV programs run");
            try {
                outputPartFile.close();
            } catch (Throwable e) {
            }
        }
        downloadLink.getLinkStatus().setStatusText(null);
        if (!handleErrors()) return;
        try {
            File part = new File(outputCompleteFile.getAbsolutePath() + ".part");
            /* lets check the hash/crc/sfv */
            if (JsonConfig.create(GeneralSettings.class).isHashCheckEnabled()) {
                synchronized (HASHCHECKLOCK) {
                    /*
                     * we only want one hashcheck running at the same time. many finished downloads can cause heavy diskusage here
                     */
                    String hash = null;
                    String type = null;
                    Boolean success = null;

                    // StatsManager
                    if ((hash = downloadLink.getMD5Hash()) != null && hash.length() == 32) {
                        /* MD5 Check */
                        type = "MD5";
                        downloadLink.getLinkStatus().setStatusText(_JDT._.system_download_doCRC2("MD5"));
                        String hashFile = Hash.getMD5(part);
                        success = hash.equalsIgnoreCase(hashFile);
                    } else if (!StringUtils.isEmpty(hash = downloadLink.getSha1Hash()) && hash.length() == 40) {
                        /* SHA1 Check */
                        type = "SHA1";
                        downloadLink.getLinkStatus().setStatusText(_JDT._.system_download_doCRC2("SHA1"));
                        String hashFile = Hash.getSHA1(part);
                        success = hash.equalsIgnoreCase(hashFile);
                    } else if ((hash = new Regex(downloadLink.getName(), ".*?\\[([A-Fa-f0-9]{8})\\]").getMatch(0)) != null) {
                        type = "CRC32";
                        String hashFile = Long.toHexString(Hash.getCRC32(part));
                        success = hash.equalsIgnoreCase(hashFile);
                    } else {
                        DownloadLink sfv = null;
                        synchronized (downloadLink.getFilePackage()) {
                            for (DownloadLink dl : downloadLink.getFilePackage().getChildren()) {
                                if (dl.getFileOutput().toLowerCase().endsWith(".sfv")) {
                                    sfv = dl;
                                    break;
                                }
                            }
                        }
                        /* SFV File Available, lets use it */
                        if (sfv != null && sfv.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                            String sfvText = IO.readFileToString(new File(sfv.getFileOutput()));
                            if (sfvText != null) {
                                /* Delete comments */
                                sfvText = sfvText.replaceAll(";(.*?)[\r\n]{1,2}", "");
                                if (sfvText != null && sfvText.contains(downloadLink.getName())) {
                                    downloadLink.getLinkStatus().setStatusText(_JDT._.system_download_doCRC2("CRC32"));
                                    type = "CRC32";
                                    String crc = Long.toHexString(Hash.getCRC32(part));
                                    success = new Regex(sfvText, downloadLink.getName() + "\\s*" + crc).matches();
                                }
                            }
                        }
                    }
                    if (success != null) {
                        hashCheckFinished(type, success);
                    }
                }
            }

            boolean renameOkay = false;
            int retry = 5;
            /* rename part file to final filename */
            while (retry > 0) {
                /* first we try normal rename method */
                if ((renameOkay = part.renameTo(outputCompleteFile)) == true) {
                    break;
                }
                /* this may fail because something might lock the file */
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
                retry--;
            }
            /* Fallback */
            if (renameOkay == false) {
                /* rename failed, lets try fallback */
                logger.severe("Could not rename file " + part + " to " + outputCompleteFile);
                logger.severe("Try copy workaround!");
                try {
                    DISKSPACECHECK freeSpace = DownloadWatchDog.getInstance().checkFreeDiskSpace(part.getParentFile(), part.length());
                    if (DISKSPACECHECK.FAILED.equals(freeSpace)) throw new Throwable("not enough diskspace free to copy part to complete file");
                    IO.copyFile(part, outputCompleteFile);
                    renameOkay = true;
                    part.deleteOnExit();
                    part.delete();
                } catch (Throwable e) {
                    LogSource.exception(logger, e);
                    /* error happened, lets delete complete file */
                    if (outputCompleteFile.exists() && outputCompleteFile.length() != part.length()) {
                        outputCompleteFile.delete();
                        outputCompleteFile.deleteOnExit();
                    }
                }
                if (!renameOkay) {
                    logger.severe("Copy workaround: :(");
                    error(LinkStatus.ERROR_LOCAL_IO, _JDT._.system_download_errors_couldnotrename());
                } else {
                    logger.severe("Copy workaround: :)");
                }
            }
            if (renameOkay) {

                if (StatsManager.I().isEnabled()) {
                    long speed = 0;
                    long startDelay = -1;
                    try {
                        speed = (outputCompleteFile.length() - Math.max(0, sizeBefore)) / ((System.currentTimeMillis() - getStartTimeStamp()) / 1000);
                    } catch (final Throwable e) {
                        LogSource.exception(logger, e);
                    }
                    try {
                        startDelay = System.currentTimeMillis() - downloadLink.getDownloadLinkController().getStartTimestamp();
                    } catch (final Throwable e) {
                        LogSource.exception(logger, e);
                    }
                    StatsManager.I().onFileDownloaded(outputCompleteFile, downloadLink, speed, startDelay, getChunks().size());
                }

                /* save absolutepath as final location property */
                downloadLink.setProperty(DownloadLink.PROPERTY_FINALLOCATION, outputCompleteFile.getAbsolutePath());
                Date last = TimeFormatter.parseDateString(connection.getHeaderField("Last-Modified"));
                if (last != null && JsonConfig.create(GeneralSettings.class).isUseOriginalLastModified()) {
                    /* set original lastModified timestamp */
                    outputCompleteFile.setLastModified(last.getTime());
                } else {
                    /* set current timestamp as lastModified timestamp */
                    outputCompleteFile.setLastModified(System.currentTimeMillis());
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception", e);
            addException(e);
        }
    }

    private void hashCheckFinished(String hashType, boolean success) {
        logger.info(hashType + "-Check: " + (success ? "ok" : "failed"));
        if (success) {
            downloadLink.getLinkStatus().setStatusText(_JDT._.system_download_doCRC2_success(hashType));
        } else {
            String error = _JDT._.system_download_doCRC2_failed(hashType);
            downloadLink.getLinkStatus().removeStatus(LinkStatus.FINISHED);
            downloadLink.getLinkStatus().setStatusText(error);
            downloadLink.getLinkStatus().setValue(LinkStatus.VALUE_FAILED_HASH);
            error(LinkStatus.ERROR_DOWNLOAD_FAILED, error);
        }
    }

    protected void setupChunks() throws Exception {
        try {
            if (isRangeRequestSupported() && checkResumabled()) {
                logger.finer("Setup resume");
                this.setupResume();
            } else {
                logger.finer("Setup virgin download");
                this.setupVirginStart();
            }
        } catch (Exception e) {
            try {
                logger.info("CLOSE HD FILE");
                outputPartFile.close();
            } catch (Throwable e2) {
            }
            addException(e);
            throw e;
        }
    }

    private void setupVirginStart() throws FileNotFoundException {
        RAFChunk chunk;
        totalLinkBytesLoaded = 0;
        downloadLink.setDownloadCurrent(0);
        long partSize = getFileSize() / getChunkNum();
        if (connection.getRange() != null) {
            if ((connection.getRange()[1] == connection.getRange()[2] - 1) || (connection.getRange()[1] == connection.getRange()[2])) {
                logger.warning("Chunkload protection. this may cause traffic errors");
                partSize = getFileSize() / getChunkNum();
            } else {
                // Falls schon der 1. range angefordert wurde.... werden die
                // restlichen chunks angepasst
                partSize = (getFileSize() - connection.getLongContentLength()) / (getChunkNum() - 1);
            }
        }
        if (partSize <= 0) {
            logger.warning("Could not get Filesize.... reset chunks to 1");
            setChunkNum(1);
        }
        logger.finer("Start Download in " + getChunkNum() + " chunks. Chunksize: " + partSize);

        createOutputChannel();
        downloadLink.setChunksProgress(new long[chunkNum]);

        addToChunksInProgress(getChunkNum());
        int start = 0;

        long rangePosition = 0;
        if (connection.getRange() != null && connection.getRange()[1] != connection.getRange()[2] - 1) {
            // Erster range schon angefordert

            chunk = new RAFChunk(0, rangePosition = connection.getRange()[1], connection, this, downloadLink);
            rangePosition++;
            logger.finer("Setup chunk 0: " + chunk);
            addChunk(chunk);
            start++;
        }

        for (int i = start; i < getChunkNum(); i++) {
            if (i == getChunkNum() - 1) {
                chunk = new RAFChunk(rangePosition, -1, connection, this, downloadLink);
            } else {
                chunk = new RAFChunk(rangePosition, rangePosition + partSize - 1, connection, this, downloadLink);
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
        }
        chunk.startChunk();
    }

    protected void addChunk(Chunk chunk) {
        throw new WTFException("This should not happen!");
    }

    protected boolean writeChunkBytes(final Chunk chunk) {
        throw new WTFException("This should not happen!");
    }

    private void createOutputChannel() throws FileNotFoundException {
        outputCompleteFile = new File(downloadLink.getFileOutput());
        if (!outputCompleteFile.getParentFile().exists()) {
            outputCompleteFile.getParentFile().mkdirs();
        }
        outputPartFile = new RandomAccessFile(outputCompleteFile.getAbsolutePath() + ".part", "rw");
    }

    private void setupResume() throws FileNotFoundException {
        long parts = getFileSize() / getChunkNum();
        logger.info("Resume: " + getFileSize() + " partsize: " + parts);
        RAFChunk chunk;
        this.createOutputChannel();
        addToChunksInProgress(getChunkNum());

        for (int i = 0; i < getChunkNum(); i++) {
            if (i == getChunkNum() - 1) {
                chunk = new RAFChunk(downloadLink.getChunksProgress()[i] == 0 ? 0 : downloadLink.getChunksProgress()[i] + 1, -1, connection, this, downloadLink);
                chunk.setLoaded((downloadLink.getChunksProgress()[i] - i * parts + 1));
            } else {
                chunk = new RAFChunk(downloadLink.getChunksProgress()[i] == 0 ? 0 : downloadLink.getChunksProgress()[i] + 1, (i + 1) * parts - 1, connection, this, downloadLink);
                chunk.setLoaded((downloadLink.getChunksProgress()[i] - i * parts + 1));
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
        if (updateLiveData) totalLinkBytesLoadedLive.addAndGet(block);
    }

    protected boolean writeChunkBytes(RAFChunk chunk) {
        try {
            synchronized (outputPartFile) {
                outputPartFile.seek(chunk.getWritePosition());
                outputPartFile.write(chunk.buffer.getInternalBuffer(), 0, chunk.buffer.size());
                if (chunk.getID() >= 0) {
                    downloadLink.getChunksProgress()[chunk.getID()] = chunk.getCurrentBytesPosition() - 1;
                }
            }
            DownloadController.getInstance().requestSaving(true);
            return true;
        } catch (Exception e) {
            LogSource.exception(logger, e);
            error(LinkStatus.ERROR_LOCAL_IO, Exceptions.getStackTrace(e));
            addException(e);
            return false;
        }
    }

    /**
     * 
     * @param downloadLink
     *            downloadlink der geladne werden soll (wird zur darstellung verwendet)
     * @param request
     *            Verbindung die geladen werden soll
     * @param b
     *            Resumefaehige verbindung
     * @param i
     *            max chunks. fuer negative werte wirden die chunks aus der config verwendet. Bsp: -3 : Min(3,Configwert);
     * @return
     * @throws IOException
     * @throws PluginException
     */
    public static DownloadInterface download(DownloadLink downloadLink, Request request, boolean b, int i) throws IOException, PluginException {
        /* disable gzip, because current downloadsystem cannot handle it correct */
        request.getHeaders().put("Accept-Encoding", null);
        RAFDownload dl = new RAFDownload(downloadLink.getLivePlugin(), downloadLink, request);
        PluginForHost plugin = downloadLink.getLivePlugin();
        if (plugin != null) plugin.setDownloadInterface(dl);
        if (i == 0) {
            dl.setChunkNum(JsonConfig.create(GeneralSettings.class).getMaxChunksPerFile());
        } else {
            dl.setChunkNum(i < 0 ? Math.min(i * -1, JsonConfig.create(GeneralSettings.class).getMaxChunksPerFile()) : i);
        }
        dl.setResume(b);
        return dl;

    }

    public static DownloadInterface download(DownloadLink downloadLink, Request request) throws Exception {
        return download(downloadLink, request, false, 1);
    }

    public void cleanupDownladInterface() {
        try {
            this.outputPartFile.close();
        } catch (Throwable e) {
        }
    }

    public Logger getLogger() {
        return logger;
    }

    @Override
    public void close() {
    }

}
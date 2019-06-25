package jd.plugins.download;

import java.awt.Color;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.rmi.ConnectException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import jd.controlling.downloadcontroller.DiskSpaceReservation;
import jd.controlling.downloadcontroller.ExceptionRunnable;
import jd.controlling.downloadcontroller.FileIsLockedException;
import jd.controlling.downloadcontroller.ManagedThrottledConnectionHandler;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.SimpleFTP;
import jd.nutils.SimpleFTP.STATE;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.httpconnection.SocketStreamInterface;
import org.appwork.utils.net.socketconnection.SocketConnection;
import org.appwork.utils.net.throttledconnection.MeteredThrottledInputStream;
import org.appwork.utils.speedmeter.AverageSpeedMeter;
import org.jdownloader.plugins.DownloadPluginProgress;
import org.jdownloader.plugins.SkipReason;
import org.jdownloader.plugins.SkipReasonException;
import org.jdownloader.translate._JDT;

public class SimpleFTPDownloadInterface extends DownloadInterface {
    private final Downloadable                      downloadable;
    private final ManagedThrottledConnectionHandler connectionHandler;
    private final LogInterface                      logger;
    private final SimpleFTP                         simpleFTP;
    private String                                  filePath;
    private final AtomicBoolean                     abort                    = new AtomicBoolean(false);
    private final AtomicBoolean                     terminated               = new AtomicBoolean(false);
    private File                                    outputCompleteFile;
    private File                                    outputFinalCompleteFile;
    private File                                    outputPartFile;
    protected PluginException                       caughtPluginException    = null;
    protected long                                  totalLinkBytesLoaded     = -1;
    protected final AtomicLong                      totalLinkBytesLoadedLive = new AtomicLong(0);
    private long                                    startTimeStamp           = -1;
    private boolean                                 resumed                  = false;

    public SimpleFTPDownloadInterface(SimpleFTP simpleFTP, final DownloadLink link, String filePath) {
        connectionHandler = new ManagedThrottledConnectionHandler();
        final String host = SocketConnection.getHostName(simpleFTP.getControlSocket().getSocket().getRemoteSocketAddress());
        downloadable = new DownloadLinkDownloadable(link) {
            @Override
            public boolean isResumable() {
                return link.getBooleanProperty("RESUME", true);
            }

            @Override
            public void setResumeable(boolean value) {
                link.setProperty("RESUME", value);
                super.setResumeable(value);
            }

            @Override
            public String getHost() {
                return host;
            }
        };
        if (!link.hasProperty(DownloadLink.PROPERTY_RESUMEABLE)) {
            downloadable.setResumeable(true);
        }
        this.filePath = filePath;
        logger = downloadable.getLogger();
        downloadable.setDownloadInterface(this);
        this.simpleFTP = simpleFTP;
    }

    @Override
    public ManagedThrottledConnectionHandler getManagedConnetionHandler() {
        return connectionHandler;
    }

    private void createOutputFiles() throws SkipReasonException {
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

    protected void download(String filename, boolean resume) throws IOException, PluginException, SkipReasonException {
        final File file = outputPartFile;
        if (!simpleFTP.isBinary()) {
            logger.info("Warning: Download in ASCII mode may fail!");
        }
        final InetSocketAddress pasv = simpleFTP.pasv();
        resumed = false;
        if (resume) {
            final long resumePosition = file.length();
            if (resumePosition > 0) {
                resumed = true;
                totalLinkBytesLoadedLive.set(resumePosition);
                simpleFTP.sendLine("REST " + resumePosition);
                try {
                    simpleFTP.readLines(new int[] { 350 }, "Resume not supported");
                    downloadable.setResumeable(true);
                } catch (final IOException e) {
                    cleanupDownladInterface();
                    if (e.getMessage().contains("Resume not")) {
                        file.delete();
                        downloadable.setResumeable(false);
                        throw new PluginException(LinkStatus.ERROR_RETRY);
                    }
                    throw e;
                }
            }
        }
        final RandomAccessFile raf;
        try {
            raf = IO.open(file, "rw");
        } catch (final IOException e) {
            throw new SkipReasonException(SkipReason.INVALID_DESTINATION, e);
        }
        SocketStreamInterface dataSocket = null;
        MeteredThrottledInputStream input = null;
        try {
            dataSocket = simpleFTP.createSocket(new InetSocketAddress(pasv.getHostName(), pasv.getPort()));
            dataSocket.getSocket().setSoTimeout(simpleFTP.getReadTimeout(STATE.DOWNLOADING));
            simpleFTP.sendLine("RETR " + filename);
            simpleFTP.readLines(new int[] { 150, 125 }, null);
            input = new MeteredThrottledInputStream(dataSocket.getInputStream(), new AverageSpeedMeter(10));
            connectionHandler.addThrottledConnection(input);
            if (resumed) {
                /* in case we do resume, reposition the writepointer */
                totalLinkBytesLoaded = file.length();
                raf.seek(totalLinkBytesLoaded);
            } else {
                totalLinkBytesLoaded = 0;
            }
            final byte[] buffer = new byte[32767];
            int bytesRead = 0;
            while ((bytesRead = input.read(buffer)) != -1) {
                if (abort.get()) {
                    break;
                }
                if (bytesRead > 0) {
                    raf.write(buffer, 0, bytesRead);
                    totalLinkBytesLoaded += bytesRead;
                    totalLinkBytesLoadedLive.addAndGet(bytesRead);
                }
            }
            /* max 10 seks wait for buggy servers */
            simpleFTP.getControlSocket().getSocket().setSoTimeout(simpleFTP.getReadTimeout(STATE.CLOSING));
            simpleFTP.shutDownSocket(dataSocket);
            input.close();
            try {
                simpleFTP.readLine();
            } catch (SocketTimeoutException e) {
                LogSource.exception(logger, e);
            }
        } catch (SocketTimeoutException e) {
            LogSource.exception(logger, e);
            error(new PluginException(LinkStatus.ERROR_DOWNLOAD_INCOMPLETE, _JDT.T.download_error_message_networkreset(), LinkStatus.VALUE_NETWORK_IO_ERROR));
        } catch (SocketException e) {
            LogSource.exception(logger, e);
            error(new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, _JDT.T.download_error_message_networkreset(), 1000l * 60 * 5));
        } catch (ConnectException e) {
            LogSource.exception(logger, e);
            error(new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, _JDT.T.download_error_message_networkreset(), 1000l * 60 * 5));
        } finally {
            close(raf);
            close(input);
            close(dataSocket);
            cleanupDownladInterface();
            if (totalLinkBytesLoaded >= 0) {
                downloadable.setDownloadBytesLoaded(totalLinkBytesLoaded);
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

    @Override
    public boolean startDownload() throws Exception {
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
                download(filePath, downloadable.isResumable());
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
        }
    }

    private boolean isDownloadComplete() throws Exception {
        final long verifiedFileSize = downloadable.getVerifiedFileSize();
        if (verifiedFileSize >= 0) {
            if (totalLinkBytesLoaded > verifiedFileSize) {
                if (resumed) {
                    logger.severe("It seems the ftp server has buggy REST support: transfered=" + totalLinkBytesLoaded + " fileSize=" + verifiedFileSize);
                    outputPartFile.delete();
                    downloadable.setDownloadBytesLoaded(0);
                    downloadable.setResumeable(false);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
            return totalLinkBytesLoaded == verifiedFileSize;
        }
        if (externalDownloadStop() == false && isTerminated() == false) {
            // no stop and not terminated, normal finish
            return true;
        }
        return false;
    }

    private boolean isTerminated() {
        return terminated.get();
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

    /**
     * ueber error() kann ein fehler gemeldet werden. DIe Methode entscheided dann ob dieser fehler zu einem Abbruch fuehren muss
     */
    private void error(PluginException pluginException) {
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

    private void cleanupDownladInterface() {
        try {
            downloadable.removeConnectionHandler(this.getManagedConnetionHandler());
        } catch (final Throwable e) {
        }
        try {
            this.simpleFTP.disconnect();
        } catch (Throwable e) {
        }
    }

    protected long getFileSize() {
        return downloadable.getVerifiedFileSize();
    }

    public boolean handleErrors() throws PluginException {
        if (externalDownloadStop()) {
            return false;
        }
        if (getFileSize() > 0 && totalLinkBytesLoaded != getFileSize()) {
            if (totalLinkBytesLoaded > getFileSize()) {
                /*
                 * workaround for old bug deep in this downloadsystem. more data got loaded (maybe just counting bug) than filesize. but in
                 * most cases the file is okay! WONTFIX because new downloadsystem is on its way
                 */
                logger.severe("Filesize: " + getFileSize() + " Loaded: " + totalLinkBytesLoaded);
                if (caughtPluginException == null) {
                    downloadable.setLinkStatus(LinkStatus.FINISHED);
                }
                return true;
            }
            logger.severe("Filesize: " + getFileSize() + " Loaded: " + totalLinkBytesLoaded);
            logger.severe("DOWNLOAD INCOMPLETE DUE TO FILESIZECHECK");
            if (caughtPluginException != null) {
                throw caughtPluginException;
            }
            throw new PluginException(LinkStatus.ERROR_DOWNLOAD_INCOMPLETE, _JDT.T.download_error_message_incomplete());
        }
        if (caughtPluginException == null) {
            downloadable.setLinkStatus(LinkStatus.FINISHED);
            downloadable.setVerifiedFileSize(outputCompleteFile.length());
            return true;
        } else {
            throw caughtPluginException;
        }
    }

    @Override
    public URLConnectionAdapter getConnection() {
        throw new WTFException("Not needed for SimpleFTPDownloadInterface");
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

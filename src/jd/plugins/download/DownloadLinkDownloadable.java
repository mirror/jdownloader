package jd.plugins.download;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

import jd.controlling.downloadcontroller.DiskSpaceManager.DISKSPACERESERVATIONRESULT;
import jd.controlling.downloadcontroller.DiskSpaceReservation;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.ExceptionRunnable;
import jd.controlling.downloadcontroller.FileIsLockedException;
import jd.controlling.downloadcontroller.ManagedThrottledConnectionHandler;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.DownloadLinkDatabindingInterface;
import jd.plugins.FilePackage;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.PluginProgress;
import jd.plugins.download.HashInfo.TYPE;

import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.plugins.FinalLinkState;
import org.jdownloader.plugins.HashCheckPluginProgress;
import org.jdownloader.plugins.SkipReason;
import org.jdownloader.plugins.SkipReasonException;

public class DownloadLinkDownloadable implements Downloadable {
    /**
     *
     */
    private final DownloadLink  downloadLink;
    private final PluginForHost plugin;

    public DownloadLinkDownloadable(DownloadLink downloadLink) {
        this.downloadLink = downloadLink;
        plugin = downloadLink.getLivePlugin();
    }

    public DownloadLink getDownloadLink() {
        return downloadLink;
    }

    @Override
    public void setResumeable(boolean value) {
        downloadLink.setResumeable(value);
    }

    @Override
    public Browser getContextBrowser() {
        return plugin.getBrowser().cloneBrowser();
    }

    @Override
    public LogInterface getLogger() {
        return plugin.getLogger();
    }

    @Override
    public void setDownloadInterface(DownloadInterface di) {
        plugin.setDownloadInterface(di);
    }

    @Override
    public long getVerifiedFileSize() {
        return downloadLink.getView().getBytesTotalVerified();
    }

    @Override
    public boolean isServerComaptibleForByteRangeRequest() {
        return downloadLink.getBooleanProperty("ServerComaptibleForByteRangeRequest", false);
    }

    @Override
    public String getHost() {
        final DownloadInterface dli = getDownloadInterface();
        if (dli != null) {
            final URLConnectionAdapter connection = dli.getConnection();
            if (connection != null) {
                return connection.getURL().getHost();
            }
        }
        return downloadLink.getHost();
    }

    @Override
    public boolean isDebug() {
        return this.plugin.getBrowser().isDebug();
    }

    @Override
    public void setDownloadTotalBytes(long l) {
        downloadLink.setDownloadSize(l);
    }

    public SingleDownloadController getDownloadLinkController() {
        return downloadLink.getDownloadLinkController();
    }

    @Override
    public void setLinkStatus(int finished) {
        getDownloadLinkController().getLinkStatus().setStatus(finished);
    }

    @Override
    public void setVerifiedFileSize(long length) {
        downloadLink.setVerifiedFileSize(length);
    }

    @Override
    public void validateLastChallengeResponse() {
        plugin.validateLastChallengeResponse();
    }

    @Override
    public void setConnectionHandler(ManagedThrottledConnectionHandler managedConnetionHandler) {
        getDownloadLinkController().getConnectionHandler().addConnectionHandler(managedConnetionHandler);
    }

    @Override
    public void removeConnectionHandler(ManagedThrottledConnectionHandler managedConnetionHandler) {
        getDownloadLinkController().getConnectionHandler().removeConnectionHandler(managedConnetionHandler);
    }

    @Override
    public void setAvailable(AvailableStatus status) {
        downloadLink.setAvailableStatus(status);
    }

    @Override
    public String getFinalFileName() {
        return downloadLink.getFinalFileName();
    }

    @Override
    public void setFinalFileName(String newfinalFileName) {
        downloadLink.setFinalFileName(newfinalFileName);
    }

    @Override
    public boolean checkIfWeCanWrite(final ExceptionRunnable runOkay, final ExceptionRunnable runFailed) throws Exception {
        final SingleDownloadController dlc = getDownloadLinkController();
        final AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        DownloadWatchDog.getInstance().localFileCheck(dlc, new ExceptionRunnable() {
            @Override
            public void run() throws Exception {
                runOkay.run();
                atomicBoolean.set(true);
            }
        }, runFailed);
        return atomicBoolean.get();
    }

    @Override
    public void lockFiles(File... files) throws FileIsLockedException {
        final SingleDownloadController dlc = getDownloadLinkController();
        for (File f : files) {
            dlc.lockFile(f);
        }
    }

    @Override
    public void unlockFiles(File... files) {
        final SingleDownloadController dlc = getDownloadLinkController();
        for (File f : files) {
            dlc.unlockFile(f);
        }
    }

    @Override
    public void addDownloadTime(long ms) {
        downloadLink.addDownloadTime(ms);
    }

    @Override
    public void setLinkStatusText(String label) {
        getDownloadLinkController().getLinkStatus().setStatusText(label);
    }

    @Override
    public long getDownloadTotalBytes() {
        return downloadLink.getView().getBytesTotalEstimated();
    }

    @Override
    public void setDownloadBytesLoaded(long bytes) {
        downloadLink.setDownloadCurrent(bytes);
    }

    @Override
    public boolean isHashCheckEnabled() {
        return downloadLink.getBooleanProperty("ALLOW_HASHCHECK", true);
    }

    @Override
    public String getName() {
        return downloadLink.getName();
    }

    @Override
    public long getKnownDownloadSize() {
        return downloadLink.getView().getBytesTotal();
    }

    @Override
    public void addPluginProgress(PluginProgress progress) {
        downloadLink.addPluginProgress(progress);
    }

    public HashResult getHashResult(HashInfo hashInfo, File outputPartFile) {
        if (hashInfo == null) {
            return null;
        }
        TYPE type = hashInfo.getType();
        final PluginProgress hashProgress = new HashCheckPluginProgress(outputPartFile, Color.YELLOW.darker(), type);
        hashProgress.setProgressSource(this);
        try {
            addPluginProgress(hashProgress);
            final byte[] b = new byte[32767];
            String hashFile = null;
            FileInputStream fis = null;
            int n = 0;
            int cur = 0;
            switch (type) {
            case MD5:
            case SHA1:
            case SHA256:
            case SHA512:
                DigestInputStream is = null;
                try {
                    is = new DigestInputStream(fis = new FileInputStream(outputPartFile), MessageDigest.getInstance(type.getDigest()));
                    while ((n = is.read(b)) >= 0) {
                        cur += n;
                        hashProgress.setCurrent(cur);
                    }
                    hashFile = HexFormatter.byteArrayToHex(is.getMessageDigest().digest());
                } catch (final Throwable e) {
                    LogSource.exception(getLogger(), e);
                } finally {
                    try {
                        is.close();
                    } catch (final Throwable e) {
                    }
                    try {
                        fis.close();
                    } catch (final Throwable e) {
                    }
                }
                break;
            case CRC32:
                CheckedInputStream cis = null;
                try {
                    fis = new FileInputStream(outputPartFile);
                    cis = new CheckedInputStream(fis, new CRC32());
                    while ((n = cis.read(b)) >= 0) {
                        cur += n;
                        hashProgress.setCurrent(cur);
                    }
                    long value = cis.getChecksum().getValue();
                    byte[] longBytes = new byte[] { (byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value };
                    hashFile = HexFormatter.byteArrayToHex(longBytes);
                } catch (final Throwable e) {
                    LogSource.exception(getLogger(), e);
                } finally {
                    try {
                        cis.close();
                    } catch (final Throwable e) {
                    }
                    try {
                        fis.close();
                    } catch (final Throwable e) {
                    }
                }
                break;
            }
            return new HashResult(hashInfo, hashFile);
        } finally {
            removePluginProgress(hashProgress);
        }
    }

    @Override
    public HashInfo getHashInfo() {
        final HashInfo hashInfo = downloadLink.getHashInfo();
        if (hashInfo != null) {
            return hashInfo;
        }
        final String name = getName();
        final List<HashInfo> hashInfos = new ArrayList<HashInfo>();
        for (final HashInfo.TYPE type : HashInfo.TYPE.values()) {
            if (!HashInfo.TYPE.NONE.equals(type)) {
                final String hash = new Regex(name, ".*?\\[([A-Fa-f0-9]{" + type.getSize() + "})\\]").getMatch(0);
                if (hash != null) {
                    hashInfos.add(new HashInfo(hash, type, false));
                }
            }
        }
        final FilePackage filePackage = downloadLink.getFilePackage();
        if (!FilePackage.isDefaultFilePackage(filePackage)) {
            final ArrayList<File> checkSumFiles = new ArrayList<File>();
            final boolean readL = filePackage.getModifyLock().readLock();
            try {
                for (final DownloadLink dl : filePackage.getChildren()) {
                    if (dl != downloadLink && FinalLinkState.CheckFinished(dl.getFinalLinkState())) {
                        final File checkSumFile = getFileOutput(dl, false);
                        final String fileName = checkSumFile.getName();
                        if (fileName.matches(".*\\.(sfv|md5|sha1|sha256|sha512)$") && checkSumFile.exists() && !checkSumFiles.contains(checkSumFile)) {
                            checkSumFiles.add(checkSumFile);
                        }
                    }
                }
            } finally {
                filePackage.getModifyLock().readUnlock(readL);
            }
            final File[] files = new File(filePackage.getDownloadDirectory()).listFiles();
            if (files != null) {
                for (final File file : files) {
                    final String fileName = file.getName();
                    if (fileName.matches(".*\\.(sfv|md5|sha1|sha256|sha512)$") && file.isFile() && !checkSumFiles.contains(file)) {
                        checkSumFiles.add(file);
                    }
                }
            }
            for (final File checkSumFile : checkSumFiles) {
                try {
                    final String content = IO.readFileToString(checkSumFile);
                    if (StringUtils.isNotEmpty(content)) {
                        final String lines[] = Regex.getLines(content);
                        for (final String line : lines) {
                            if (line.startsWith(";") || !line.contains(name)) {
                                continue;
                            }
                            for (final HashInfo.TYPE type : HashInfo.TYPE.values()) {
                                if (!HashInfo.TYPE.NONE.equals(type)) {
                                    final String hash = new Regex(line, "(?:^|\\s+)([A-Fa-f0-9]{" + type.getSize() + "})(\\s+|$)").getMatch(0);
                                    if (hash != null) {
                                        hashInfos.add(new HashInfo(hash, type));
                                    }
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    getLogger().log(e);
                }
            }
        }
        if (hashInfos.size() == 1) {
            return hashInfos.get(0);
        } else if (hashInfos.size() > 1) {
            HashInfo best = null;
            for (final HashInfo info : hashInfos) {
                if (best == null) {
                    best = info;
                } else if (info.isStrongerThan(best) && (info.isTrustworthy() || !best.isTrustworthy())) {
                    best = info;
                } else if (info.isTrustworthy() && !best.isTrustworthy()) {
                    best = info;
                }
            }
            return best;
        } else {
            return null;
        }
    }

    private File getFileOutput(DownloadLink link, boolean ignoreCustom) {
        final SingleDownloadController controller = link.getDownloadLinkController();
        if (controller == null) {
            return new File(link.getFileOutput(false, ignoreCustom));
        } else {
            return controller.getFileOutput(false, ignoreCustom);
        }
    }

    @Override
    @Deprecated
    public String getMD5Hash() {
        return downloadLink.getMD5Hash();
    }

    @Override
    @Deprecated
    public String getSha1Hash() {
        return downloadLink.getSha1Hash();
    }

    @Override
    @Deprecated
    public String getSha256Hash() {
        return downloadLink.getSha256Hash();
    }

    @Override
    @Deprecated
    public long[] getChunksProgress() {
        return downloadLink.getView().getChunksProgress();
    }

    @Override
    @Deprecated
    public void setChunksProgress(long[] ls) {
        downloadLink.setChunksProgress(ls);
    }

    public PluginForHost getPlugin() {
        return plugin;
    }

    @Override
    public void setHashResult(HashResult result) {
        getDownloadLinkController().setHashResult(result);
    }

    @Override
    public boolean rename(final File outputPartFile, final File outputCompleteFile) throws InterruptedException {
        boolean renameOkay = false;
        int retry = 5;
        /* rename part file to final filename */
        while (retry > 0) {
            /* first we try normal rename method */
            if ((renameOkay = outputPartFile.renameTo(outputCompleteFile)) == true) {
                break;
            }
            /* this may fail because something might lock the file */
            Thread.sleep(1000);
            retry--;
        }
        /* Fallback */
        if (renameOkay == false) {
            /* rename failed, lets try fallback */
            getLogger().severe("Could not rename file " + outputPartFile + " to " + outputCompleteFile);
            getLogger().severe("Try copy workaround!");
            DiskSpaceReservation reservation = new DiskSpaceReservation() {
                @Override
                public long getSize() {
                    return outputPartFile.length() - outputCompleteFile.length();
                }

                @Override
                public File getDestination() {
                    return outputCompleteFile;
                }
            };
            try {
                try {
                    DISKSPACERESERVATIONRESULT result = DownloadWatchDog.getInstance().getSession().getDiskSpaceManager().checkAndReserve(reservation, this);
                    switch (result) {
                    case OK:
                    case UNSUPPORTED:
                        IO.copyFile(outputPartFile, outputCompleteFile);
                        renameOkay = true;
                        outputPartFile.delete();
                        break;
                    }
                } finally {
                    DownloadWatchDog.getInstance().getSession().getDiskSpaceManager().free(reservation, this);
                }
            } catch (Throwable e) {
                LogSource.exception(getLogger(), e);
                /* error happened, lets delete complete file */
                if (outputCompleteFile.exists() && outputCompleteFile.length() != outputPartFile.length()) {
                    FileCreationManager.getInstance().delete(outputCompleteFile, null);
                }
            }
            if (!renameOkay) {
                getLogger().severe("Copy workaround: :(");
            } else {
                getLogger().severe("Copy workaround: :)");
            }
        }
        return renameOkay;
    }

    @Override
    public void waitForNextConnectionAllowed() throws InterruptedException {
        plugin.waitForNextConnectionAllowed(downloadLink);
    }

    @Override
    public boolean isInterrupted() {
        final SingleDownloadController sdc = getDownloadLinkController();
        return (sdc != null && sdc.isAborting());
    }

    @Override
    public String getFileOutput() {
        return getFileOutput(downloadLink, false).getAbsolutePath();
    }

    @Override
    public int getLinkStatus() {
        return getDownloadLinkController().getLinkStatus().getStatus();
    }

    @Override
    public String getFileOutputPart() {
        return getFileOutput() + ".part";
    }

    @Override
    public String getFinalFileOutput() {
        return getFileOutput(downloadLink, true).getAbsolutePath();
    }

    @Override
    public boolean isResumable() {
        return downloadLink.isResumeable();
    }

    @Override
    public DiskSpaceReservation createDiskSpaceReservation() {
        return new DiskSpaceReservation() {
            @Override
            public long getSize() {
                final File partFile = new File(getFileOutputPart());
                final long doneSize = Math.max((partFile.exists() ? partFile.length() : 0l), getDownloadBytesLoaded());
                return getKnownDownloadSize() - Math.max(0, doneSize);
            }

            @Override
            public File getDestination() {
                return new File(getFileOutput());
            }
        };
    }

    @Override
    public void checkAndReserve(DiskSpaceReservation reservation) throws Exception {
        DISKSPACERESERVATIONRESULT result = DownloadWatchDog.getInstance().getSession().getDiskSpaceManager().checkAndReserve(reservation, getDownloadLinkController());
        switch (result) {
        case FAILED:
            throw new SkipReasonException(SkipReason.DISK_FULL);
        case INVALIDDESTINATION:
            throw new SkipReasonException(SkipReason.INVALID_DESTINATION);
        }
    }

    @Override
    public void free(DiskSpaceReservation reservation) {
        DownloadWatchDog.getInstance().getSession().getDiskSpaceManager().free(reservation, getDownloadLinkController());
    }

    @Override
    public long getDownloadBytesLoaded() {
        return downloadLink.getView().getBytesLoaded();
    }

    @Override
    public boolean removePluginProgress(PluginProgress remove) {
        return downloadLink.removePluginProgress(remove);
    }

    @Override
    public <T> T getDataBindingInterface(Class<? extends DownloadLinkDatabindingInterface> T) {
        return (T) downloadLink.bindData(T);
    }

    @Override
    public void updateFinalFileName() {
        if (getFinalFileName() == null) {
            LogInterface logger = getLogger();
            DownloadInterface dl = getDownloadInterface();
            URLConnectionAdapter connection = getDownloadInterface().getConnection();
            logger.info("FinalFileName is not set yet!");
            if (connection.isContentDisposition() || dl.allowFilenameFromURL) {
                String name = Plugin.getFileNameFromHeader(connection);
                logger.info("FinalFileName: set to '" + name + "' from connection");
                if (dl.fixWrongContentDispositionHeader) {
                    setFinalFileName(Encoding.htmlDecode(name));
                } else {
                    setFinalFileName(name);
                }
            } else {
                String name = getName();
                logger.info("FinalFileName: set to '" + name + "' from plugin");
                setFinalFileName(name);
            }
        }
    }

    @Override
    public DownloadInterface getDownloadInterface() {
        return plugin.getDownloadInterface();
    }

    public void setHashInfo(final HashInfo hashInfo) {
        if (hashInfo != null && hashInfo.isTrustworthy() && getHashInfo() == null) {
            downloadLink.setHashInfo(hashInfo);
        }
    }

    @Override
    public int getChunks() {
        return downloadLink.getChunks();
    }
}
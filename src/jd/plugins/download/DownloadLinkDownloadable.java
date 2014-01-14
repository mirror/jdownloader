package jd.plugins.download;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.PluginForHost;
import jd.plugins.PluginProgress;
import jd.plugins.download.raf.HashResult;
import jd.plugins.download.raf.HashResult.TYPE;

import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.downloadcore.v15.Downloadable;
import org.jdownloader.downloadcore.v15.HashInfo;
import org.jdownloader.plugins.FinalLinkState;
import org.jdownloader.plugins.SkipReason;
import org.jdownloader.plugins.SkipReasonException;
import org.jdownloader.statistics.StatsManager;

public class DownloadLinkDownloadable implements Downloadable {
    /**
     * 
     */

    private final DownloadLink downloadLink;
    private PluginForHost      plugin;

    public DownloadLinkDownloadable(DownloadLink downloadLink) {
        this.downloadLink = downloadLink;
        plugin = downloadLink.getLivePlugin();
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
    public Logger getLogger() {
        return plugin.getLogger();
    }

    @Override
    public void setDownloadInterface(DownloadInterface di) {
        plugin.setDownloadInterface(di);
    }

    @Override
    public void setFilesizeCheck(boolean b) {
        downloadLink.setProperty(RAFDownload.PROPERTY_DOFILESIZECHECK, b);
    }

    @Override
    public long getVerifiedFileSize() {
        return downloadLink.getVerifiedFileSize();
    }

    @Override
    public boolean isServerComaptibleForByteRangeRequest() {
        return downloadLink.getBooleanProperty("ServerComaptibleForByteRangeRequest", false);
    }

    @Override
    public String getHost() {
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

    @Override
    public long[] getChunksProgress() {
        return downloadLink.getChunksProgress();
    }

    @Override
    public void setChunksProgress(long[] ls) {
        downloadLink.setChunksProgress(ls);
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
        return downloadLink.getDownloadSize();
    }

    @Override
    public boolean isDoFilesizeCheckEnabled() {
        return downloadLink.getBooleanProperty(RAFDownload.PROPERTY_DOFILESIZECHECK, true);
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
    public String getMD5Hash() {
        return downloadLink.getMD5Hash();
    }

    @Override
    public String getSha1Hash() {
        return downloadLink.getSha1Hash();
    }

    @Override
    public String getName() {
        return downloadLink.getName();
    }

    @Override
    public long getKnownDownloadSize() {
        return downloadLink.getKnownDownloadSize();
    }

    @Override
    public void setPluginProgress(PluginProgress progress) {
        downloadLink.setPluginProgress(progress);
    }

    public HashResult getHashResult(HashInfo hashInfo) {
        if (hashInfo == null) return null;
        TYPE type = hashInfo.getType();
        File outputPartFile = new File(getFileOutputPart());
        PluginProgress hashProgress = new HashCheckPluginProgress(outputPartFile, Color.YELLOW.darker(), type);
        hashProgress.setProgressSource(this);
        try {
            setPluginProgress(hashProgress);
            final byte[] b = new byte[32767];
            String hashFile = null;
            FileInputStream fis = null;
            int n = 0;
            int cur = 0;
            switch (type) {
            case MD5:
            case SHA1:
                try {
                    DigestInputStream is = new DigestInputStream(fis = new FileInputStream(outputPartFile), MessageDigest.getInstance(type.name()));
                    while ((n = is.read(b)) >= 0) {
                        cur += n;
                        hashProgress.setCurrent(cur);
                    }
                    hashFile = HexFormatter.byteArrayToHex(is.getMessageDigest().digest());
                } catch (final Throwable e) {
                    LogSource.exception(getLogger(), e);
                } finally {
                    try {
                        fis.close();
                    } catch (final Throwable e) {
                    }
                }
                break;
            case CRC32:
                try {
                    fis = new FileInputStream(outputPartFile);
                    CheckedInputStream cis = new CheckedInputStream(fis, new CRC32());
                    while ((n = cis.read(b)) >= 0) {
                        cur += n;
                        hashProgress.setCurrent(cur);
                    }
                    hashFile = Long.toHexString(cis.getChecksum().getValue());
                } catch (final Throwable e) {
                    LogSource.exception(getLogger(), e);
                } finally {
                    try {
                        fis.close();
                    } catch (final Throwable e) {
                    }
                }
                break;
            }
            return new HashResult(hashInfo.getHash(), hashFile, hashInfo.getType());
        } finally {
            setPluginProgress(null);
        }
    }

    @Override
    public HashInfo getHashInfo() {
        String hash;
        // StatsManager
        String name = getName();
        if ((hash = getMD5Hash()) != null && hash.length() == 32) {
            /* MD5 Check */

            return new HashInfo(hash, HashResult.TYPE.MD5);
        } else if (!StringUtils.isEmpty(hash = getSha1Hash()) && hash.length() == 40) {
            /* SHA1 Check */
            return new HashInfo(hash, HashResult.TYPE.SHA1);
        } else if ((hash = new Regex(name, ".*?\\[([A-Fa-f0-9]{8})\\]").getMatch(0)) != null) {
            return new HashInfo(hash, HashResult.TYPE.CRC32);
        } else {
            FilePackage filePackage = downloadLink.getFilePackage();
            if (!FilePackage.isDefaultFilePackage(filePackage)) {
                ArrayList<DownloadLink> SFVs = new ArrayList<DownloadLink>();
                boolean readL = filePackage.getModifyLock().readLock();
                try {
                    for (DownloadLink dl : filePackage.getChildren()) {
                        if (dl.getFileOutput().toLowerCase().endsWith(".sfv") && FinalLinkState.CheckFinished(dl.getFinalLinkState())) {
                            SFVs.add(dl);
                        }
                    }
                } finally {
                    filePackage.getModifyLock().readUnlock(readL);
                }
                /* SFV File Available, lets use it */
                for (DownloadLink SFV : SFVs) {
                    File file = new File(SFV.getFileOutput());
                    if (file.exists()) {
                        String sfvText;
                        try {
                            sfvText = IO.readFileToString(file);

                            if (sfvText != null) {
                                /* Delete comments */
                                sfvText = sfvText.replaceAll(";(.*?)[\r\n]{1,2}", "");
                                if (sfvText != null && sfvText.contains(name)) {
                                    hash = new Regex(sfvText, name + "\\s*([A-Fa-f0-9]{8})").getMatch(0);
                                    if (hash != null) { return new HashInfo(hash, HashResult.TYPE.CRC32); }
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return null;
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
    public void logStats(File outputCompleteFile, int chunksCount, long downloadTimeInMs) {
        if (StatsManager.I().isEnabled()) {
            long speed = 0;
            long startDelay = -1;
            try {
                speed = (outputCompleteFile.length() - Math.max(0, getDownloadLinkController().getSizeBefore())) / ((downloadTimeInMs) / 1000);
            } catch (final Throwable e) {
                // LogSource.exception(logger, e);
            }
            try {
                startDelay = System.currentTimeMillis() - getDownloadLinkController().getStartTimestamp();
            } catch (final Throwable e) {
                // LogSource.exception(logger, e);
            }
            StatsManager.I().onFileDownloaded(outputCompleteFile, downloadLink, speed, startDelay, chunksCount);
        }
    }

    @Override
    public void setFinalFileOutput(String absolutePath) {
        downloadLink.setFinalFileOutput(absolutePath);
    }

    @Override
    public void waitForNextConnectionAllowed() throws InterruptedException {
        plugin.waitForNextConnectionAllowed(downloadLink);
    }

    @Override
    public boolean isInterrupted() {
        SingleDownloadController sdc = getDownloadLinkController();
        return (sdc != null && sdc.isAborting());
    }

    @Override
    public String getFileOutput() {
        return downloadLink.getFileOutput();
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
        return downloadLink.getFileOutput(false, true);
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
                long doneSize = Math.max((partFile.exists() ? partFile.length() : 0l), getDownloadBytesLoaded());
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
        return downloadLink.getDownloadCurrent();
    }
}
package jd.plugins.download;

import java.io.File;

import jd.controlling.downloadcontroller.DiskSpaceReservation;
import jd.controlling.downloadcontroller.ExceptionRunnable;
import jd.controlling.downloadcontroller.FileIsLockedException;
import jd.controlling.downloadcontroller.ManagedThrottledConnectionHandler;
import jd.http.Browser;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.DownloadLinkDatabindingInterface;
import jd.plugins.PluginProgress;

import org.appwork.utils.logging2.LogInterface;

public interface Downloadable {
    void setResumeable(boolean value);

    Browser getContextBrowser();

    @Deprecated
    String getMD5Hash();

    @Deprecated
    public long[] getChunksProgress();

    @Deprecated
    public void setChunksProgress(long[] ls);

    @Deprecated
    String getSha1Hash();

    @Deprecated
    String getSha256Hash();

    LogInterface getLogger();

    void setDownloadInterface(DownloadInterface di);

    DownloadInterface getDownloadInterface();

    /**
     * return the verified(100%) size of final download
     *
     * @param bytes
     */
    long getVerifiedFileSize();

    boolean isServerComaptibleForByteRangeRequest();

    /**
     * Return the pluginhost. this can be the requested domain, but it may be a different domain as well
     *
     * @return
     */
    String getHost();

    boolean isDebug();

    public DiskSpaceReservation createDiskSpaceReservation();

    public void checkAndReserve(DiskSpaceReservation reservation) throws Exception;

    public void free(DiskSpaceReservation reservation);

    void setDownloadTotalBytes(long l);

    /**
     * returns the approximate(live) amount of downloaded bytes
     *
     * @return
     */
    public long getDownloadBytesLoaded();

    void setLinkStatus(int finished);

    /**
     * set the verified(100%) size of final download
     *
     * @param length
     */
    void setVerifiedFileSize(long length);

    void validateLastChallengeResponse();

    void setConnectionHandler(ManagedThrottledConnectionHandler managedConnetionHandler);

    void setAvailable(AvailableStatus status);

    String getFinalFileName();

    void setFinalFileName(String htmlDecode);

    void updateFinalFileName();

    boolean checkIfWeCanWrite(ExceptionRunnable runOkay, ExceptionRunnable runFailed) throws Exception;

    void lockFiles(File... files) throws FileIsLockedException;

    void unlockFiles(File... files);

    void addDownloadTime(long ms);

    void setLinkStatusText(String system_download_doCRC2_success);

    long getDownloadTotalBytes();

    /**
     * set the exact amount of bytes loaded
     *
     * @param bytes
     */
    void setDownloadBytesLoaded(long bytes);

    boolean isHashCheckEnabled();

    String getName();

    /**
     * returns the best known filesize (does not have to match the final downloadsize)
     *
     * @return
     */
    long getKnownDownloadSize();

    void addPluginProgress(PluginProgress downloadPluginProgress);

    boolean removePluginProgress(PluginProgress remove);

    HashInfo getHashInfo();

    public void setHashInfo(HashInfo hashInfo);

    void setHashResult(HashResult result);

    boolean rename(File from, File to) throws InterruptedException;

    // void setFinalFileOutput(String absolutePath);
    void removeConnectionHandler(ManagedThrottledConnectionHandler managedConnetionHandler);

    void waitForNextConnectionAllowed() throws InterruptedException;

    boolean isInterrupted();

    String getFileOutput();

    int getLinkStatus();

    HashResult getHashResult(HashInfo hashInfo, File file);

    String getFileOutputPart();

    String getFinalFileOutput();

    boolean isResumable();

    public <T> T getDataBindingInterface(Class<? extends DownloadLinkDatabindingInterface> T);

    public int getChunks();
}

package org.jdownloader.downloadcore.v15;

import java.io.File;
import java.util.logging.Logger;

import jd.controlling.downloadcontroller.ExceptionRunnable;
import jd.controlling.downloadcontroller.FileIsLockedException;
import jd.controlling.downloadcontroller.ManagedThrottledConnectionHandler;
import jd.http.Browser;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.PluginProgress;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.raf.HashResult;

public interface Downloadable {

    void setResumeable(boolean value);

    Browser getContextBrowser();

    Logger getLogger();

    void setDownloadInterface(DownloadInterface di);

    void setFilesizeCheck(boolean b);

    long getVerifiedFileSize();

    boolean isServerComaptibleForByteRangeRequest();

    /**
     * Return the pluginhost. this can be the requested domain, but it may be a different domain as well
     * 
     * @return
     */
    String getHost();

    boolean isDebug();

    void setDownloadTotalBytes(long l);

    long[] getChunksProgress();

    void setChunksProgress(long[] ls);

    void setLinkStatus(int finished);

    void setVerifiedFileSize(long length);

    void validateLastChallengeResponse();

    void setConnectionHandler(ManagedThrottledConnectionHandler managedConnetionHandler);

    void setAvailable(AvailableStatus status);

    String getFinalFileName();

    void setFinalFileName(String htmlDecode);

    boolean checkIfWeCanWrite(ExceptionRunnable runOkay, ExceptionRunnable runFailed) throws Exception;

    void lockFiles(File... files) throws FileIsLockedException;

    void unlockFiles(File... files);

    void addDownloadTime(long ms);

    void setLinkStatusText(String system_download_doCRC2_success);

    long getDownloadTotalBytes();

    boolean isDoFilesizeCheckEnabled();

    void setDownloadBytesLoaded(long bytes);

    boolean isHashCheckEnabled();

    String getMD5Hash();

    String getSha1Hash();

    String getName();

    long getKnownDownloadSize();

    void setPluginProgress(PluginProgress downloadPluginProgress);

    HashInfo getHashInfo();

    void setHashResult(HashResult result);

    boolean rename(File from, File to) throws InterruptedException;

    void logStats(File outputCompleteFile, int size, long downloadTimeInMS);

    void setFinalFileOutput(String absolutePath);

    void removeConnectionHandler(ManagedThrottledConnectionHandler managedConnetionHandler);

    void waitForNextConnectionAllowed() throws InterruptedException;

    boolean isInterrupted();

    String getFileOutput();

    int getLinkStatus();

    HashResult getHashResult(HashInfo hashInfo);

    String getFileOutputPart();

    String getFinalFileOutput();

    boolean isResumable();

}

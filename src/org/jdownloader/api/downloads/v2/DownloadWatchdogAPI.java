package org.jdownloader.api.downloads.v2;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.ApiNamespace;

@ApiNamespace("downloadcontroller")
public interface DownloadWatchdogAPI extends RemoteAPIInterface {

    void forceDownload(long[] linkIds, long[] packageIds);

    String getCurrentState();

    boolean pause(boolean value);

    /*
     * Info
     */
    int getSpeedInBps();

    /*
     * Controlls
     */
    boolean start();

    boolean stop();
}

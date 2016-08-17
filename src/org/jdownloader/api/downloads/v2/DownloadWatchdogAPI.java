package org.jdownloader.api.downloads.v2;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.APIParameterNames;
import org.appwork.remoteapi.annotations.ApiNamespace;

@ApiNamespace("downloadcontroller")
public interface DownloadWatchdogAPI extends RemoteAPIInterface {
    @APIParameterNames({ "linkIds", "packageIds" })
    void forceDownload(long[] linkIds, long[] packageIds);

    String getCurrentState();

    @APIParameterNames({ "value" })
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

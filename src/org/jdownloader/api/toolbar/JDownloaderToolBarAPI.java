package org.jdownloader.api.toolbar;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.annotations.ApiRawMethod;
import org.appwork.storage.config.annotations.AllowStorage;

@ApiNamespace("toolbar")
public interface JDownloaderToolBarAPI extends RemoteAPIInterface {

    @AllowStorage(value = { Object.class })
    public Object getStatus();

    public boolean isAvailable();

    public boolean startDownloads();

    public boolean stopDownloads();

    public boolean toggleDownloadSpeedLimit();

    public boolean togglePauseDownloads();

    public boolean togglePremium();

    public boolean toggleClipboardMonitoring();

    public boolean toggleAutomaticReconnect();

    public boolean toggleStopAfterCurrentDownload();

    public String specialURLHandling(String url);

    @AllowStorage(value = { Object.class })
    @ApiRawMethod(/* this method does not use json, it uses raw parameters */)
    public Object addLinksFromDOM(RemoteAPIRequest request);

    @AllowStorage(value = { Object.class })
    @ApiRawMethod(/* this method does not use json, it uses raw parameters */)
    public Object checkLinksFromDOM(RemoteAPIRequest request);

    public LinkCheckResult pollCheckedLinksFromDOM(String checkID);

    boolean triggerUpdate();
}

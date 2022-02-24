package org.jdownloader.api.toolbar;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.annotations.APIParameterNames;
import org.appwork.remoteapi.annotations.AllowNonStorableObjects;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.annotations.ApiRawMethod;

@ApiNamespace("toolbar")
public interface JDownloaderToolBarAPI extends RemoteAPIInterface {
    @AllowNonStorableObjects(value = { Object.class })
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

    @APIParameterNames({ "url" })
    public String specialURLHandling(String url);

    @AllowNonStorableObjects(value = { Object.class })
    @ApiRawMethod(/* this method does not use json, it uses raw parameters */)
    @APIParameterNames({ "request" })
    public Object addLinksFromDOM(RemoteAPIRequest request);

    @AllowNonStorableObjects(value = { Object.class })
    @ApiRawMethod(/* this method does not use json, it uses raw parameters */)
    @APIParameterNames({ "request" })
    public Object checkLinksFromDOM(RemoteAPIRequest request);

    @APIParameterNames({ "checkID" })
    public LinkCheckResult pollCheckedLinksFromDOM(String checkID);

    boolean triggerUpdate();
}

package org.jdownloader.extensions.jdanywhere.api.toolbar;

import org.appwork.remoteapi.ApiNamespace;
import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.storage.config.annotations.AllowStorage;

@ApiNamespace("mobile/toolbar")
public interface JDownloaderToolBarMobileAPI extends RemoteAPIInterface {

    @AllowStorage(value = { Object.class })
    public Object getStatus(final String username, final String password);

    public boolean startDownloads(final String username, final String password);

    public boolean stopDownloads(final String username, final String password);

    // pauses a download
    // used in iPhone-App
    public boolean pauseDownloads(final String username, final String password);

}

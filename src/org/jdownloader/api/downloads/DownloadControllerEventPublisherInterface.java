package org.jdownloader.api.downloads;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.ApiNamespace;

@ApiNamespace("downloadevents")
public interface DownloadControllerEventPublisherInterface extends RemoteAPIInterface {

    boolean setStatusEventInterval(long channelID, long interval);
}

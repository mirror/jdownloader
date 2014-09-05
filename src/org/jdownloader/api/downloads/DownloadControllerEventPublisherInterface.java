package org.jdownloader.api.downloads;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.exceptions.BadParameterException;
import org.jdownloader.api.downloads.v2.LinkQueryStorable;

@ApiNamespace("downloadevents")
public interface DownloadControllerEventPublisherInterface extends RemoteAPIInterface {

    boolean setStatusEventInterval(long channelID, long interval);

    DownloadListDiffStorable queryLinks(LinkQueryStorable queryParams, int diffID) throws BadParameterException;
}

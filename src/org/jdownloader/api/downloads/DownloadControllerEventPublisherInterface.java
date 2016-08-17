package org.jdownloader.api.downloads;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.APIParameterNames;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.exceptions.BadParameterException;
import org.jdownloader.api.downloads.v2.LinkQueryStorable;

@ApiNamespace("downloadevents")
public interface DownloadControllerEventPublisherInterface extends RemoteAPIInterface {
    @APIParameterNames({ "channelID", "interval" })
    boolean setStatusEventInterval(long channelID, long interval);

    @APIParameterNames({ "queryParams", "diffID" })
    DownloadListDiffStorable queryLinks(LinkQueryStorable queryParams, int diffID) throws BadParameterException;
}

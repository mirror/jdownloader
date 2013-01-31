package org.jdownloader.api.downloads;

import java.util.List;

import org.appwork.remoteapi.APIQuery;
import org.appwork.remoteapi.ApiNamespace;
import org.appwork.remoteapi.RemoteAPIInterface;

@ApiNamespace("downloads")
public interface DownloadsAPI extends RemoteAPIInterface {
    boolean start();

    boolean stop();

    boolean pause(Boolean value);

    List<FilePackageAPIStorable> queryPackages(APIQuery queryParams);

    List<DownloadLinkAPIStorable> queryLinks(APIQuery queryParams);

    boolean removeLinks(final List<Long> linkIds);

    boolean forceDownload(final List<Long> linkIds);

    int speed();

    boolean enableLinks(List<Long> linkIds);

    boolean disableLinks(List<Long> linkIds);
}

package org.jdownloader.api.downloads;

import java.util.List;

import org.appwork.remoteapi.APIQuery;
import org.appwork.remoteapi.ApiNamespace;
import org.appwork.remoteapi.RemoteAPIInterface;

@ApiNamespace("downloads")
public interface DownloadsAPI extends RemoteAPIInterface {
    /*
     * Controlls
     */
    boolean start();

    boolean stop();

    boolean pause(Boolean value);

    /*
     * Queries
     */
    List<FilePackageAPIStorable> queryPackages(APIQuery queryParams);

    List<DownloadLinkAPIStorable> queryLinks(APIQuery queryParams);

    /*
     * Functions
     */
    boolean removeLinks(final List<Long> linkIds);

    boolean forceDownload(final List<Long> linkIds);

    boolean enableLinks(List<Long> linkIds);

    boolean disableLinks(List<Long> linkIds);

    /*
     * Sorting
     */
    boolean moveTop(List<Long> linkIds);

    boolean moveBottom(List<Long> linkIds);

    boolean moveUp(List<Long> linkIds);

    boolean moveDown(List<Long> linkIds);

    /*
     * Info
     */
    int speed();
}

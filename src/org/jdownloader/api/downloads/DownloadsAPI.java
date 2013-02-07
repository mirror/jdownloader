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

    /**
     * Query Packages currently in downloads
     * 
     * Example:
     * http://localhost:3128/downloads/queryPackages?{"saveTo":true,"childCount":true,"hosts":true,"startAt":0,"maxResults":10}
     * 
     * Default fields returned: name, uuid
     * 
     * @param queryParams
     * Hashmap with the following allowed values:
     * 
     * Optional selectors:
     * startAt, Integer, index of first element to be returned
     * maxResults, Integer, total number of elements to be returned
     * 
     * Optional fields (Boolean):
     * saveTo
     * size
     * childCount
     * hosts
     * done
     * comment
     * enabled
     * 
     * @return
     */

    List<FilePackageAPIStorable> queryPackages(APIQuery queryParams);

    /**
     * Query Packages links in downloads
     * 
     * Example:
     * http://localhost:3128/downloads/queryLinks?{"packageUUIDs":[1358496436106,1358496436107],"enabled":true,"size":true,"host":true,"startAt":0,"maxResults":10}
     * 
     * Default fields returned: name, uuid, packageUUID
     * 
     * @param queryParams
     * Hashmap with the following allowed values:
     * 
     * Optional selectors:
     * packageUUIDs, List<Long>, links contained in the packages with given uuids are returned, if empty all links are returned
     * startAt, Integer, index of first element to be returned
     * maxResults, Integer, total number of elements to be returned
     * 
     * Optional fields (Boolean):
     * host
     * size
     * done
     * enabled
     * 
     * @return
     */
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
    boolean movePackages(APIQuery query);

    /*
     * Info
     */
    int speed();
}

package org.jdownloader.api.linkcollector;

import java.util.List;

import org.appwork.remoteapi.APIQuery;
import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.ApiNamespace;

@ApiNamespace("linkcollector")
public interface LinkCollectorAPI extends RemoteAPIInterface {

    /**
     * Query Packages currently in linkcollector
     * 
     * Example: http://localhost:3128/linkcollector/queryPackages?{"saveTo":true,"childCount":true,"hosts":true,"startAt":0,"maxResults":10}
     * 
     * Default fields returned: name uuid
     * 
     * @param queryParams
     *            Hashmap with the following allowed values:
     * 
     *            Optional selectors: startAt, Integer, index of first element to be returned maxResults, Integer, total number of elements
     *            to be returned
     * 
     *            Optional fields (Boolean): saveTo size childCount hosts availability
     * 
     * @return
     */
    List<CrawledPackageAPIStorable> queryPackages(APIQuery queryParams);

    /**
     * Query Links currently in linkcollector
     * 
     * Example:
     * http://localhost:3128/linkcollector/queryLinks?{"packageUUIDs":[1358496436106,1358496436107],"size":true,"host":true,"availability"
     * :true}
     * 
     * Default fields returned: name uuid
     * 
     * @param queryParams
     *            Hashmap with the following allowed values:
     * 
     *            Optional selectors: packageUUIDs, List<Long>, links contained in the packages with given uuids are returned, if empty all
     *            links are returned
     * 
     *            startAt, Integer, index of first element to be returned maxResults, Integer, total number of elements to be returned
     * 
     *            Optional fields (Boolean): size host availability
     * 
     * @return
     */
    List<CrawledLinkAPIStorable> queryLinks(APIQuery queryParams);

    /**
     * Example: http://localhost:3128/linkcollector/addLinks?"STRING_WITH_URLS"&""&""&""
     * 
     * @param link
     * @param packageName
     * @param archivePassword
     * @param linkPassword
     * @return
     */
    Boolean addLinks(String links, String packageName, String extractPassword, String downloadPassword, String destinationFolder);

    Boolean addLinks(String link, String packageName, String archivePassword, String linkPassword);

    /**
     * Same as addLinks, but autostarts the downloads if crawling was successful
     * 
     * @param links
     * @param packageName
     * @param extractPassword
     * @param downloadPassword
     * @return
     */
    Boolean addLinksAndStartDownload(String links, String packageName, String extractPassword, String downloadPassword);

    // Boolean uploadLinkContainer(RemoteAPIRequest request);

    Boolean startDownloads(List<Long> linkIds);

    Boolean startDownloads(List<Long> linkIds, List<Long> packageIds);

    Boolean removeLinks(List<Long> linkIds);

    Boolean removeLinks(List<Long> packageIds, List<Long> linkIds);

    boolean enableLinks(List<Long> linkIds);

    boolean enableLinks(List<Long> linkIds, List<Long> packageIds);

    boolean disableLinks(List<Long> linkIds);

    boolean disableLinks(List<Long> linkIds, List<Long> packageIds);

    boolean renameLink(Long packageId, Long linkId, String newName);

    boolean renamePackage(Long packageId, String newName);

    /*
     * Sorting
     */
    boolean movePackages(APIQuery query);

    boolean moveLinks(APIQuery query);

    /*
     * Changed?
     */
    Long getChildrenChanged(Long structureWatermark);

    /*
     * Selection bases
     */
    List<String> getDownloadFolderHistorySelectionBase();

    /*
     * Status
     */

    int packageCount();
}

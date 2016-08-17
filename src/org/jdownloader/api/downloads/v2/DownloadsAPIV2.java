package org.jdownloader.api.downloads.v2;

import java.util.List;
import java.util.Map;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.APIParameterNames;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.exceptions.BadParameterException;
import org.jdownloader.myjdownloader.client.bindings.CleanupActionOptions;
import org.jdownloader.myjdownloader.client.bindings.PriorityStorable;
import org.jdownloader.myjdownloader.client.bindings.SkipReasonStorable;
import org.jdownloader.myjdownloader.client.bindings.UrlDisplayTypeStorable;

@ApiNamespace("downloadsV2")
public interface DownloadsAPIV2 extends RemoteAPIInterface {
    @APIParameterNames({ "enabled", "linkIds", "packageIds" })
    void setEnabled(boolean enabled, long[] linkIds, long[] packageIds);

    @APIParameterNames({ "linkId", "packageId" })
    void setStopMark(long linkId, long packageId);

    void removeStopMark();

    long getStopMark();

    int packageCount();

    /**
     * Query Packages currently in downloads
     *
     * Example: http://localhost:3128/downloads/queryPackages?{"saveTo":true,"childCount":true,"hosts":true,"startAt":0,"maxResults":10}
     * Optionally you can query only certian packages:
     * http://localhost:3128/downloads/queryPackages?{"packageUUIDs":[1358496436106,1358496436107]}
     *
     * Default fields returned: name, uuid
     *
     * @param queryParams
     *            Hashmap with the following allowed values:
     *
     *            Optional selectors: startAt, Integer, index of first element to be returned maxResults, Integer, total number of elements
     *            to be returned
     *
     *            Optional fields (Boolean): saveTo size childCount hosts done comment enabled
     *
     * @return
     */
    @APIParameterNames({ "queryParams" })
    List<FilePackageAPIStorableV2> queryPackages(PackageQueryStorable queryParams) throws BadParameterException;
    @APIParameterNames({"linkIds","packageIds"})
    void removeLinks(final long[] linkIds, final long[] packageIds) throws BadParameterException;

    @APIParameterNames({ "packageId", "newName" })
    void renamePackage(Long packageId, String newName);

    @APIParameterNames({ "linkId", "newName" })
    void renameLink(Long linkId, String newName);

    @APIParameterNames({ "linkIds", "packageIds" })
    void resetLinks(long[] linkIds, long[] packageIds);

    /**
     * Returns the new Counter if the counter does not equal oldCounterValue If the value changed, we should update the structure. Use this
     * method to check whether a structure update is required or not
     *
     * @param oldCounterValue
     * @return
     */
    @APIParameterNames({ "oldCounterValue" })
    long getStructureChangeCounter(long oldCounterValue);

    @APIParameterNames({ "packageIds", "afterDestPackageId" })
    void movePackages(long[] packageIds, long afterDestPackageId);
    @APIParameterNames({"linkIds","afterLinkID","destPackageID"})
    void moveLinks(long[] linkIds, long afterLinkID, long destPackageID);

    /**
     * Query Packages links in downloads
     *
     * Example:
     * http://localhost:3128/downloads/queryLinks?{"packageUUIDs":[1358496436106,1358496436107],"enabled":true,"size":true,"host":true
     * ,"startAt":0,"maxResults":10}
     *
     * Default fields returned: name, uuid, packageUUID
     *
     * @param queryParams
     *            Hashmap with the following allowed values:
     *
     *            Optional selectors: packageUUIDs, long[], links contained in the packages with given uuids are returned, if empty all
     *            links are returned startAt, Integer, index of first element to be returned maxResults, Integer, total number of elements
     *            to be returned
     *
     *            Optional fields (Boolean): host size done enabled
     *
     * @return
     */
    @APIParameterNames({ "queryParams" })
    List<DownloadLinkAPIStorableV2> queryLinks(LinkQueryStorable queryParams) throws BadParameterException;

    @APIParameterNames({ "priority", "linkIds", "packageIds" })
    void setPriority(PriorityStorable priority, long[] linkIds, long[] packageIds) throws BadParameterException;

    @APIParameterNames({ "linkIds", "packageIds" })
    void resumeLinks(long[] linkIds, long[] packageIds) throws BadParameterException;

    @APIParameterNames({ "directory", "packageIds" })
    void setDownloadDirectory(String directory, long[] packageIds);

    DownloadLinkAPIStorableV2 getStopMarkedLink();

    @APIParameterNames({ "linkIds", "packageIds" })
    void startOnlineStatusCheck(long[] linkIds, long[] packageIds) throws BadParameterException;

    @APIParameterNames({ "linkIds", "packageIds", "urlDisplayType" })
    Map<String, List<Long>> getDownloadUrls(long[] linkIds, long[] packageIds, UrlDisplayTypeStorable[] urlDisplayType) throws BadParameterException;

    @APIParameterNames({ "linkIds", "pkgIds", "newPkgName", "downloadPath" })
    void movetoNewPackage(long[] linkIds, long[] pkgIds, String newPkgName, String downloadPath) throws BadParameterException;

    @APIParameterNames({ "linkIds", "pkgIds" })
    void splitPackageByHoster(long[] linkIds, long[] pkgIds);

    @APIParameterNames({ "linkIds", "packageIds", "action", "mode", "selectionType" })
    void cleanup(final long[] linkIds, final long[] packageIds, final CleanupActionOptions.Action action, final CleanupActionOptions.Mode mode, final CleanupActionOptions.SelectionType selectionType) throws BadParameterException;

    @APIParameterNames({ "linkIds", "packageIds", "pass" })
    boolean setDownloadPassword(long[] linkIds, long[] packageIds, String pass) throws BadParameterException;

    @APIParameterNames({ "linkIds", "packageIds" })
    boolean forceDownload(long[] linkIds, long[] packageIds) throws BadParameterException;

    @APIParameterNames({ "packageIds", "linkIds", "filterByReason" })
    boolean unskip(long[] packageIds, long[] linkIds, SkipReasonStorable.Reason filterByReason) throws BadParameterException;
}

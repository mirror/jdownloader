package org.jdownloader.api.linkcollector.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.APIParameterNames;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.exceptions.BadParameterException;
import org.jdownloader.myjdownloader.client.bindings.CleanupActionOptions.Action;
import org.jdownloader.myjdownloader.client.bindings.CleanupActionOptions.Mode;
import org.jdownloader.myjdownloader.client.bindings.CleanupActionOptions.SelectionType;
import org.jdownloader.myjdownloader.client.bindings.PriorityStorable;
import org.jdownloader.myjdownloader.client.bindings.UrlDisplayTypeStorable;

@ApiNamespace("linkgrabberv2")
public interface LinkCollectorAPIV2 extends RemoteAPIInterface {
    @APIParameterNames({ "queryParams" })
    ArrayList<CrawledPackageAPIStorableV2> queryPackages(CrawledPackageQueryStorable queryParams) throws BadParameterException;

    @APIParameterNames({ "queryParams" })
    ArrayList<CrawledLinkAPIStorableV2> queryLinks(CrawledLinkQueryStorable queryParams) throws BadParameterException;

    @APIParameterNames({ "linkIds", "packageIds" })
    void moveToDownloadlist(long[] linkIds, long[] packageIds) throws BadParameterException;

    @APIParameterNames({ "linkIds", "packageIds" })
    void removeLinks(long[] linkIds, long[] packageIds) throws BadParameterException;

    @APIParameterNames({ "enabled", "linkIds", "packageIds" })
    void setEnabled(boolean enabled, long[] linkIds, long[] packageIds) throws BadParameterException;

    @APIParameterNames({ "priority", "linkIds", "packageIds" })
    void setPriority(PriorityStorable priority, long[] linkIds, long[] packageIds) throws BadParameterException;

    @APIParameterNames({ "linkId", "newName" })
    void renameLink(long linkId, String newName) throws BadParameterException;

    @APIParameterNames({ "packageId", "newName" })
    void renamePackage(long packageId, String newName) throws BadParameterException;

    @APIParameterNames({ "structureWatermark" })
    long getChildrenChanged(long structureWatermark);

    List<String> getDownloadFolderHistorySelectionBase();

    int getPackageCount();

    @APIParameterNames({ "packageIds", "afterDestPackageId" })
    void movePackages(long[] packageIds, long afterDestPackageId) throws BadParameterException;

    @APIParameterNames({ "linkIds", "afterLinkID", "destPackageID" })
    void moveLinks(long[] linkIds, long afterLinkID, long destPackageID) throws BadParameterException;

    @APIParameterNames({ "query" })
    LinkCollectingJobAPIStorable addLinks(AddLinksQueryStorable query);

    @APIParameterNames({ "type", "content" })
    LinkCollectingJobAPIStorable addContainer(String type, String content);

    @APIParameterNames({ "linkid" })
    List<LinkVariantStorableV2> getVariants(long linkid) throws BadParameterException;

    @APIParameterNames({ "linkid", "variantID" })
    void setVariant(long linkid, String variantID) throws BadParameterException;

    @APIParameterNames({ "linkid", "destinationAfterLinkID", "destinationPackageID", "variantID" })
    void addVariantCopy(long linkid, long destinationAfterLinkID, long destinationPackageID, String variantID) throws BadParameterException;

    @APIParameterNames({ "linkIds", "packageIds" })
    void startOnlineStatusCheck(long[] linkIds, long[] packageIds) throws BadParameterException;

    @APIParameterNames({"linkIds","packageIds","urlDisplayTypes"})

    Map<String, List<Long>> getDownloadUrls(final long[] linkIds, final long[] packageIds, UrlDisplayTypeStorable[] urlDisplayTypes) throws BadParameterException;

    @APIParameterNames({"linkIds","pkgIds","newPkgName","downloadPath"})

    void movetoNewPackage(long[] linkIds, long[] pkgIds, String newPkgName, String downloadPath) throws BadParameterException;

    @APIParameterNames({ "linkIds", "pkgIds" })
    void splitPackageByHoster(long[] linkIds, long[] pkgIds);

    @APIParameterNames({ "linkIds", "packageIds", "action", "mode", "selectionType" })
    void cleanup(long[] linkIds, long[] packageIds, Action action, Mode mode, SelectionType selectionType) throws BadParameterException;

    @APIParameterNames({ "directory", "packageIds" })
    void setDownloadDirectory(String directory, long[] packageIds) throws BadParameterException;

    @APIParameterNames({ "linkIds", "packageIds", "pass" })
    boolean setDownloadPassword(long[] linkIds, long[] packageIds, String pass) throws BadParameterException;

    boolean clearList();

    boolean abort();

    boolean isCollecting();

    @APIParameterNames({ "jobId" })
    boolean abort(long jobId);

    @APIParameterNames({ "query" })
    List<JobLinkCrawlerAPIStorable> queryLinkCrawlerJobs(final LinkCrawlerJobsQueryStorable query);
}

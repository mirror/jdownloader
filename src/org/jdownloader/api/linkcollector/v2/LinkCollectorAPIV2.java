package org.jdownloader.api.linkcollector.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.exceptions.BadParameterException;
import org.jdownloader.myjdownloader.client.bindings.CleanupActionOptions.Action;
import org.jdownloader.myjdownloader.client.bindings.CleanupActionOptions.Mode;
import org.jdownloader.myjdownloader.client.bindings.CleanupActionOptions.SelectionType;
import org.jdownloader.myjdownloader.client.bindings.PriorityStorable;
import org.jdownloader.myjdownloader.client.bindings.UrlDisplayTypeStorable;

@ApiNamespace("linkgrabberv2")
public interface LinkCollectorAPIV2 extends RemoteAPIInterface {

    ArrayList<CrawledPackageAPIStorableV2> queryPackages(CrawledPackageQueryStorable queryParams) throws BadParameterException;

    ArrayList<CrawledLinkAPIStorableV2> queryLinks(CrawledLinkQueryStorable queryParams) throws BadParameterException;

    void moveToDownloadlist(long[] linkIds, long[] packageIds) throws BadParameterException;

    void removeLinks(long[] linkIds, long[] packageIds) throws BadParameterException;

    void setEnabled(boolean enabled, long[] linkIds, long[] packageIds) throws BadParameterException;

    void setPriority(PriorityStorable priority, long[] linkIds, long[] packageIds) throws BadParameterException;

    void renameLink(long linkId, String newName) throws BadParameterException;

    void renamePackage(long packageId, String newName) throws BadParameterException;

    long getChildrenChanged(long structureWatermark);

    List<String> getDownloadFolderHistorySelectionBase();

    int getPackageCount();

    void movePackages(long[] packageIds, long afterDestPackageId) throws BadParameterException;

    void moveLinks(long[] linkIds, long afterLinkID, long destPackageID) throws BadParameterException;

    void addLinks(AddLinksQueryStorable query);

    void addContainer(String type, String content);

    List<LinkVariantStorableV2> getVariants(long linkid) throws BadParameterException;

    void setVariant(long linkid, String variantID) throws BadParameterException;

    void addVariantCopy(long linkid, long destinationAfterLinkID, long destinationPackageID, String variantID) throws BadParameterException;

    void startOnlineStatusCheck(long[] linkIds, long[] packageIds) throws BadParameterException;

    Map<String, List<Long>> getDownloadUrls(final long[] linkIds, final long[] packageIds, UrlDisplayTypeStorable[] urlDisplayTypes) throws BadParameterException;

    void movetoNewPackage(long[] linkIds, long[] pkgIds, String newPkgName, String downloadPath) throws BadParameterException;

    void splitPackageByHoster(long[] linkIds, long[] pkgIds);

    void cleanup(long[] linkIds, long[] packageIds, Action action, Mode mode, SelectionType selectionType) throws BadParameterException;

    void setDownloadDirectory(String directory, long[] packageIds) throws BadParameterException;

}

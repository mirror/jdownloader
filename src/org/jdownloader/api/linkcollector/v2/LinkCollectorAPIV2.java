package org.jdownloader.api.linkcollector.v2;

import java.util.ArrayList;
import java.util.List;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.exceptions.BadParameterException;

@ApiNamespace("linkgrabberv2")
public interface LinkCollectorAPIV2 extends RemoteAPIInterface {

    ArrayList<CrawledPackageAPIStorableV2> queryPackages(CrawledPackageQueryStorable queryParams) throws BadParameterException;

    ArrayList<CrawledLinkAPIStorableV2> queryLinks(CrawledLinkQueryStorable queryParams) throws BadParameterException;

    void moveToDownloadlist(long[] linkIds, long[] packageIds) throws BadParameterException;

    void removeLinks(long[] linkIds, long[] packageIds) throws BadParameterException;

    void setEnabled(boolean enabled, long[] linkIds, long[] packageIds) throws BadParameterException;

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
}

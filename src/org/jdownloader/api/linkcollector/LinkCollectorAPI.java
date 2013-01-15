package org.jdownloader.api.linkcollector;

import java.util.List;

import org.appwork.remoteapi.APIQuery;
import org.appwork.remoteapi.ApiNamespace;
import org.appwork.remoteapi.RemoteAPIInterface;

@ApiNamespace("linkcollector")
public interface LinkCollectorAPI extends RemoteAPIInterface {
    List<CrawledPackageAPIStorable> queryPackages(APIQuery queryParams);

    List<CrawledLinkAPIStorable> queryLinks(APIQuery queryParams);

    Boolean addLinks(String link);

    Long getChildrenChanged(Long structureWatermark);

    Boolean startDownloads(List<String> linkIds);
}

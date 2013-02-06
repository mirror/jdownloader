package org.jdownloader.extensions.oliverremoteapi.api.linkcollector;

import java.util.List;

import org.appwork.remoteapi.ApiNamespace;
import org.appwork.remoteapi.RemoteAPIInterface;

@ApiNamespace("mobile/linkCollector")
public interface LinkCollectorMobileAPI extends RemoteAPIInterface {

    public List<CrawledPackageAPIStorable> list();

    public boolean AddCrawledPackageToDownloads(long crawledPackageID);

    public boolean AddCrawledLinkToDownloads(long crawledLinkID);

    public boolean CrawlLink(String URL);

    public boolean removeCrawledLink(String ID);

    public boolean removeCrawledPackage(String ID);

    public CrawledPackageAPIStorable getCrawledPackage(long crawledPackageID);

    public CrawledLinkAPIStorable getCrawledLink(long crawledLinkID);

    public String getPackageIDFromLinkID(long ID);
}

package org.jdownloader.extensions.jdanywhere.api.linkcollector;

import java.util.List;

import org.appwork.remoteapi.ApiNamespace;
import org.appwork.remoteapi.RemoteAPIInterface;

@ApiNamespace("mobile/linkCollector")
public interface LinkCollectorMobileAPI extends RemoteAPIInterface {

    public List<CrawledPackageAPIStorable> list(final String username, final String password);

    public boolean AddCrawledPackageToDownloads(long crawledPackageID, final String username, final String password);

    public boolean AddCrawledLinkToDownloads(long crawledLinkID, final String username, final String password);

    public boolean CrawlLink(String URL, final String username, final String password);

    public boolean removeCrawledLink(String ID, final String username, final String password);

    public boolean removeCrawledPackage(String ID, final String username, final String password);

    public CrawledPackageAPIStorable getCrawledPackage(long crawledPackageID, final String username, final String password);

    public CrawledLinkAPIStorable getCrawledLink(long crawledLinkID, final String username, final String password);

    public String getPackageIDFromLinkID(long ID, final String username, final String password);
}

package org.jdownloader.extensions.jdanywhere.api.interfaces;

import java.util.List;

import org.appwork.remoteapi.ApiNamespace;
import org.appwork.remoteapi.ApiSessionRequired;
import org.appwork.remoteapi.RemoteAPIInterface;
import org.jdownloader.extensions.jdanywhere.api.storable.CrawledLinkStoreable;
import org.jdownloader.extensions.jdanywhere.api.storable.CrawledPackageStorable;

@ApiNamespace("jdanywhere/linkCrawler")
@ApiSessionRequired
public interface ILinkCrawlerApi extends RemoteAPIInterface {

    public List<CrawledPackageStorable> list();

    public boolean AddCrawledPackageToDownloads(long crawledPackageID);

    public boolean AddCrawledLinkToDownloads(long crawledLinkID);

    public boolean CrawlLink(String URL);

    public boolean removeCrawledLink(String ID);

    public boolean removeCrawledPackage(String ID);

    public CrawledPackageStorable getCrawledPackage(long crawledPackageID);

    public CrawledLinkStoreable getCrawledLink(long crawledLinkID);

    public String getPackageIDFromLinkID(long ID);
}

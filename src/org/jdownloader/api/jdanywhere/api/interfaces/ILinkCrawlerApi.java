package org.jdownloader.api.jdanywhere.api.interfaces;

import java.util.List;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.annotations.ApiSessionRequired;
import org.jdownloader.api.jdanywhere.api.storable.CrawledLinkStoreable;
import org.jdownloader.api.jdanywhere.api.storable.CrawledPackageStorable;

@ApiNamespace("jdanywhere/linkCrawler")
@ApiSessionRequired
public interface ILinkCrawlerApi extends RemoteAPIInterface {

    public List<CrawledPackageStorable> list();

    public boolean AddCrawledPackageToDownloads(long crawledPackageID);

    public boolean CrawlLink(String URL);

    public boolean removeCrawledPackage(String ID);

    public CrawledPackageStorable getCrawledPackage(long crawledPackageID);

    public CrawledLinkStoreable getCrawledLink(long crawledLinkID);

    public String getPackageIDFromLinkID(long ID);

    public boolean addCrawledLinkToDownloads(List<Long> linkIds);

    public boolean removeCrawledLink(List<Long> linkIds);

    public boolean enableCrawledLink(final List<Long> linkIds, boolean enabled);

    public boolean setCrawledLinkPriority(final List<Long> linkIds, int priority);

    public boolean setCrawledPackagePriority(long ID, int priority);

    public boolean setCrawledLinkEnabled(final List<Long> linkIds, boolean enabled);

    public boolean setCrawledPackageEnabled(long ID, boolean enabled);

    public List<CrawledLinkStoreable> listLinks(long ID);

    public boolean addDLC(String dlcContent);
}

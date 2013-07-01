package org.jdownloader.controlling;

import java.util.HashSet;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.plugins.DownloadLink;

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.DomainInfo;
import org.jdownloader.gui.views.SelectionInfo;

public class AggregatedCrawlerNumbers {

    private long totalBytes;

    public String getTotalBytesString() {
        return SizeFormatter.formatBytes(totalBytes);
    }

    public int getLinkCount() {
        return linkCount;
    }

    public int getPackageCount() {
        return packageCount;
    }

    private int  linkCount;
    private int  packageCount;

    private long disabledTotalBytes;
    private long statusOnline;
    private long statusOffline;

    public long getStatusOnline() {
        return statusOnline;
    }

    public long getStatusOffline() {
        return statusOffline;
    }

    public long getStatusUnknown() {
        return statusUnknown;
    }

    public HashSet<DomainInfo> getHoster() {
        return hoster;
    }

    private long                statusUnknown;
    private HashSet<DomainInfo> hoster;

    public long getDisabledTotalBytes() {
        return disabledTotalBytes;
    }

    public AggregatedCrawlerNumbers(SelectionInfo<CrawledPackage, CrawledLink> selection) {

        totalBytes = 0l;
        disabledTotalBytes = 0l;
        statusOnline = 0l;
        statusOffline = 0l;
        statusUnknown = 0l;
        hoster = new HashSet<DomainInfo>();
        packageCount = selection.getAllPackages().size();
        linkCount = selection.getChildren().size();
        for (CrawledLink cl : selection.getChildren()) {
            if (cl == null) continue;
            if (cl.isEnabled()) {
                totalBytes += cl.getSize();
            } else {
                disabledTotalBytes += cl.getSize();

            }
            DownloadLink dl = cl.getDownloadLink();
            hoster.add(dl.getDomainInfo());
            switch (dl.getAvailableStatus()) {
            case FALSE:
                statusOffline++;
                break;
            case TRUE:
                statusOnline++;
                break;
            default:
                statusUnknown++;
                break;

            }

        }

    }

    public long getTotalBytes() {
        return totalBytes;
    }

}

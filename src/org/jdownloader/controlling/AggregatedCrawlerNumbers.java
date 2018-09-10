package org.jdownloader.controlling;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.DomainInfo;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.SelectionInfo.PackageView;
import org.jdownloader.myjdownloader.client.json.AvailableLinkState;
import org.jdownloader.settings.GeneralSettings;

public class AggregatedCrawlerNumbers {
    protected static final boolean FORCED_MIRROR_CASE_INSENSITIVE = CrossSystem.isWindows() || JsonConfig.create(GeneralSettings.class).isForceMirrorDetectionCaseInsensitive();
    private final long             totalBytes;

public final String getTotalBytesString(boolean includeDisabled) {
    if (includeDisabled) {
        return format(totalBytes + disabledTotalBytes);
    }
    return format(totalBytes);
}

private final String format(long totalBytes2) {
    if (totalBytes2 < 0) {
        return _GUI.T.lit_unknown();
    }
    return SizeFormatter.formatBytes(totalBytes2);
}

public final int getLinkCount() {
    return linkCount;
}

public final int getPackageCount() {
    return packageCount;
}

private final int  linkCount;
private final int  packageCount;
    private final long disabledTotalBytes;
private final long statusOnline;
private final long statusOffline;

public final long getStatusOnline() {
    return statusOnline;
}

public final long getStatusOffline() {
    return statusOffline;
}

public final long getStatusUnknown() {
    return statusUnknown;
}

public final HashSet<DomainInfo> getHoster() {
    return hoster;
}

private final long                statusUnknown;
private final HashSet<DomainInfo> hoster = new HashSet<DomainInfo>();

public final long getDisabledTotalBytes() {
    return disabledTotalBytes;
}

private final static class AggregatedCrawledLink {
    private long               bytes   = -1;
    private boolean            enabled = false;
    private AvailableLinkState status  = AvailableLinkState.UNKNOWN;
}

public AggregatedCrawlerNumbers(final SelectionInfo<CrawledPackage, CrawledLink> selection) {
    long totalBytes = 0l;
    long disabledTotalBytes = 0l;
    long statusOnline = 0;
    long statusOffline = 0;
    long statusUnknown = 0;
    final List<PackageView<CrawledPackage, CrawledLink>> packageViews = selection.getPackageViews();
    packageCount = packageViews.size();
    int linkCount = 0;
    for (final PackageView<CrawledPackage, CrawledLink> packageView : packageViews) {
        final HashMap<String, AggregatedCrawledLink> aggregatedCrawledLinks = new HashMap<String, AggregatedCrawledLink>();
        for (final CrawledLink cl : packageView.getChildren()) {
            if (cl == null) {
                continue;
            } else {
                final String name;
                if (FORCED_MIRROR_CASE_INSENSITIVE) {
                    name = cl.getName().toLowerCase(Locale.ENGLISH);
                } else {
                    name = cl.getName();
                }
                hoster.add(cl.getDomainInfo());
                AggregatedCrawledLink acl = aggregatedCrawledLinks.get(name);
                final long itemSize = Math.max(0, cl.getSize());
                final AvailableLinkState state = cl.getLinkState();
                if (acl == null) {
                    acl = new AggregatedCrawledLink();
                    acl.enabled = cl.isEnabled();
                    acl.bytes = itemSize;
                    acl.status = state;
                    aggregatedCrawledLinks.put(name, acl);
                } else {
                    if (itemSize > acl.bytes) {
                        acl.bytes = itemSize;
                    }
                    if (acl.enabled == false) {
                        acl.enabled = cl.isEnabled();
                    }
                    switch (acl.status) {
                    case ONLINE:
                        break;
                    default:
                        acl.status = state;
                        break;
                    }
                }
            }
        }
        linkCount += aggregatedCrawledLinks.size();
        for (final AggregatedCrawledLink acl : aggregatedCrawledLinks.values()) {
            if (acl.enabled) {
                totalBytes += acl.bytes;
            } else {
                disabledTotalBytes += acl.bytes;
            }
            switch (acl.status) {
            case ONLINE:
                statusOnline++;
                break;
            case OFFLINE:
                statusOffline++;
                break;
            default:
                statusUnknown++;
                break;
            }
        }
    }
    this.statusOffline = statusOffline;
    this.statusOnline = statusOnline;
    this.statusUnknown = statusUnknown;
    this.disabledTotalBytes = disabledTotalBytes;
    this.totalBytes = totalBytes;
    this.linkCount = linkCount;
}

public final long getTotalBytes() {
    return totalBytes;
}
}

package jd.controlling.linkcrawler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.ChildrenView;
import jd.plugins.DownloadLink;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.DomainInfo;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModelData.PackageControllerTableModelDataPackage;
import org.jdownloader.gui.views.downloads.columns.AvailabilityColumn;
import org.jdownloader.myjdownloader.client.json.AvailableLinkState;
import org.jdownloader.settings.GeneralSettings;

public class CrawledPackageView extends ChildrenView<CrawledLink> {
    private static class LinkInfo {
        private long bytesTotal = -1;
    }

    protected volatile long                fileSize                       = -1;
    private volatile DomainInfo[]          domainInfos                    = new DomainInfo[0];
    protected volatile boolean             enabled                        = false;
    private volatile int                   offline                        = 0;
    private volatile int                   online                         = 0;
    private volatile int                   items                          = 0;
    private final AtomicLong               updatesRequired                = new AtomicLong(0);
    private volatile long                  updatesDone                    = -1;
    private volatile String                commonSourceUrl;
    private volatile String                availabilityColumnString       = null;
    private volatile ChildrenAvailablility availability                   = ChildrenAvailablility.UNKNOWN;
    private final CrawledPackage           pkg;
    protected static final boolean         FORCED_MIRROR_CASE_INSENSITIVE = CrossSystem.isWindows() || JsonConfig.create(GeneralSettings.class).isForceMirrorDetectionCaseInsensitive();

    public CrawledPackageView() {
        this(null);
    }

    public CrawledPackageView(CrawledPackage pkg) {
        this.pkg = pkg;
    }

    @Override
    public CrawledPackageView aggregate() {
        if (pkg != null) {
            final boolean readL = pkg.getModifyLock().readLock();
            try {
                return setItems(pkg.getChildren());
            } finally {
                pkg.getModifyLock().readUnlock(readL);
            }
        } else {
            synchronized (this) {
                final Temp tmp = new Temp();
                /* this is called for repaint, so only update values that could have changed for existing items */
                final PackageControllerTableModelDataPackage tableModelDataPackage = getTableModelDataPackage();
                int size = 0;
                if (tableModelDataPackage != null) {
                    for (final AbstractNode item : tableModelDataPackage.getVisibleChildren()) {
                        size++;
                        addtoTmp(tmp, (CrawledLink) item);
                    }
                }
                writeTmpToFields(tmp);
                updateAvailability(size, tmp.newOffline, tmp.newOnline);
                availabilityColumnString = _GUI.T.AvailabilityColumn_getStringValue_object_(tmp.newOnline, size);
            }
        }
        return this;
    }

    private class Temp {
        final HashMap<String, LinkInfo> linkInfos         = new HashMap<String, LinkInfo>();
        final HashSet<DomainInfo>       domains           = new HashSet<DomainInfo>();
        int                             newOnline         = 0;
        boolean                         newEnabled        = false;
        int                             newOffline        = 0;
        String                          sameSource        = null;
        boolean                         sameSourceFullUrl = true;
        long                            lupdatesRequired  = updatesRequired.get();
    }

    @Override
    public CrawledPackageView setItems(List<CrawledLink> items) {
        synchronized (this) {
            final Temp tmp = new Temp();
            /* this is called for tablechanged, so update everything for given items */
            if (items != null) {
                for (CrawledLink item : items) {
                    // domain
                    tmp.domains.add(item.getDomainInfo());
                    addtoTmp(tmp, item);
                }
            }
            writeTmpToFields(tmp);
            final ArrayList<DomainInfo> lst = new ArrayList<DomainInfo>(tmp.domains);
            Collections.sort(lst, new Comparator<DomainInfo>() {
                @Override
                public int compare(DomainInfo o1, DomainInfo o2) {
                    return o1.getTld().compareTo(o2.getTld());
                }
            });
            domainInfos = lst.toArray(new DomainInfo[] {});
            if (items == null) {
                this.items = 0;
            } else {
                this.items = items.size();
            }
            updateAvailability(this.items, tmp.newOffline, tmp.newOnline);
            availabilityColumnString = _GUI.T.AvailabilityColumn_getStringValue_object_(tmp.newOnline, this.items);
        }
        return this;
    }

    protected void writeTmpToFields(Temp tmp) {
        this.commonSourceUrl = tmp.sameSource;
        if (!tmp.sameSourceFullUrl) {
            commonSourceUrl += "[...]";
        }
        long size = -1;
        for (final LinkInfo linkInfo : tmp.linkInfos.values()) {
            if (linkInfo.bytesTotal != -1) {
                if (size == -1) {
                    size = 0;
                }
                size += linkInfo.bytesTotal;
            }
        }
        fileSize = size;
        enabled = tmp.newEnabled;
        offline = tmp.newOffline;
        online = tmp.newOnline;
        updatesDone = tmp.lupdatesRequired;
    }

    protected void addtoTmp(Temp tmp, CrawledLink link) {
        final DownloadLink dlLink = link.getDownloadLink();
        final String sourceUrl = dlLink.getView().getDisplayUrl();
        if (sourceUrl != null) {
            tmp.sameSource = StringUtils.getCommonalities(tmp.sameSource, sourceUrl);
            tmp.sameSourceFullUrl = tmp.sameSourceFullUrl && tmp.sameSource.equals(sourceUrl);
        }
        // enabled
        if (link.isEnabled()) {
            tmp.newEnabled = true;
        }
        if (link.getLinkState() == AvailableLinkState.OFFLINE) {
            // offline
            tmp.newOffline++;
        } else if (link.getLinkState() == AvailableLinkState.ONLINE) {
            // online
            tmp.newOnline++;
        }
        final String name;
        if (FORCED_MIRROR_CASE_INSENSITIVE) {
            name = link.getName().toLowerCase(Locale.ENGLISH);
        } else {
            name = link.getName();
        }
        LinkInfo existing = tmp.linkInfos.get(name);
        final long linkSize = link.getSize();
        if (existing == null) {
            existing = new LinkInfo();
            existing.bytesTotal = linkSize;
            tmp.linkInfos.put(name, existing);
        } else if (linkSize > existing.bytesTotal) {
            existing.bytesTotal = linkSize;
        }
    }

    private final void updateAvailability(int size, int offline, int online) {
        if (online == size) {
            availability = ChildrenAvailablility.ONLINE;
            return;
        }
        if (offline == size) {
            availability = ChildrenAvailablility.OFFLINE;
            return;
        }
        if ((offline == 0 && online == 0) || (online == 0 && offline > 0)) {
            availability = ChildrenAvailablility.UNKNOWN;
            return;
        }
        availability = ChildrenAvailablility.MIXED;
        return;
    }

    public String getCommonSourceUrl() {
        return commonSourceUrl;
    }

    public DomainInfo[] getDomainInfos() {
        return domainInfos;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getOfflineCount() {
        return offline;
    }

    public int getOnlineCount() {
        return online;
    }

    public long getFileSize() {
        return fileSize;
    }

    @Override
    public void requestUpdate() {
        updatesRequired.incrementAndGet();
    }

    @Override
    public boolean updateRequired() {
        return updatesRequired.get() != updatesDone;
    }

    @Override
    public ChildrenAvailablility getAvailability() {
        return availability;
    }

    @Override
    public String getMessage(Object requestor) {
        if (requestor instanceof AvailabilityColumn) {
            return availabilityColumnString;
        }
        return null;
    }

    @Override
    public int size() {
        return items;
    }
}

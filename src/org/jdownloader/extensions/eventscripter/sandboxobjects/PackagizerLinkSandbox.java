package org.jdownloader.extensions.eventscripter.sandboxobjects;

import java.io.File;
import java.util.Map;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.PackageInfo;

import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.controlling.Priority;
import org.jdownloader.extensions.eventscripter.ScriptAPI;

public class PackagizerLinkSandbox {
    private final CrawledLink link;

    public PackagizerLinkSandbox(final CrawledLink link) {
        this.link = link;
    }

    @Override
    public int hashCode() {
        if (link != null) {
            return link.hashCode();
        } else {
            return super.hashCode();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PackagizerLinkSandbox) {
            return ((PackagizerLinkSandbox) obj).link == link;
        } else {
            return super.equals(obj);
        }
    }

    public PackagizerLinkSandbox() {
        this(null);
    }

    public long getBytesTotal() {
        return link != null ? link.getSize() : -1;
    }

    public boolean isEnabled() {
        return link != null && link.isEnabled();
    }

    public void setEnabled(boolean e) {
        if (link != null) {
            link.setEnabled(e);
        }
    }

    public String getHost() {
        return link != null ? link.getHost() : null;
    }

    public String getName() {
        return link != null ? link.getName() : null;
    }

    public void setName(String name) {
        if (link != null) {
            link.setName(name);
        }
    }

    @ScriptAPI(description = "Get the Link Priority (HIGHEST|HIGHER|HIGH|DEFAULT|LOWER)")
    public String getPriority() {
        return link != null ? link.getPriority().name() : null;
    }

    public int getChunks() {
        return link != null ? link.getChunks() : -1;
    }

    public void setChunks(int chunks) {
        if (link != null) {
            link.setChunks(chunks);
        }
    }

    public String getDownloadFolder() {
        if (link != null) {
            final PackageInfo dpi = link.getDesiredPackageInfo();
            return dpi == null ? null : dpi.getDestinationFolder();
        } else {
            return null;
        }
    }

    public void setDownloadFolder(String destinationFolder) {
        if (link != null) {
            PackageInfo packageInfo = link.getDesiredPackageInfo();
            if (packageInfo == null) {
                packageInfo = new PackageInfo();
            }
            if (StringUtils.isNotEmpty(destinationFolder)) {
                destinationFolder = CrossSystem.fixPathSeparators(destinationFolder + File.separator);
            }
            packageInfo.setDestinationFolder(destinationFolder);
            link.setDesiredPackageInfo(packageInfo);
        }
    }

    public String getURL() {
        return link != null ? link.getURL() : null;
    }

    public String getLinkState() {
        return link != null ? link.getLinkState().name() : null;
    }

    public String[] getSourceUrls() {
        return link != null ? link.getSourceUrls() : null;
    }

    public String getPackageName() {
        if (link != null) {
            final PackageInfo dpi = link.getDesiredPackageInfo();
            return dpi == null ? null : dpi.getName();
        } else {
            return null;
        }
    }

    public void setPackageName(String name) {
        if (link != null) {
            PackageInfo packageInfo = link.getDesiredPackageInfo();
            if (packageInfo == null) {
                packageInfo = new PackageInfo();
            }
            packageInfo.setName(name);
            link.setDesiredPackageInfo(packageInfo);
        }
    }

    private CrawledLinkSandbox crawledLinkSandbox = null;

    public CrawledLinkSandbox getCrawledLink() {
        if (crawledLinkSandbox != null) {
            return crawledLinkSandbox;
        }
        if (link != null) {
            crawledLinkSandbox = new CrawledLinkSandbox(link);
            return crawledLinkSandbox;
        } else {
            return null;
        }
    }

    public Object getProperty(String key) {
        final CrawledLinkSandbox crawledLinkSandbox = getCrawledLink();
        if (crawledLinkSandbox != null) {
            return crawledLinkSandbox.getProperty(key);
        } else {
            return null;
        }
    }

    public Map<String, Object> getProperties() {
        final CrawledLinkSandbox crawledLinkSandbox = getCrawledLink();
        if (crawledLinkSandbox != null) {
            return crawledLinkSandbox.getProperties();
        } else {
            return null;
        }
    }

    public void setProperty(String key, Object value) {
        final CrawledLinkSandbox crawledLinkSandbox = getCrawledLink();
        if (crawledLinkSandbox != null) {
            crawledLinkSandbox.setProperty(key, value);
        }
    }

    @ScriptAPI(description = "Sets the Link Priority", parameters = { "HIGHEST|HIGHER|HIGH|DEFAULT|LOWER" })
    public void setPriority(String priority) {
        if (link != null) {
            link.setPriority(Priority.valueOf(priority));
        }
    }

    public String getComment() {
        return link != null ? link.getComment() : null;
    }

    public void setComment(String comment) {
        if (link != null) {
            link.setComment(comment);
        }
    }

    @ScriptAPI(description = "If true, the link will automove to the downloadlist")
    public boolean isAutoConfirmEnabled() {
        return link != null && link.isAutoConfirmEnabled();
    }

    @ScriptAPI(description = "If true, the link will automove to the downloadlist")
    public void setAutoConfirmEnabled(boolean b) {
        if (link != null) {
            link.setAutoConfirmEnabled(b);
        }
    }

    @ScriptAPI(description = "If true, the link will autostart download after beeing confirmed")
    public boolean isAutoStartEnabled() {
        return link != null && link.isAutoStartEnabled();
    }

    @ScriptAPI(description = "If true, the link will autostart download after beeing confirmed")
    public void setAutoStartEnabled(boolean b) {
        if (link != null) {
            link.setAutoStartEnabled(b);
        }
    }
}

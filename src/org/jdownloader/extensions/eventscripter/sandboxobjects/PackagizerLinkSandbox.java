package org.jdownloader.extensions.eventscripter.sandboxobjects;

import java.io.File;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.PackageInfo;

import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.controlling.Priority;
import org.jdownloader.extensions.eventscripter.ScriptAPI;

public class PackagizerLinkSandbox {
    private final CrawledLink link;

    public PackagizerLinkSandbox(CrawledLink link) {
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
        if (link != null) {
            return link.getSize();
        }
        return -1;
    }

    public boolean isEnabled() {
        if (link != null) {
            return link.isEnabled();
        }
        return false;
    }

    public void setEnabled(boolean e) {
        if (link != null) {
            link.setEnabled(e);
        }
    }

    public String getHost() {
        if (link != null) {
            return link.getHost();
        }
        return null;
    }

    public String getName() {
        if (link != null) {
            return link.getName();
        }
        return null;
    }

    public void setName(String name) {
        if (link != null) {
            link.setName(name);
        }
    }

    @ScriptAPI(description = "Get the Link Priority (HIGHEST|HIGHER|HIGH|DEFAULT|LOWER)")
    public String getPriority() {
        if (link != null) {
            return link.getPriority().name();
        }
        return null;
    }

    public int getChunks() {
        if (link != null) {
            return link.getChunks();
        }
        return -1;
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
        }
        return null;
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
        if (link != null) {
            return link.getURL();
        }
        return null;
    }

    public String getLinkState() {
        if (link != null) {
            return link.getLinkState().name();
        }
        return null;
    }

    public String[] getSourceUrls() {
        if (link != null) {
            return link.getSourceUrls();
        }
        return null;
    }

    public String getPackageName() {
        if (link != null) {
            final PackageInfo dpi = link.getDesiredPackageInfo();
            return dpi == null ? null : dpi.getName();
        }
        return null;
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

    @ScriptAPI(description = "Sets the Link Priority", parameters = { "HIGHEST|HIGHER|HIGH|DEFAULT|LOWER" })
    public void setPriority(String priority) {
        if (link != null) {
            link.setPriority(Priority.valueOf(priority));
        }
    }

    public String getComment() {
        if (link != null) {
            return link.getComment();
        }
        return null;
    }

    public void setComment(String comment) {
        if (link != null) {
            link.setComment(comment);
        }
    }

    @ScriptAPI(description = "If true, the link will automove to the downloadlist")
    public boolean isAutoConfirmEnabled() {
        if (link != null) {
            return link.isAutoConfirmEnabled();
        }
        return false;
    }

    @ScriptAPI(description = "If true, the link will automove to the downloadlist")
    public void setAutoConfirmEnabled(boolean b) {
        if (link != null) {
            link.setAutoConfirmEnabled(b);
        }
    }

    @ScriptAPI(description = "If true, the link will autostart download after beeing confirmed")
    public boolean isAutoStartEnabled() {
        if (link != null) {
            return link.isAutoStartEnabled();
        }
        return false;
    }

    @ScriptAPI(description = "If true, the link will autostart download after beeing confirmed")
    public void setAutoStartEnabled(boolean b) {
        if (link != null) {
            link.setAutoStartEnabled(b);
        }
    }
}

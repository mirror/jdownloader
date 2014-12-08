package org.jdownloader.extensions.eventscripter.sandboxobjects;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.PackageInfo;

import org.jdownloader.controlling.Priority;
import org.jdownloader.extensions.eventscripter.ScriptAPI;

public class PackagizerLinkSandbox {

    private CrawledLink link;

    public PackagizerLinkSandbox(CrawledLink link) {
        this.link = link;
    }

    public PackagizerLinkSandbox() {
        // {"variant":null,"host":null,"name":null,"comment":null,"availability":null,"variants":false,"priority":"DEFAULT","packageUUID":-1,"bytesTotal":-1,"uuid":-1,"url":null,"enabled":false}

    }

    public long getBytesTotal() {
        if (link == null) {
            return -1;
        }
        return link.getSize();
    }

    public boolean isEnabled() {
        if (link == null) {
            return false;
        }
        return link.isEnabled();
    }

    public String getHost() {
        if (link == null) {
            return null;
        }
        return link.getHost();
    }

    public String getName() {
        if (link == null) {
            return null;
        }
        return link.getName();
    }

    @ScriptAPI(description = "Get the Link Priority (HIGHEST|HIGHER|HIGH|DEFAULT|LOWER)")
    public String getPriority() {
        if (link == null) {
            return null;
        }
        return link.getPriority().name();
    }

    // public String
    // PackageInfo dpi = link.getDesiredPackageInfo();
    // if (dpi == null) {
    // dpi = new PackageInfo();
    // }
    // boolean dpiSet = false;
    // if (lgr.getRule().getChunks() >= 0) {
    // /* customize chunk numbers */
    // link.setChunks(lgr.getRule().getChunks());
    // }

    public int getChunks() {
        if (link == null) {
            return -1;
        }
        return link.getChunks();
    }

    public void setChunks(int chunks) {
        if (link == null) {
            return;
        }
        link.setChunks(chunks);
    }

    // if (!StringUtils.isEmpty(lgr.getRule().getDownloadDestination())) {
    // /* customize download destination folder */
    // String path = replaceVariables(lgr.getRule().getDownloadDestination(), link, lgr);
    // dpiSet = true;
    // dpi.setDestinationFolder(path);
    // }

    public String getDownloadFolder() {
        if (link == null) {
            return null;
        }
        PackageInfo dpi = link.getDesiredPackageInfo();
        return dpi == null ? null : dpi.getDestinationFolder();
    }

    public void setDownloadFolder(String destinationFolder) {
        if (link == null) {
            return;
        }
        getDesiredPackageInfo().setDestinationFolder(destinationFolder);
    }

    private PackageInfo getDesiredPackageInfo() {
        PackageInfo dpi = link.getDesiredPackageInfo();
        if (dpi == null) {
            dpi = new PackageInfo();
            link.setDesiredPackageInfo(dpi);
        }
        return dpi;
    }

    // if (lgr.getRule().getLinkEnabled() != null) {
    // link.setEnabled(lgr.getRule().getLinkEnabled());
    // }
    // if (!StringUtils.isEmpty(lgr.getRule().getPackageName())) {
    // /* customize package name */
    // String name = replaceVariables(lgr.getRule().getPackageName(), link, lgr);
    // dpiSet = true;
    // dpi.setName(name);
    // }
    public String getPackageName() {
        if (link == null) {
            return null;
        }
        PackageInfo dpi = link.getDesiredPackageInfo();
        return dpi == null ? null : dpi.getName();
    }

    public void setPackageName(String name) {
        if (link == null) {
            return;
        }
        getDesiredPackageInfo().setName(name);
    }

    // if (lgr.getRule().getPriority() != null) {
    // /* customize priority */
    // link.setPriority(lgr.getRule().getPriority());
    // }
    @ScriptAPI(description = "Sets the Link Priority", parameters = { "HIGHEST|HIGHER|HIGH|DEFAULT|LOWER" })
    public void setPriority(String priority) {
        if (link == null) {
            return;
        }
        link.setPriority(Priority.valueOf(priority));
    }

    // if (!StringUtils.isEmpty(lgr.getRule().getFilename())) {
    // /* customize filename */
    // link.setName(replaceVariables(lgr.getRule().getFilename(), link, lgr));
    // }
    // if (!StringUtils.isEmpty(lgr.getRule().getComment())) {
    // /* customize filename */
    // link.setComment(replaceVariables(lgr.getRule().getComment(), link, lgr));
    // }
    public String getComment() {
        if (link == null) {
            return null;
        }
        return link.getComment();
    }

    public void setComment(String comment) {
        if (link == null) {
            return;
        }
        link.setComment(comment);
    }

    // Boolean b = null;
    // if ((b = lgr.getRule().isAutoExtractionEnabled()) != null) {
    // /* customize auto extract */
    // link.getArchiveInfo().setAutoExtract(b ? BooleanStatus.TRUE : BooleanStatus.FALSE);
    //
    // }
    // if ((b = lgr.getRule().isAutoAddEnabled()) != null) {
    // /* customize auto add */
    //
    // link.setAutoConfirmEnabled(b);
    //
    @ScriptAPI(description = "If true, the link will automove to the downloadlist")
    public boolean isAutoConfirmEnabled() {
        if (link == null) {
            return false;
        }
        return link.isAutoConfirmEnabled();
    }

    @ScriptAPI(description = "If true, the link will automove to the downloadlist")
    public void setAutoConfirmEnabled(boolean b) {
        if (link == null) {
            return;
        }
        link.setAutoConfirmEnabled(b);
    }

    // }
    // if ((b = lgr.getRule().isAutoStartEnabled()) != null) {
    // /* customize auto start */
    //
    // link.setAutoStartEnabled(b);
    //
    // }
    // if ((b = lgr.getRule().isAutoForcedStartEnabled()) != null) {
    // /* customize auto start */
    //
    // link.setForcedAutoStartEnabled(b);
    //
    // }

    @ScriptAPI(description = "If true, the link will autostart download after beeing confirmed")
    public boolean isAutoStartEnabled() {
        if (link == null) {
            return false;
        }
        return link.isAutoStartEnabled();
    }

    @ScriptAPI(description = "If true, the link will autostart download after beeing confirmed")
    public void setAutoStartEnabled(boolean b) {
        if (link == null) {
            return;
        }
        link.setAutoStartEnabled(b);
    }
    // if (dpiSet && link.getDesiredPackageInfo() == null) {
    // /* set desiredpackageinfo if not set yet */
    // link.setDesiredPackageInfo(dpi);
    // dpi.setPackagizerRuleMatched(true);
    // }
}

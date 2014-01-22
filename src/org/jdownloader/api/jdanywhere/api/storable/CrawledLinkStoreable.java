package org.jdownloader.api.jdanywhere.api.storable;

import jd.controlling.linkcrawler.CrawledLink;

import org.appwork.storage.Storable;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.bindings.crawledlink.CrawledLinkFactory;

public class CrawledLinkStoreable implements Storable {

    public long getId() {
        if (link == null) return 0;
        return link.getDownloadLink().getUniqueID().getID();
    }

    public String getName() {
        if (link == null) return null;
        return link.getName();
    }

    public String getHost() {
        if (link == null) return null;
        return link.getHost();
    }

    public String getOnlinestatus() {
        if (link == null) return null;
        return "";
    }

    public long getSize() {
        if (link == null) return -1l;
        return link.getDownloadLink().getView().getBytesTotalEstimated();
    }

    public boolean isEnabled() {
        if (link == null) return true;
        return link.isEnabled();
    }

    public long getAdded() {
        if (link == null) return -1l;
        return link.getCreated();
    }

    public long getFinished() {
        if (link == null) return -1l;
        return link.getFinishedDate();
    }

    public int getPriority() {
        if (link == null) return 0;
        return link.getPriority().getId();
    }

    public int getChunks() {
        if (link == null) return 0;
        return link.getChunks();
    }

    public String getBrowserurl() {
        if (link == null) return null;
        return link.getURL();
    }

    public String getPassword() {

        if (link == null) return null;
        String returnValue = "";
        ExtractionExtension extractor = (ExtractionExtension) ExtensionController.getInstance().getExtension(ExtractionExtension.class)._getExtension();
        CrawledLinkFactory clf = new CrawledLinkFactory(((CrawledLink) link));
        Archive archive = extractor.getArchiveByFactory(clf);
        if (archive != null && archive.getSettings() != null && archive.getSettings().getPasswords() != null) {
            for (String pwd : archive.getSettings().getPasswords()) {
                if (returnValue != "") returnValue += "/";
                returnValue += pwd;
            }
        }
        return returnValue;// getParentNode().getDownloadPassword(); //
                           // getDownloadLink().getDownloadPassword();
    }

    public String getDirectory() {
        if (link == null) return null;
        return link.getParentNode().getDownloadFolder(); // getDownloadLink().getFileOutput();
        // return null;
    }

    public String getComment() {
        if (link == null) return null;
        return link.getDownloadLink().getComment();
    }

    // public LinkStatusJob getLinkStatus() {
    // if (link == null) return null;
    //
    // LinkStatus ls = link.getLinkStatus();
    // LinkStatusJob lsj = new LinkStatusJob();
    //
    // lsj.setActive(ls.isPluginActive());
    // lsj.setFinished(ls.isFinished());
    // lsj.setInProgress(ls.isPluginInProgress());
    // lsj.setLinkID(link.getUniqueID().toString());
    // lsj.setStatus(ls.getStatus());
    // lsj.setStatusText(ls.getMessage(false));
    // return lsj;
    // }

    private CrawledLink link;

    @SuppressWarnings("unused")
    private CrawledLinkStoreable() {
        this.link = null;
    }

    public CrawledLinkStoreable(CrawledLink link) {
        this.link = link;
    }
}

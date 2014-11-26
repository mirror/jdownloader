package jd.controlling.linkcollector;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.linkcrawler.CrawledLink;

import org.jdownloader.translate._JDT;

public enum VariousCrawledLinkFlags implements MatchesInterface<CrawledLink> {
    DOWNLOAD_LIST_DUPE(_JDT._.DOWNLOAD_LIST_DUPE()) {
        public boolean matches(CrawledLink link) {
            return DownloadController.getInstance().hasDownloadLinkByID(link.getLinkID());
        }

    };

    private String translation;

    private VariousCrawledLinkFlags(String translation) {
        this.translation = translation;
    }

    public String getTranslation() {
        return translation;
    }

}

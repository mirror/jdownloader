package jd.controlling.linkcrawler.modifier;

import java.util.List;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledLinkModifier;
import jd.plugins.DownloadLink;

import org.appwork.utils.StringUtils;

public class CommentModifier implements CrawledLinkModifier {
    protected final String comment;

    public String getComment() {
        return comment;
    }

    public CommentModifier(final String comment) {
        this.comment = comment;
    }

    @Override
    public List<CrawledLinkModifier> getSubCrawledLinkModifier(CrawledLink link) {
        return null;
    }

    @Override
    public boolean modifyCrawledLink(CrawledLink link) {
        final DownloadLink dlLink = link.getDownloadLink();
        if (dlLink != null && StringUtils.isEmpty(dlLink.getComment())) {
            dlLink.setComment(comment);
            return true;
        }
        return false;
    }
}

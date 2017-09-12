package jd.controlling.linkcrawler.modifier;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledLinkModifier;
import jd.plugins.DownloadLink;

import org.appwork.utils.StringUtils;

public class CommentModifier implements CrawledLinkModifier {
    protected final String comment;

    public CommentModifier(final String comment) {
        this.comment = comment;
    }

    @Override
    public void modifyCrawledLink(CrawledLink link) {
        final DownloadLink dlLink = link.getDownloadLink();
        if (dlLink != null && StringUtils.isEmpty(dlLink.getComment())) {
            dlLink.setComment(comment);
        }
    }
}

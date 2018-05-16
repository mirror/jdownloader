package jd.controlling.linkcrawler.modifier;

import java.util.List;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledLinkModifier;

public class FileNameModifier implements CrawledLinkModifier {
    protected final String name;

    public String getName() {
        return name;
    }

    public FileNameModifier(final String name) {
        this.name = name;
    }

    @Override
    public boolean modifyCrawledLink(CrawledLink link) {
        link.setName(getName());
        return true;
    }

    @Override
    public List<CrawledLinkModifier> getSubCrawledLinkModifier(CrawledLink link) {
        return null;
    }
}

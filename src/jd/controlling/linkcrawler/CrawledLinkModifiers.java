package jd.controlling.linkcrawler;

import java.util.ArrayList;
import java.util.List;

public class CrawledLinkModifiers implements CrawledLinkModifier {
    private final List<CrawledLinkModifier> modifiers = new ArrayList<CrawledLinkModifier>();

    public CrawledLinkModifiers(List<CrawledLinkModifier> modifiers) {
        if (modifiers != null) {
            this.modifiers.addAll(modifiers);
        }
    }

    @Override
    public boolean modifyCrawledLink(CrawledLink link) {
        boolean ret = false;
        for (final CrawledLinkModifier mod : modifiers) {
            if (mod.modifyCrawledLink(link)) {
                ret = true;
            }
        }
        return ret;
    }
}

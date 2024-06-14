package jd.controlling.linkcrawler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CrawledLinkModifiers implements CrawledLinkModifier {
    private final List<CrawledLinkModifier> modifiers;

    public CrawledLinkModifiers(List<CrawledLinkModifier> modifiers) {
        if (modifiers != null) {
            this.modifiers = Collections.unmodifiableList(modifiers);
        } else {
            this.modifiers = Collections.unmodifiableList(new ArrayList<CrawledLinkModifier>(0));
        }
    }

    public List<CrawledLinkModifier> getCrawledLinkModifier() {
        return modifiers;
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

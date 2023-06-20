package org.jdownloader.controlling.filter;

import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.controlling.linkcollector.LinkOrigin;
import jd.controlling.linkcrawler.CrawledLink;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.OriginFilter;

import org.appwork.storage.Storable;
import org.appwork.storage.StorableAllowPrivateAccessModifier;

public class CompiledOriginFilter extends OriginFilter implements Storable {
    @StorableAllowPrivateAccessModifier
    private CompiledOriginFilter() {
    }

    private OriginFilter        originFilter;
    private HashSet<LinkOrigin> originsSet;
    private boolean             checkMyJDCNL = false;
    private final Pattern       dummyCNL     = Pattern.compile("(?i)^https?://dummycnl\\.jdownloader\\.org/.+");

    public CompiledOriginFilter(OriginFilter originFilter) {
        this.originFilter = originFilter;
        originsSet = new HashSet<LinkOrigin>();
        for (LinkOrigin lo : originFilter.getOrigins()) {
            originsSet.add(lo);
        }
        checkMyJDCNL = originsSet.contains(LinkOrigin.CNL) && !originsSet.contains(LinkOrigin.MYJD);
    }

    public boolean matches(LinkOrigin source, CrawledLink link) {
        if (LinkOrigin.MYJD.equals(source) && checkMyJDCNL) {
            // only check origin and ignore dummyCNL results
            CrawledLink current = link.getOriginLink();
            final Matcher matcher = dummyCNL.matcher("");
            while (current != null) {
                final String url = current.getURL();
                if (url != null && matcher.reset(url).matches()) {
                    source = LinkOrigin.CNL;
                    break;
                }
                current = current.getSourceLink();
            }
        }
        switch (originFilter.getMatchType()) {
        case IS:
            return originsSet.contains(source);
        case ISNOT:
            return !originsSet.contains(source);
        default:
            return false;
        }
    }
}

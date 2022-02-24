package org.jdownloader.controlling.filter;

import java.util.HashSet;

import org.appwork.storage.Storable;

import jd.controlling.linkcollector.LinkOrigin;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.OriginFilter;

public class CompiledOriginFilter extends OriginFilter implements Storable {
    private CompiledOriginFilter() {
    }

    private OriginFilter        originFilter;
    private HashSet<LinkOrigin> originsSet;

    public CompiledOriginFilter(OriginFilter originFilter) {
        this.originFilter = originFilter;
        originsSet = new HashSet<LinkOrigin>();
        for (LinkOrigin lo : originFilter.getOrigins()) {
            originsSet.add(lo);
        }
    }

    public boolean matches(LinkOrigin source) {
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

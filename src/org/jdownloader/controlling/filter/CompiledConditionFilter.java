package org.jdownloader.controlling.filter;

import jd.controlling.linkcollector.VariousCrawledLinkFlags;
import jd.controlling.linkcrawler.CrawledLink;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.ConditionFilter;

import org.appwork.storage.Storable;
import org.appwork.storage.StorableAllowPrivateAccessModifier;

public class CompiledConditionFilter extends ConditionFilter implements Storable {
    @StorableAllowPrivateAccessModifier
    private CompiledConditionFilter(/* Storable */) {
    }

    private ConditionFilter filter;

    public CompiledConditionFilter(ConditionFilter originFilter) {
        this.filter = originFilter;
    }

    public boolean matches(CrawledLink link) {
        switch (filter.getMatchType()) {
        case IS_TRUE:
            for (VariousCrawledLinkFlags lo : filter.getConditions()) {
                if (!lo.matches(link)) {
                    return false;
                }
            }
            return true;
        case IS_FALSE:
            for (VariousCrawledLinkFlags lo : filter.getConditions()) {
                if (lo.matches(link)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}

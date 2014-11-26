package org.jdownloader.controlling.filter;

import java.util.HashSet;

import jd.controlling.linkcollector.VariousCrawledLinkFlags;
import jd.controlling.linkcrawler.CrawledLink;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.ConditionFilter;

public class CompiledConditionFilter extends ConditionFilter {

    private ConditionFilter                  filter;
    private HashSet<VariousCrawledLinkFlags> set;

    public CompiledConditionFilter(ConditionFilter originFilter) {
        this.filter = originFilter;
        set = new HashSet<VariousCrawledLinkFlags>();
        for (VariousCrawledLinkFlags lo : filter.getConditions()) {
            set.add(lo);
        }
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

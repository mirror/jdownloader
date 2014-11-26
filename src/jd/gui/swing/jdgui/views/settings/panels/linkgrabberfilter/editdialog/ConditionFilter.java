package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog;

import jd.controlling.linkcollector.VariousCrawledLinkFlags;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.Storable;
import org.jdownloader.controlling.filter.Filter;
import org.jdownloader.gui.translate._GUI;

public class ConditionFilter extends Filter implements Storable {
    private Matchtype                 matchType = Matchtype.IS_TRUE;
    private VariousCrawledLinkFlags[] conditions   = null;

    public VariousCrawledLinkFlags[] getConditions() {
        return conditions;
    }

    public void setConditions(VariousCrawledLinkFlags[] origins) {
        this.conditions = origins;
    }

    public ConditionFilter() {
        // STorable
    }

    public Matchtype getMatchType() {
        return matchType;
    }

    public void setMatchType(Matchtype matchType) {
        this.matchType = matchType;
    }

    public String toString() {
        switch (getMatchType()) {
        case IS_TRUE:
            StringBuilder sb = new StringBuilder();
            if (conditions != null) {
                for (VariousCrawledLinkFlags lo : conditions) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(lo.getTranslation());
                }
            }

            if (sb.length() == 0) {
                sb.append(_GUI._.OriginFilter_toString_nothing());
            }

            return _GUI._.OriginFilter_toString(sb.toString());

        case IS_FALSE:
            sb = new StringBuilder();
            if (conditions != null) {
                for (VariousCrawledLinkFlags lo : conditions) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(lo.getTranslation());
                }
            }

            if (sb.length() == 0) {
                sb.append(_GUI._.OriginFilter_toString_nothing());
            }

            return _GUI._.OriginFilter_toString_isNot(sb.toString());
        }
        throw new WTFException();
    }

    public ConditionFilter(Matchtype matchType, boolean selected, VariousCrawledLinkFlags[] originList) {
        this.matchType = matchType;
        this.enabled = selected;
        this.conditions = originList;
    }

    public static enum Matchtype {
        IS_TRUE,
        IS_FALSE
    }

}

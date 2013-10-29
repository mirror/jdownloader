package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog;

import jd.controlling.linkcollector.LinkOrigin;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.Storable;
import org.jdownloader.controlling.filter.Filter;
import org.jdownloader.gui.translate._GUI;

public class OriginFilter extends Filter implements Storable {
    private Matchtype    matchType = Matchtype.IS;
    private LinkOrigin[] origins   = null;

    public LinkOrigin[] getOrigins() {
        return origins;
    }

    public void setOrigins(LinkOrigin[] origins) {
        this.origins = origins;
    }

    public OriginFilter() {
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
        case IS:
            StringBuilder sb = new StringBuilder();
            if (origins != null) {
                for (LinkOrigin lo : origins) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(lo.getTranslation());
                }
            }

            if (sb.length() == 0) sb.append(_GUI._.OriginFilter_toString_nothing());

            return _GUI._.OriginFilter_toString(sb.toString());

        case ISNOT:
            sb = new StringBuilder();
            if (origins != null) {
                for (LinkOrigin lo : origins) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(lo.getTranslation());
                }
            }

            if (sb.length() == 0) sb.append(_GUI._.OriginFilter_toString_nothing());

            return _GUI._.OriginFilter_toString_isNot(sb.toString());
        }
        throw new WTFException();
    }

    public OriginFilter(Matchtype matchType, boolean selected, LinkOrigin[] originList) {
        this.matchType = matchType;
        this.enabled = selected;
        this.origins = originList;
    }

    public static enum Matchtype {
        IS,
        ISNOT
    }

}

package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.Storable;
import org.jdownloader.controlling.filter.Filter;
import org.jdownloader.gui.translate._GUI;

public class OnlineStatusFilter extends Filter implements Storable {
    private OnlineStatusMatchtype matchType    = OnlineStatusMatchtype.IS;
    private OnlineStatus          onlineStatus = OnlineStatus.OFFLINE;

    public OnlineStatusFilter() {
        // STorable
    }

    public OnlineStatusMatchtype getMatchType() {
        return matchType;
    }

    public void setMatchType(OnlineStatusMatchtype matchType) {
        this.matchType = matchType;
    }

    public OnlineStatus getOnlineStatus() {
        return onlineStatus;
    }

    public String toString() {
        switch (getMatchType()) {
        case IS:
            switch (getOnlineStatus()) {
            case OFFLINE:
                return _GUI._.FilterRule_toString_offline();

            case ONLINE:
                return _GUI._.FilterRule_toString_online();

            case UNCHECKABLE:
                return _GUI._.FilterRule_toString_uncheckable();

            }
        case ISNOT:
            switch (getOnlineStatus()) {
            case OFFLINE:
                return _GUI._.FilterRule_toString_offline_not();

            case ONLINE:
                return _GUI._.FilterRule_toString_online_not();

            case UNCHECKABLE:
                return _GUI._.FilterRule_toString_uncheckable_not();

            }
        }
        throw new WTFException();
    }

    public void setOnlineStatus(OnlineStatus onlineStatus) {
        this.onlineStatus = onlineStatus;
    }

    public OnlineStatusFilter(OnlineStatusMatchtype onlineStatusMatchtype, boolean selected, OnlineStatus onlineStatus) {
        this.matchType = onlineStatusMatchtype;
        this.enabled = selected;
        this.onlineStatus = onlineStatus;
    }

    public static enum OnlineStatusMatchtype {
        IS,
        ISNOT
    }

    public static enum OnlineStatus {
        UNCHECKABLE,
        ONLINE,
        OFFLINE
    }

}

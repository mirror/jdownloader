package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog;

import org.appwork.storage.Storable;
import org.jdownloader.controlling.filter.Filter;

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

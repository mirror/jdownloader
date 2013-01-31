package org.jdownloader.controlling.filter;

import jd.controlling.linkcrawler.CrawledLink.LinkState;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.OnlineStatusFilter;

public class CompiledOnlineStatusFiler extends OnlineStatusFilter {

    public CompiledOnlineStatusFiler(OnlineStatusFilter onlineStatusFilter) {
        super(onlineStatusFilter.getMatchType(), onlineStatusFilter.isEnabled(), onlineStatusFilter.getOnlineStatus());

    }

    public boolean matches(LinkState linkState) {
        switch (getMatchType()) {
        case IS:
            switch (getOnlineStatus()) {
            case OFFLINE:
                return linkState == LinkState.OFFLINE;
            case ONLINE:
                return linkState == LinkState.ONLINE;
            case UNCHECKABLE:
                return linkState == LinkState.TEMP_UNKNOWN;
            }
            return false;
        case ISNOT:
            switch (getOnlineStatus()) {
            case OFFLINE:
                return linkState != LinkState.OFFLINE;
            case ONLINE:
                return linkState != LinkState.ONLINE;
            case UNCHECKABLE:
                return linkState != LinkState.TEMP_UNKNOWN;
            }

        }

        return false;
    }

}

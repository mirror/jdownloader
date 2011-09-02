package org.jdownloader.gui.views.downloads.columns;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import jd.controlling.linkcrawler.CrawledLinkInfo;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class AvailabilityColumn extends ExtTextColumn<AbstractNode> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private String            nothing          = "";
    private ImageIcon         unknown;
    private ImageIcon         online;
    private ImageIcon         offline;

    public AvailabilityColumn() {
        super(_GUI._.AvailabilityColumn_AvailabilityColumn());
        unknown = NewTheme.I().getIcon("help", 16);
        online = NewTheme.I().getIcon("ok", 16);
        offline = NewTheme.I().getIcon("error", 16);
    }

    @Override
    protected boolean isDefaultResizable() {
        return false;
    }

    public boolean isPaintWidthLockIcon() {
        return false;
    }

    @Override
    protected Icon getIcon(AbstractNode value) {
        AvailableStatus status = null;
        DownloadLink dl = null;
        if (value instanceof DownloadLink) {
            dl = (DownloadLink) value;
            status = dl.getAvailableStatusInfo();
        } else if (value instanceof CrawledLinkInfo) {
            CrawledLinkInfo cl = (CrawledLinkInfo) value;
            dl = cl.getDownloadLink();
            if (dl != null) {
                status = dl.getAvailableStatusInfo();
            }
        }
        if (dl != null) {
            if (status != null) {
                switch (status) {
                case TRUE:
                    return online;
                case FALSE:
                    return offline;
                default:
                    return unknown;
                }
            }
            return unknown;
        }
        return null;
    }

    @Override
    protected String getTooltipText(AbstractNode value) {
        AvailableStatus status = null;
        DownloadLink dl = null;
        if (value instanceof DownloadLink) {
            dl = (DownloadLink) value;
            status = dl.getAvailableStatusInfo();
        } else if (value instanceof CrawledLinkInfo) {
            CrawledLinkInfo cl = (CrawledLinkInfo) value;
            dl = cl.getDownloadLink();
            if (dl != null) {
                status = dl.getAvailableStatusInfo();
            }
        }
        if (dl != null) {
            if (status != null) {
                switch (status) {
                case TRUE:
                    return _GUI._.linkgrabber_onlinestatus_online();
                case FALSE:
                    return _GUI._.linkgrabber_onlinestatus_offline();
                case UNCHECKABLE:
                    return _GUI._.linkgrabber_onlinestatus_uncheckable();
                case UNCHECKED:
                    return _GUI._.linkgrabber_onlinestatus_unchecked();
                }

            }
            return _GUI._.linkgrabber_onlinestatus_unchecked();
        }
        return null;
    }

    @Override
    public int getDefaultWidth() {
        return 30;
    }

    @Override
    public boolean isEnabled(AbstractNode obj) {
        return obj.isEnabled();
    }

    @Override
    public int getMinWidth() {
        return getDefaultWidth();
    }

    @Override
    public boolean isDefaultVisible() {
        return false;
    }

    @Override
    public String getStringValue(AbstractNode value) {
        return nothing;
    }

}

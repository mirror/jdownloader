package org.jdownloader.gui.views.downloads.columns;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.linkcrawler.CrawledPackageView;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.FilePackageView;

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
    private ImageIcon         mixed;

    public AvailabilityColumn() {
        super(_GUI._.AvailabilityColumn_AvailabilityColumn());
        unknown = NewTheme.I().getIcon("help", 16);
        online = NewTheme.I().getIcon("true", 16);
        mixed = NewTheme.I().getIcon("true-orange", 16);
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
            status = dl.getAvailableStatus();
            if (status == null) return unknown;
            switch (status) {
            case TRUE:
                return online;
            case FALSE:
                return offline;
            default:
                return unknown;
            }
        } else if (value instanceof CrawledLink) {
            CrawledLink cl = (CrawledLink) value;
            dl = cl.getDownloadLink();
            if (dl != null) {
                status = dl.getAvailableStatus();
                if (status == null) return unknown;
                switch (status) {
                case TRUE:
                    return online;
                case FALSE:
                    return offline;
                default:
                    return unknown;
                }
            }
        } else if (value instanceof CrawledPackage) {
            CrawledPackageView view = ((CrawledPackage) value).getView();
            int size = view.getItems().size();
            int off = view.getOfflineCount();
            int on = view.getOnlineCount();
            if (on == size) return online;
            if (off == size) return offline;
            if ((off == 0 && on == 0) || (on == 0 && off > 0)) { return unknown; }
            return mixed;
        } else if (value instanceof FilePackage) {
            FilePackageView view = ((FilePackage) value).getView();
            int size = view.getItems().size();
            int off = view.getOfflineCount();
            int on = view.getOnlineCount();
            if (on == size) return online;
            if (off == size) return offline;
            if ((off == 0 && on == 0) || (on == 0 && off > 0)) { return unknown; }
            return mixed;
        }
        return null;
    }

    @Override
    protected String getTooltipText(AbstractNode value) {
        AvailableStatus status = null;
        DownloadLink dl = null;
        if (value instanceof DownloadLink) {
            dl = (DownloadLink) value;
            status = dl.getAvailableStatus();
        } else if (value instanceof CrawledLink) {
            CrawledLink cl = (CrawledLink) value;
            dl = cl.getDownloadLink();
            if (dl != null) {
                status = dl.getAvailableStatus();
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
        return 100;
    }

    @Override
    public boolean isEnabled(AbstractNode obj) {
        if (obj instanceof CrawledPackage) { return ((CrawledPackage) obj).getView().isEnabled(); }
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
        if (value instanceof CrawledPackage) { return _GUI._.AvailabilityColumn_getStringValue_object_(((CrawledPackage) value).getView().getOnlineCount(), ((CrawledPackage) value).getView().getItems().size()); }
        if (value instanceof FilePackage) { return _GUI._.AvailabilityColumn_getStringValue_object_(((FilePackage) value).getView().getOnlineCount(), ((FilePackage) value).getView().getItems().size()); }
        return nothing;
    }

}

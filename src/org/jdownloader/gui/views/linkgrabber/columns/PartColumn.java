package org.jdownloader.gui.views.linkgrabber.columns;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.LinkInfo;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtDefaultRowSorter;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.jdownloader.gui.translate._GUI;

public class PartColumn extends ExtTextColumn<AbstractNode> {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * 
     */

    public PartColumn() {
        super(_GUI._.LinkGrabberTableModel_partcolumn());
        this.setRowSorter(new ExtDefaultRowSorter<AbstractNode>() {
            @Override
            public int compare(final AbstractNode o1, final AbstractNode o2) {
                LinkInfo p1 = getLinkInfo(o1);
                LinkInfo p2 = getLinkInfo(o2);
                int l1 = p1 == null ? -1 : p1.getPartNum();
                int l2 = p2 == null ? -1 : p2.getPartNum();

                if (l1 == l2) {
                    return 0;
                }
                if (this.getSortOrderIdentifier() == ExtColumn.SORT_ASC) {
                    return l1 > l2 ? -1 : 1;
                } else {
                    return l1 < l2 ? -1 : 1;
                }
            }

        });
    }

    @Override
    public boolean isEditable(AbstractNode obj) {
        return false;
    }

    @Override
    public boolean isEnabled(AbstractNode obj) {
        return obj.isEnabled();
    }

    @Override
    public boolean isSortable(AbstractNode obj) {
        return true;
    }

    @Override
    public int getDefaultWidth() {
        return 75;
    }

    @Override
    protected boolean isDefaultResizable() {
        return false;
    }

    @Override
    public boolean isDefaultVisible() {
        return false;
    }

    @Override
    public boolean isHidable() {
        return true;
    }

    @Override
    public String getStringValue(AbstractNode value) {
        LinkInfo linkInfo = getLinkInfo(value);
        if (linkInfo != null) {
            int num = linkInfo.getPartNum();
            if (num >= 0) {
                return String.valueOf(num);
            }
        }
        return null;
    }

    /**
     * @param value
     * @return
     */
    public LinkInfo getLinkInfo(AbstractNode value) {
        LinkInfo linkInfo = null;
        if (value instanceof CrawledLink) {
            linkInfo = ((CrawledLink) value).getLinkInfo();
        }
        if (value instanceof DownloadLink) {
            linkInfo = ((DownloadLink) value).getLinkInfo();
        }
        return linkInfo;
    }
}
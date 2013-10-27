package org.jdownloader.gui.views.linkgrabber.columns;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.PartInfo;

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
                PartInfo p1 = getPartInfo(o1);
                PartInfo p2 = getPartInfo(o2);
                int l1 = p1 == null ? -1 : p1.getNum();
                int l2 = p2 == null ? -1 : p2.getNum();

                if (l1 == l2) { return 0; }
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
        PartInfo partInfo = getPartInfo(value);
        if (partInfo != null) return partInfo.getPartID();
        return null;
    }

    /**
     * @param value
     * @return
     */
    public PartInfo getPartInfo(AbstractNode value) {
        PartInfo partInfo = null;
        if (value instanceof CrawledLink) {
            partInfo = ((CrawledLink) value).getPartInfo();
        }
        if (value instanceof DownloadLink) {
            partInfo = ((DownloadLink) value).getPartInfo();
        }
        return partInfo;
    }
}
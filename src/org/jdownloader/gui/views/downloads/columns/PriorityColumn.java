package org.jdownloader.gui.views.downloads.columns;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import jd.plugins.DownloadLink;
import jd.plugins.PackageLinkNode;

import org.appwork.utils.swing.table.ExtDefaultRowSorter;
import org.appwork.utils.swing.table.columns.ExtIconColumn;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class PriorityColumn extends ExtIconColumn<PackageLinkNode> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private ImageIcon         imgPriorityS;
    private ImageIcon         imgPriority1;
    private ImageIcon         imgPriority2;
    private ImageIcon         imgPriority3;
    private String            strPriorityS;
    private String            strPriority1;
    private String            strPriority2;
    private String            strPriority3;

    public PriorityColumn() {
        super("PriorityColumn");
        imgPriorityS = NewTheme.I().getIcon("prio_-1", 16);
        imgPriority1 = NewTheme.I().getIcon("prio_1", 16);
        imgPriority2 = NewTheme.I().getIcon("prio_2", 16);
        imgPriority3 = NewTheme.I().getIcon("prio_3", 16);
        strPriorityS = _GUI._.gui_treetable_tooltip_priority_1();
        strPriority1 = _GUI._.gui_treetable_tooltip_priority1();
        strPriority2 = _GUI._.gui_treetable_tooltip_priority2();
        strPriority3 = _GUI._.gui_treetable_tooltip_priority3();

        this.setRowSorter(new ExtDefaultRowSorter<PackageLinkNode>() {
            /**
             * sorts the icon by hashcode
             */
            @Override
            public int compare(final PackageLinkNode o1, final PackageLinkNode o2) {
                int p1 = getPriority(o1);
                int p2 = getPriority(o2);
                if (p1 == p2) { return 0; }
                if (this.isSortOrderToggle()) {
                    return p1 > p2 ? -1 : 1;
                } else {
                    return p1 < p2 ? -1 : 1;
                }
            }

        });
    }

    protected int getPriority(PackageLinkNode value) {
        if (value instanceof DownloadLink) {
            switch (((DownloadLink) value).getPriority()) {
            case 0:
            default:
                return 0;
            case -1:
                return -1;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            }
        }
        return 0;
    }

    @Override
    protected Icon getIcon(PackageLinkNode value) {
        switch (getPriority(value)) {
        case 0:
        default:
            return null;
        case -1:
            return imgPriorityS;
        case 1:
            return imgPriority1;
        case 2:
            return imgPriority2;
        case 3:
            return imgPriority3;
        }
    }

    protected String getToolTip(PackageLinkNode value) {
        switch (getPriority(value)) {
        case 0:
        default:
            return null;
        case -1:
            return strPriorityS;
        case 1:
            return strPriority1;
        case 2:
            return strPriority2;
        case 3:
            return strPriority3;
        }
    }

    @Override
    public boolean isEnabled(PackageLinkNode obj) {
        return obj.isEnabled();
    }

}
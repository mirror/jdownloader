package org.jdownloader.gui.views.downloads.columns;


 import org.jdownloader.gui.translate.*;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import jd.plugins.DownloadLink;
import jd.plugins.PackageLinkNode;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

import org.appwork.utils.swing.table.ExtDefaultRowSorter;
import org.appwork.utils.swing.table.columns.ExtIconColumn;

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
        imgPriorityS = JDTheme.II("gui.images.priority-1", 16, 16);
        imgPriority1 = JDTheme.II("gui.images.priority1", 16, 16);
        imgPriority2 = JDTheme.II("gui.images.priority2", 16, 16);
        imgPriority3 = JDTheme.II("gui.images.priority3", 16, 16);
        strPriorityS = T._.gui_treetable_tooltip_priority_1();
        strPriority1 = T._.gui_treetable_tooltip_priority1();
        strPriority2 = T._.gui_treetable_tooltip_priority2();
        strPriority3 = T._.gui_treetable_tooltip_priority3();

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

}
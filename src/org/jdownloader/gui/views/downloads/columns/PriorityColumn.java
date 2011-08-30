package org.jdownloader.gui.views.downloads.columns;

import java.awt.Component;
import java.util.Date;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtDefaultRowSorter;
import org.appwork.swing.exttable.ExtTableHeaderRenderer;
import org.appwork.swing.exttable.columns.ExtIconColumn;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class PriorityColumn extends ExtIconColumn<AbstractNode> {

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

    public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

        final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {

            private static final long serialVersionUID = 2051980596953422289L;

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setIcon(NewTheme.I().getIcon("prio_3", 14));
                setHorizontalAlignment(CENTER);
                setText(null);
                return this;
            }

        };

        return ret;
    }

    public PriorityColumn() {
        super(_GUI._.PriorityColumn_PriorityColumn());
        imgPriorityS = NewTheme.I().getIcon("prio_-1", 16);
        imgPriority1 = NewTheme.I().getIcon("prio_1", 16);
        imgPriority2 = NewTheme.I().getIcon("prio_2", 16);
        imgPriority3 = NewTheme.I().getIcon("prio_3", 16);
        strPriorityS = _GUI._.gui_treetable_tooltip_priority_1();
        strPriority1 = _GUI._.gui_treetable_tooltip_priority1();
        strPriority2 = _GUI._.gui_treetable_tooltip_priority2();
        strPriority3 = _GUI._.gui_treetable_tooltip_priority3();

        this.setRowSorter(new ExtDefaultRowSorter<AbstractNode>() {
            /**
             * sorts the icon by hashcode
             */
            @Override
            public int compare(final AbstractNode o1, final AbstractNode o2) {
                int p1 = getPriority(o1);
                int p2 = getPriority(o2);
                if (p1 == p2) { return 0; }
                if (this.getSortOrderIdentifier() == ExtColumn.SORT_ASC) {
                    return p1 > p2 ? -1 : 1;
                } else {
                    return p1 < p2 ? -1 : 1;
                }
            }

        });
    }

    @Override
    protected boolean isDefaultResizable() {
        return false;
    }

    @Override
    public boolean isDefaultVisible() {
        return false;
    }

    public static void main(String[] args) {
        System.out.println(new Date());
    }

    public boolean isPaintWidthLockIcon() {
        return false;
    }

    // /**
    // * Sets max width to 30. overwrite to set other maxsizes
    // */
    // @Override
    // public int getMaxWidth() {
    // return 30;
    // }

    @Override
    public int getMinWidth() {
        return 20;
    }

    @Override
    public int getDefaultWidth() {
        return 30;
    }

    protected int getPriority(AbstractNode value) {
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
    protected Icon getIcon(AbstractNode value) {
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

    protected String getTooltipText(AbstractNode value) {
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
    public boolean isEnabled(AbstractNode obj) {
        return obj.isEnabled();
    }

}
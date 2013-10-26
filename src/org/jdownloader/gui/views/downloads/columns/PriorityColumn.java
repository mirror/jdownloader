package org.jdownloader.gui.views.downloads.columns;

import java.awt.Component;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.AbstractButton;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.JTableHeader;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.resources.AWUTheme;
import org.appwork.swing.action.BasicAction;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtDefaultRowSorter;
import org.appwork.swing.exttable.ExtTableHeaderRenderer;
import org.appwork.swing.exttable.columns.ExtComboColumn;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.jdownloader.controlling.Priority;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class PriorityColumn extends ExtComboColumn<AbstractNode, Priority> {

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
    private ImageIcon         imgPriorityDefault;

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

    public JPopupMenu createHeaderPopup() {

        return FileColumn.createColumnPopup(this, getMinWidth() == getMaxWidth() && getMaxWidth() > 0);

    }

    public PriorityColumn() {

        super(_GUI._.PriorityColumn_PriorityColumn(), new DefaultComboBoxModel<Priority>(Priority.values()));
        imgPriorityS = NewTheme.I().getIcon("prio_-1", 16);
        imgPriorityDefault = NewTheme.I().getIcon(IconKey.ICON_PRIO_0, 16);
        imgPriority1 = NewTheme.I().getIcon("prio_1", 16);
        imgPriority2 = NewTheme.I().getIcon("prio_2", 16);
        imgPriority3 = NewTheme.I().getIcon("prio_3", 16);
        rendererField.setHorizontalTextPosition(SwingConstants.RIGHT);
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
                int p1 = getPriority(o1).ordinal();
                int p2 = getPriority(o2).ordinal();
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

    // /**
    // * Sets max width to 30. overwrite to set other maxsizes
    // */
    // @Override
    // public int getMaxWidth() {
    // return 30;
    // }

    @Override
    public int getMinWidth() {
        return 35;
    }

    protected AbstractButton getPopupElement(final Priority o, final boolean selected) {
        return new JMenuItem(new BasicAction(o.toString()) {
            {

                setName(modelItemToString(o));
                ImageIcon ico = getIconByPriority(o);

                if (selected) {
                    Image checkBox = AWUTheme.I().getImage("enable", 16);
                    BufferedImage back = ImageProvider.merge(ico.getImage(), checkBox, 5, 0, 0, ico.getImage().getHeight(null) - checkBox.getHeight(null) + 5);

                    setSmallIcon(new ImageIcon(back));
                } else {
                    setSmallIcon(ico);
                }

            }

            @Override
            public void actionPerformed(final ActionEvent e) {

            }

        });
    }

    @Override
    public void configureRendererComponent(AbstractNode value, boolean isSelected, boolean hasFocus, int row, int column) {
        super.configureRendererComponent(value, isSelected, hasFocus, row, column);
        rendererField.setText("");
        rendererField.setIcon(getPriorityIcon(value));
    }

    @Override
    public boolean onRenameClick(MouseEvent e, AbstractNode obj) {
        return super.onRenameClick(e, obj);
    }

    public boolean onSingleClick(final MouseEvent e, final AbstractNode value) {
        return false;
    }

    @Override
    public int getDefaultWidth() {
        return 35;
    }

    @Override
    public boolean isEditable(final AbstractNode obj) {
        return true;
    }

    // protected int getPriority(AbstractNode value) {
    // Integer p = null;
    // if (value instanceof DownloadLink) {
    // p = ((DownloadLink) value).getPriority();
    // } else if (value instanceof CrawledLink) {
    // p = ((CrawledLink) value).getPriority().getId();
    // }
    // if (p != null) {
    // switch (p) {
    // case 0:
    // default:
    // return 0;
    // case -1:
    // return -1;
    // case 1:
    // return 1;
    // case 2:
    // return 2;
    // case 3:
    // return 3;
    // }
    // }
    // return 0;
    // }

    protected Icon getPriorityIcon(AbstractNode value) {
        Priority p = getPriority(value);
        return getIconByPriority(p);
    }

    /**
     * @param p
     * @return
     */
    protected ImageIcon getIconByPriority(Priority p) {
        switch (p) {
        case DEFAULT:
        default:
            return imgPriorityDefault;
        case LOWER:
            return imgPriorityS;
        case HIGH:
            return imgPriority1;
        case HIGHER:
            return imgPriority2;
        case HIGHEST:
            return imgPriority3;
        }
    }

    private Priority getPriority(AbstractNode value) {
        if (value instanceof DownloadLink) {
            return ((DownloadLink) value).getPriorityEnum();
        } else if (value instanceof CrawledLink) {
            return ((CrawledLink) value).getPriority();
        } else if (value instanceof FilePackage) {
            return ((FilePackage) value).getView().getHighestPriority();
        } else if (value instanceof CrawledPackage) {

        return ((CrawledPackage) value).getView().getHighestPriority();

        }
        return Priority.DEFAULT;
    }

    protected String getTooltipText(AbstractNode value) {
        switch (getPriority(value)) {
        case DEFAULT:
        default:
            return null;
        case LOWER:
            return strPriorityS;
        case HIGH:
            return strPriority1;
        case HIGHER:
            return strPriority2;
        case HIGHEST:
            return strPriority3;
        }
    }

    @Override
    public boolean isEnabled(AbstractNode obj) {
        if (obj instanceof CrawledPackage) { return ((CrawledPackage) obj).getView().isEnabled(); }
        if (obj instanceof FilePackage) { return ((FilePackage) obj).getView().isEnabled(); }
        return obj.isEnabled();
    }

    @Override
    protected Priority getSelectedItem(AbstractNode value) {
        return getPriority(value);
    }

    @Override
    protected void setSelectedItem(AbstractNode object, Priority value) {
        if (object instanceof DownloadLink) {
            ((DownloadLink) object).setPriorityEnum(value);
        } else if (object instanceof CrawledLink) {
            ((CrawledLink) object).setPriority(value);
        } else if (object instanceof FilePackage) {
            boolean readL = ((FilePackage) object).getModifyLock().readLock();
            try {
                for (DownloadLink dl : ((FilePackage) object).getChildren()) {
                    dl.setPriorityEnum(value);
                }
            } finally {
                ((FilePackage) object).getModifyLock().readUnlock(readL);
            }

        } else if (object instanceof CrawledPackage) {

            boolean readL = ((CrawledPackage) object).getModifyLock().readLock();
            try {
                for (CrawledLink dl : ((CrawledPackage) object).getChildren()) {
                    dl.setPriority(value);
                }
            } finally {
                ((CrawledPackage) object).getModifyLock().readUnlock(readL);
            }
        }
    }
}
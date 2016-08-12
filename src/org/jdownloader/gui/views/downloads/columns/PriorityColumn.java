package org.jdownloader.gui.views.downloads.columns;

import java.awt.Component;
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

import jd.controlling.TaskQueue;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.swing.action.BasicAction;
import org.appwork.swing.components.CheckBoxIcon;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtDefaultRowSorter;
import org.appwork.swing.exttable.ExtTableHeaderRenderer;
import org.appwork.swing.exttable.columns.ExtComboColumn;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.images.IconIO;
import org.jdownloader.controlling.Priority;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.BadgeIcon;
import org.jdownloader.images.NewTheme;

public class PriorityColumn extends ExtComboColumn<AbstractNode, Priority> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private final Icon        imgPriority_3;
    private final Icon        imgPriority_2;
    private final Icon        imgPriority_1;
    private final Icon        imgPriority0;
    private final Icon        imgPriority1;
    private final Icon        imgPriority2;
    private final Icon        imgPriority3;

    private final Icon        imgPriority0__3;
    private final Icon        imgPriority0__2;
    private final Icon        imgPriority0__1;
    private final Icon        imgPriority0_0;
    private final Icon        imgPriority0_1;
    private final Icon        imgPriority0_2;
    private final Icon        imgPriority0_3;

    public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

        final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {

            private static final long serialVersionUID = 2051980596953422289L;

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setIcon(new AbstractIcon(IconKey.ICON_PRIO_3, 14));
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
        super(_GUI.T.PriorityColumn_PriorityColumn(), new DefaultComboBoxModel<Priority>(Priority.values()));
        imgPriority_3 = NewTheme.I().getIcon(IconKey.ICON_PRIO__3, 16);
        imgPriority_2 = NewTheme.I().getIcon(IconKey.ICON_PRIO__2, 16);
        imgPriority_1 = NewTheme.I().getIcon(IconKey.ICON_PRIO__1, 16);
        imgPriority0 = NewTheme.I().getIcon(IconKey.ICON_PRIO_0, 16);
        imgPriority1 = NewTheme.I().getIcon(IconKey.ICON_PRIO_1, 16);
        imgPriority2 = NewTheme.I().getIcon(IconKey.ICON_PRIO_2, 16);
        imgPriority3 = NewTheme.I().getIcon(IconKey.ICON_PRIO_3, 16);

        imgPriority0__3 = new BadgeIcon(imgPriority0, IconIO.getScaledInstance(imgPriority_3, 12, 12), 4, 2);
        imgPriority0__2 = new BadgeIcon(imgPriority0, IconIO.getScaledInstance(imgPriority_2, 12, 12), 4, 2);
        imgPriority0__1 = new BadgeIcon(imgPriority0, IconIO.getScaledInstance(imgPriority_1, 12, 12), 4, 2);
        imgPriority0_0 = new BadgeIcon(imgPriority0, IconIO.getScaledInstance(imgPriority0, 12, 12), 4, 2);
        imgPriority0_1 = new BadgeIcon(imgPriority0, IconIO.getScaledInstance(imgPriority1, 12, 12), 4, 2);
        imgPriority0_2 = new BadgeIcon(imgPriority0, IconIO.getScaledInstance(imgPriority2, 12, 12), 4, 2);
        imgPriority0_3 = new BadgeIcon(imgPriority0, IconIO.getScaledInstance(imgPriority3, 12, 12), 4, 2);

        rendererField.setHorizontalTextPosition(SwingConstants.RIGHT);
        this.setRowSorter(new ExtDefaultRowSorter<AbstractNode>() {
            /**
             * sorts the icon by hashcode
             */
            @Override
            public int compare(final AbstractNode o1, final AbstractNode o2) {
                int p1 = getPriority(o1).ordinal();
                int p2 = getPriority(o2).ordinal();
                if (p1 == p2) {
                    return 0;
                }
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

    @Override
    public int getMinWidth() {
        return 35;
    }

    protected AbstractButton getPopupElement(final Priority o, final boolean selected, final AbstractNode value) {
        return new JMenuItem(new BasicAction(o.toString()) {
            {

                setName(modelItemToString(o, value));
                Icon ico = getIconByPriority(o);

                if (selected) {
                    CheckBoxIcon checkBox = CheckBoxIcon.TRUE;
                    BufferedImage back = ImageProvider.merge(ico, checkBox, 5, 0, 0, ico.getIconHeight() - checkBox.getIconHeight() + 5);

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
        rendererField.setIcon(getIconByPriority(value));
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

    /**
     * @param p
     * @return
     */
    protected Icon getIconByPriority(Priority p) {
        switch (p) {
        case DEFAULT:
        default:
            return imgPriority0;
        case LOWEST:
            return imgPriority_3;
        case LOWER:
            return imgPriority_2;
        case LOW:
            return imgPriority_1;
        case HIGH:
            return imgPriority1;
        case HIGHER:
            return imgPriority2;
        case HIGHEST:
            return imgPriority3;
        }
    }

    protected Icon getInheritedIconByPriority(Priority p) {
        switch (p) {
        case DEFAULT:
        default:
            return imgPriority0;
        case LOWEST:
            return imgPriority0__3;
        case LOWER:
            return imgPriority0__2;
        case LOW:
            return imgPriority0__1;
        case HIGH:
            return imgPriority0_1;
        case HIGHER:
            return imgPriority0_2;
        case HIGHEST:
            return imgPriority0_3;
        }
    }

    protected Icon getIconByPriority(AbstractNode value) {
        if (value instanceof DownloadLink) {
            final DownloadLink link = ((DownloadLink) value);
            final Priority ret = link.getPriorityEnum();
            if (Priority.DEFAULT.equals(ret)) {
                final FilePackage parent = link.getFilePackage();
                if (parent != null) {
                    return getInheritedIconByPriority(parent.getPriorityEnum());
                }
            }
            return getIconByPriority(ret);
        } else if (value instanceof CrawledLink) {
            final CrawledLink link = ((CrawledLink) value);
            final Priority ret = link.getPriority();
            if (Priority.DEFAULT.equals(ret)) {
                final CrawledPackage parent = link.getParentNode();
                if (parent != null) {
                    return getInheritedIconByPriority(parent.getPriorityEnum());
                }
            }
            return getIconByPriority(ret);
        } else if (value instanceof FilePackage) {
            return getIconByPriority(((FilePackage) value).getPriorityEnum());
        } else if (value instanceof CrawledPackage) {
            return getIconByPriority(((CrawledPackage) value).getPriorityEnum());
        }
        return imgPriority0;
    }

    private Priority getPriority(AbstractNode value) {
        if (value instanceof DownloadLink) {
            final DownloadLink link = ((DownloadLink) value);
            final Priority ret = link.getPriorityEnum();
            if (Priority.DEFAULT.equals(ret)) {
                FilePackage parent = link.getFilePackage();
                if (parent != null) {
                    return parent.getPriorityEnum();
                }
            }
            return ret;
        } else if (value instanceof CrawledLink) {
            final CrawledLink link = ((CrawledLink) value);
            final Priority ret = link.getPriority();
            if (Priority.DEFAULT.equals(ret)) {
                final CrawledPackage parent = link.getParentNode();
                if (parent != null) {
                    return parent.getPriorityEnum();
                }
            }
            return ret;
        } else if (value instanceof FilePackage) {
            return ((FilePackage) value).getPriorityEnum();
        } else if (value instanceof CrawledPackage) {
            return ((CrawledPackage) value).getPriorityEnum();
        }
        return Priority.DEFAULT;
    }

    protected String getTooltipText(AbstractNode value) {
        final Priority priority = getPriority(value);
        if (priority != null && priority != Priority.DEFAULT) {
            return priority.T();
        } else {
            return null;
        }
    }

    @Override
    public boolean isEnabled(AbstractNode obj) {
        if (obj instanceof CrawledPackage) {
            return ((CrawledPackage) obj).getView().isEnabled();
        }
        if (obj instanceof FilePackage) {
            return ((FilePackage) obj).getView().isEnabled();
        }
        return obj.isEnabled();
    }

    @Override
    protected Priority getSelectedItem(AbstractNode value) {
        return getPriority(value);
    }

    @Override
    protected void setSelectedItem(final AbstractNode object, final Priority value) {
        TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                if (object instanceof DownloadLink) {
                    ((DownloadLink) object).setPriorityEnum(value);
                } else if (object instanceof CrawledLink) {
                    ((CrawledLink) object).setPriority(value);
                } else if (object instanceof FilePackage) {
                    ((FilePackage) object).setPriorityEnum(value);
                } else if (object instanceof CrawledPackage) {
                    ((CrawledPackage) object).setPriorityEnum(value);
                }
                return null;
            }

        });

    }
}
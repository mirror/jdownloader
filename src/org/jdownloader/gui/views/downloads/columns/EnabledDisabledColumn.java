package org.jdownloader.gui.views.downloads.columns;

import java.awt.Component;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.appwork.swing.components.CheckBoxIcon;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtDefaultRowSorter;
import org.appwork.swing.exttable.ExtTableHeaderRenderer;
import org.appwork.swing.exttable.columns.ExtIconColumn;
import org.appwork.utils.ModifyLock;
import org.jdownloader.gui.translate._GUI;

/**
 * Class giving the implementation details of the enabled / disabled column type
 *
 * @author pp_me
 *
 */
public class EnabledDisabledColumn extends ExtIconColumn<AbstractNode> {
    /**
     *
     */
    private final Icon iconNo;
    private final Icon iconYes;

    public EnabledDisabledColumn() {
        super(_GUI.T.EnabledDisabledColumn_EnabledDisabledColumn());
        iconYes = new CheckBoxIcon(true, true);
        iconNo = new CheckBoxIcon(false, true);
        /**
         * Custom sorting for this type of ExtIconColumn We need custom sorting as the usual implementation only sorts links within packages
         * but does not sort the packages. The custom sort allows disabled links to be moved to the top of the download list.
         */
        super.setRowSorter(new ExtDefaultRowSorter<AbstractNode>() {
            @Override
            public int compare(final AbstractNode o1, final AbstractNode o2) {
                if (o1 instanceof AbstractPackageNode && o2 instanceof AbstractPackageNode) {
                    return sortPackages((AbstractPackageNode) o1, (AbstractPackageNode) o2);
                } else {
                    return sortLinks(o1, o2);
                }
            }

            /**
             * Method used to sort the packages list based on the status of their children
             *
             * @param o1
             *            Cell in download list
             * @param o2
             *            Cell in download list
             * @return 1,0,-1 depending on output of sort
             */
            private final int sortPackages(final AbstractPackageNode o1, final AbstractPackageNode o2) {
                int h1 = 0;
                int h2 = 0;
                ModifyLock lock = o1.getModifyLock();
                boolean readL = lock.readLock();
                try {
                    for (Object link : o1.getChildren()) {
                        if (!((AbstractNode) link).isEnabled()) {
                            h1 = -1;
                        } else {
                            h1 = 1;
                        }
                        break;
                    }
                } finally {
                    lock.readUnlock(readL);
                }
                lock = o2.getModifyLock();
                readL = lock.readLock();
                try {
                    for (Object link : o2.getChildren()) {
                        if (!((AbstractNode) link).isEnabled()) {
                            h2 = -1;
                        } else {
                            h2 = 1;
                        }
                        break;
                    }
                } finally {
                    lock.readUnlock(readL);
                }
                return finalDecision(h1, h2);
            }

            /**
             * Method for sorting links within packages
             *
             * @param o1
             *            Cell in download list
             * @param o2
             *            Cell in download list
             * @return 1,0,-1 depending on output of sort
             */
            private final int sortLinks(final AbstractNode o1, final AbstractNode o2) {
                return finalDecision(o1.isEnabled() ? 1 : 0, o2.isEnabled() ? 1 : 0);
            }

            /**
             * Method to decide if the sort on this cell will return 1,0,-1
             *
             * @param h1
             * @param h2
             * @return
             */
            private final int finalDecision(int h1, int h2) {
                if (h1 == h2) {
                    return 0;
                } else if (this.getSortOrderIdentifier() == ExtColumn.SORT_ASC) {
                    return h1 > h2 ? -1 : 1;
                } else {
                    return h2 > h1 ? -1 : 1;
                }
            }
        });
    }

    /**
     * Click handler on single click to enable or disable a link based on clicking on the cell in the download list
     */
    public boolean onSingleClick(final MouseEvent e, final AbstractNode obj) {
        if (obj.isEnabled()) {
            obj.setEnabled(false);
        } else {
            obj.setEnabled(true);
        }
        return false;
    }

    public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {
        final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setIcon(CheckBoxIcon.TRUE);
                // defaultProxy
                setHorizontalAlignment(CENTER);
                setText(null);
                setToolTipText(_GUI.T.StatusColumn_StatusColumn2());
                return this;
            }
        };
        return ret;
    }

    public JPopupMenu createHeaderPopup() {
        return FileColumn.createColumnPopup(this, getMinWidth() == getMaxWidth() && getMaxWidth() > 0);
    }

    @Override
    public boolean isDefaultVisible() {
        return false;
    }

    @Override
    protected boolean isEditable(final AbstractNode obj, final boolean enabled) {
        return false;
    }

    @Override
    public int getMaxWidth() {
        return 100;
    }

    @Override
    public int getMinWidth() {
        return 12;
    }

    @Override
    public int getDefaultWidth() {
        return 22;
    }

    @Override
    public boolean isEditable(AbstractNode obj) {
        return false;
    }

    @Override
    public boolean isEnabled(final AbstractNode obj) {
        return true;
    }

    @Override
    protected Icon getIcon(final AbstractNode value) {
        if (value != null) {
            return getIconToDisplay(value.isEnabled());
        }
        return null;
    }

    private Icon getIconToDisplay(final boolean isEnabled) {
        if (isEnabled) {
            return iconYes;
        } else {
            return iconNo;
        }
    }
}
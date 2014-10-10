package org.jdownloader.gui.views.downloads.columns;

import java.awt.Component;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.swing.components.CheckBoxIcon;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtDefaultRowSorter;
import org.appwork.swing.exttable.ExtTableHeaderRenderer;
import org.appwork.swing.exttable.columns.ExtIconColumn;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

/**
 * Class giving the implementation details of the enabled / disabled column type
 *
 * @author pp_me
 *
 */
public class EnabledDisabledColumn extends ExtIconColumn<AbstractNode> {

    private Icon iconNo;
    private Icon iconYes;

    public EnabledDisabledColumn() {
        super(_GUI._.EnabledDisabledColumn_EnabledDisabledColumn());

        iconYes = new CheckBoxIcon(true, true);
        iconNo = new CheckBoxIcon(false, true);

        /**
         * Custom sorting for this type of ExtIconColumn We need custom sorting as the usual implementation only sorts links within packages
         * but does not sort the packages. The custom sort allows disabled links to be moved to the top of the download list.
         */
        super.setRowSorter(new ExtDefaultRowSorter<AbstractNode>() {

            int h1 = 0;
            int h2 = 0;

            @Override
            public int compare(final AbstractNode o1, final AbstractNode o2) {
                if (o1 instanceof FilePackage && o2 instanceof FilePackage) {
                    return sortPackages(o1, o2);
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
            private int sortPackages(final AbstractNode o1, final AbstractNode o2) {
                for (DownloadLink link : ((FilePackage) o1).getChildren()) {
                    if (!link.isEnabled()) {
                        h1 = -1;
                    } else {
                        h1 = 1;
                    }
                }
                for (DownloadLink link : ((FilePackage) o2).getChildren()) {
                    if (!link.isEnabled()) {
                        h2 = -1;
                    } else {
                        h2 = 1;
                    }
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
            private int sortLinks(final AbstractNode o1, final AbstractNode o2) {
                final Icon ic1 = getIcon(o1);
                final Icon ic2 = getIcon(o2);
                h1 = ic1 == null ? 0 : ic1.hashCode();
                h2 = ic2 == null ? 0 : ic2.hashCode();
                return finalDecision(h1, h2);
            }

            /**
             * Method to decide if the sort on this cell will return 1,0,-1
             *
             * @param h1
             * @param h2
             * @return
             */
            private int finalDecision(int h1, int h2) {
                if (h1 == h2) {
                    return 0;
                }
                if (this.getSortOrderIdentifier() == ExtColumn.SORT_ASC) {
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
                setIcon(NewTheme.I().getIcon(IconKey.ICON_ENABLED, 14));
                // defaultProxy
                setHorizontalAlignment(CENTER);
                setText(null);
                setToolTipText(_GUI._.StatusColumn_StatusColumn());
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
    protected int getMaxWidth() {
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
    protected Icon getIcon(AbstractNode value) {
        if (value instanceof FilePackage) {
            FilePackage pack = ((FilePackage) value);
            return getIconToDisplay(pack.isEnabled());
        } else if (value instanceof DownloadLink) {
            DownloadLink dlink = ((DownloadLink) value);
            return getIconToDisplay(dlink.isEnabled());
        } else if (value instanceof CrawledLink) {
            DownloadLink dlink = ((CrawledLink) value).getDownloadLink();
            return getIconToDisplay(dlink.isEnabled());
        } else {
            return null;
        }
    }

    private Icon getIconToDisplay(boolean isEnabled) {
        if (isEnabled) {
            return iconYes;
        } else {
            return iconNo;
        }
    }
}
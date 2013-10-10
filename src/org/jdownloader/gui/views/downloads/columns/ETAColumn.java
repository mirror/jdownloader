package org.jdownloader.gui.views.downloads.columns;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import jd.controlling.packagecontroller.AbstractNode;
import jd.nutils.Formatter;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginProgress;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtDefaultRowSorter;
import org.appwork.swing.exttable.columnmenu.LockColumnWidthAction;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.plugins.ConditionalSkipReason;
import org.jdownloader.plugins.TimeOutCondition;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class ETAColumn extends ExtTextColumn<AbstractNode> {

    private class ColumnHelper {
        private ImageIcon icon   = null;
        private long      eta    = -1;
        private String    string = null;
    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private ImageIcon         wait;
    private ImageIcon         ipwait;
    private ColumnHelper      columnHelper     = new ColumnHelper();

    @Override
    public int getDefaultWidth() {
        return 80;
    }

    @Override
    public boolean isEnabled(AbstractNode obj) {
        return obj.isEnabled();
    }

    // @Override
    // public int getMaxWidth() {
    //
    // return 85;line
    // }
    @Override
    protected boolean isDefaultResizable() {
        return false;
    }

    public ETAColumn() {
        super(_GUI._.ETAColumn_ETAColumn());
        rendererField.setHorizontalAlignment(SwingConstants.RIGHT);
        this.wait = NewTheme.I().getIcon("wait", 16);
        this.ipwait = NewTheme.I().getIcon("auto-reconnect", 16);

        this.setRowSorter(new ExtDefaultRowSorter<AbstractNode>() {
            private ColumnHelper helper1 = new ColumnHelper();
            private ColumnHelper helper2 = new ColumnHelper();

            @Override
            public int compare(final AbstractNode o1, final AbstractNode o2) {
                if (o1 == o2) return 0;
                fillColumnHelper(o1, helper1);
                fillColumnHelper(o2, helper2);
                final long l1 = helper1.eta;
                final long l2 = helper2.eta;
                if (l1 == l2) { return 0; }
                if (this.getSortOrderIdentifier() == ExtColumn.SORT_ASC) {
                    return l1 > l2 ? -1 : 1;
                } else {
                    return l1 < l2 ? -1 : 1;
                }
            }

        });
    }

    private void fillColumnHelper(AbstractNode value, ColumnHelper columnHelper) {
        if (value instanceof DownloadLink) {
            DownloadLink link = (DownloadLink) value;
            PluginProgress progress = null;
            if ((progress = link.getPluginProgress()) != null) {
                columnHelper.icon = progress.getIcon();
                columnHelper.string = progress.getMessage(this);
                long eta = progress.getETA();
                if (eta >= 0) {
                    columnHelper.eta = eta;
                } else {
                    columnHelper.eta = -1;
                }
                return;
            }
            ConditionalSkipReason conditionalSkipReason = link.getConditionalSkipReason();
            if (conditionalSkipReason != null && !conditionalSkipReason.isConditionReached()) {
                if (conditionalSkipReason instanceof TimeOutCondition) {
                    long time = ((TimeOutCondition) conditionalSkipReason).getTimeOutLeft();
                    columnHelper.icon = conditionalSkipReason.getIcon(this, value);
                    columnHelper.eta = time;
                    columnHelper.string = conditionalSkipReason.getMessage(this, value);
                    return;
                } else {
                    columnHelper.icon = conditionalSkipReason.getIcon(this, value);
                    columnHelper.eta = -1;
                    columnHelper.string = conditionalSkipReason.getMessage(this, value);
                    return;
                }
            }
            columnHelper.icon = null;
            columnHelper.string = null;
            columnHelper.eta = -1;
        } else {
            columnHelper.icon = null;
            FilePackage fp = (FilePackage) value;
            long eta = fp.getView().getETA();
            if (eta > 0) {
                columnHelper.eta = eta;
                columnHelper.string = Formatter.formatSeconds(eta);
            } else if (eta == Integer.MIN_VALUE) {
                columnHelper.eta = Long.MAX_VALUE;
                columnHelper.string = "\u221E";
            } else {
                columnHelper.eta = 0;
                columnHelper.string = null;
            }
        }
    }

    @Override
    protected void prepareColumn(AbstractNode value) {
        fillColumnHelper(value, columnHelper);
    }

    @Override
    protected Icon getIcon(AbstractNode value) {
        return columnHelper.icon;
    }

    public JPopupMenu createHeaderPopup() {

        final JPopupMenu ret = new JPopupMenu();
        LockColumnWidthAction action;
        ret.add(new JCheckBoxMenuItem(action = new LockColumnWidthAction(this)));

        ret.add(new JCheckBoxMenuItem(new AppAction() {
            {
                setName(_GUI._.literall_premium_alert());
                setSmallIcon(wait);
                setSelected(JsonConfig.create(GraphicalUserInterfaceSettings.class).isPremiumAlertETAColumnEnabled());
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                JsonConfig.create(GraphicalUserInterfaceSettings.class).setPremiumAlertETAColumnEnabled(!JsonConfig.create(GraphicalUserInterfaceSettings.class).isPremiumAlertETAColumnEnabled());
            }
        }));
        ret.add(new JSeparator());
        return ret;

    }

    public boolean onSingleClick(final MouseEvent e, final AbstractNode value) {
        return TaskColumn.handleIPBlockCondition(e, value);
    }

    @Override
    public String getStringValue(AbstractNode value) {
        return columnHelper.string;
    }
}

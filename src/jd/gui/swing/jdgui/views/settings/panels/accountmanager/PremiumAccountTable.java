package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.LinkedList;

import javax.swing.JPopupMenu;

import jd.gui.swing.jdgui.BasicJDTable;
import jd.gui.swing.jdgui.components.premiumbar.ServiceCollection;
import jd.gui.swing.jdgui.components.premiumbar.ServicePanel;

import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.swing.components.tooltips.ToolTipController;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtComponentRowHighlighter;
import org.jdownloader.settings.GraphicalUserInterfaceSettings.PremiumStatusBarDisplay;
import org.jdownloader.updatev2.gui.LAFOptions;

public class PremiumAccountTable extends BasicJDTable<AccountEntry> {

    private static final long serialVersionUID = -2166408567306279016L;

    public PremiumAccountTable(AccountListPanel accountListPanel) {
        super(new PremiumAccountTableModel(accountListPanel));
        this.setSearchEnabled(true);
        this.getModel().addExtComponentRowHighlighter(new ExtComponentRowHighlighter<AccountEntry>((LAFOptions.getInstance().getColorForTableSelectedRowsForeground()), (LAFOptions.getInstance().getColorForTableSelectedRowsBackground()), null) {
            public int getPriority() {
                return Integer.MAX_VALUE - 1;
            }

            @Override
            public boolean accept(ExtColumn<AccountEntry> column, AccountEntry value, boolean selected, boolean focus, int row) {
                return selected;
            }

        });

        this.getModel().addExtComponentRowHighlighter(new ExtComponentRowHighlighter<AccountEntry>(LAFOptions.getInstance().getColorForTableAccountTempErrorRowForeground(), LAFOptions.getInstance().getColorForTableAccountTempErrorRowBackground(), null) {
            public int getPriority() {
                return Integer.MAX_VALUE;
            }

            @Override
            protected Color getBackground(Color current) {
                return super.getBackground(current);
            }

            @Override
            public boolean accept(ExtColumn<AccountEntry> column, AccountEntry value, boolean selected, boolean focus, int row) {
                return value.getAccount().isTempDisabled();
            }

        });
        this.getModel().addExtComponentRowHighlighter(new ExtComponentRowHighlighter<AccountEntry>(LAFOptions.getInstance().getColorForTableAccountErrorRowForeground(), LAFOptions.getInstance().getColorForTableAccountErrorRowBackground(), null) {
            public int getPriority() {
                return Integer.MAX_VALUE;
            }

            @Override
            protected Color getBackground(Color current) {
                return super.getBackground(current);
            }

            @Override
            public boolean accept(ExtColumn<AccountEntry> column, AccountEntry value, boolean selected, boolean focus, int row) {
                return !value.getAccount().isValid();
            }

        });
    }

    @Override
    protected void addSelectionHighlighter() {
        super.addSelectionHighlighter();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.swing.exttable.ExtTable#onShortcutDelete(java.util.java.util.List , java.awt.event.KeyEvent, boolean)
     */
    @Override
    protected boolean onShortcutDelete(java.util.List<AccountEntry> selectedObjects, KeyEvent evt, boolean direct) {
        new RemoveAction(selectedObjects, direct).actionPerformed(null);
        return true;
    }

    @Override
    protected boolean onDoubleClick(MouseEvent e, AccountEntry obj) {
        ToolTipController.getInstance().show(this);
        return true;
    }

    @Override
    protected ExtTooltip createToolTip(ExtColumn<AccountEntry> col, int row, Point position, AccountEntry elementAt) {
        if (elementAt == null || elementAt.getAccount() == null) return null;
        try {
            LinkedList<ServiceCollection<?>> services = ServicePanel.getInstance().groupServices(PremiumStatusBarDisplay.GROUP_BY_ACCOUNT_TYPE, false, elementAt.getAccount().getHoster());
            if (services.size() > 0) { return services.get(0).createTooltip(null); }
        } catch (Exception e) {
            // bad sync creates nullpointers..
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.swing.exttable.ExtTable#onContextMenu(javax.swing.JPopupMenu , java.lang.Object, java.util.java.util.List,
     * org.appwork.swing.exttable.ExtColumn)
     */
    @Override
    protected JPopupMenu onContextMenu(JPopupMenu popup, AccountEntry contextObject, java.util.List<AccountEntry> selection, ExtColumn<AccountEntry> column, MouseEvent ev) {
        if (popup != null) {
            if (selection == null) {
                popup.add(new NewAction());
                popup.add(new RemoveAction(selection, false));
                popup.add(new BuyAction());
                popup.add(new RefreshAction(null));
            } else {
                popup.add(new NewAction());
                popup.add(new RemoveAction(selection, false));
                popup.add(new RefreshAction(selection));
            }
        }
        return popup;
    }

}

package jd.gui.swing.jdgui.components.premiumbar;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;

import jd.gui.swing.jdgui.BasicJDTable;
import jd.gui.swing.jdgui.views.settings.panels.accountmanager.AccountEntry;
import jd.gui.swing.jdgui.views.settings.panels.accountmanager.BuyAction;
import jd.gui.swing.jdgui.views.settings.panels.accountmanager.RefreshAction;

import org.appwork.swing.components.tooltips.ToolTipController;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtComponentRowHighlighter;
import org.appwork.swing.exttable.ExtTableHeaderRenderer;
import org.jdownloader.updatev2.gui.LAFOptions;

public class AccountListTable extends BasicJDTable<AccountEntry> {

    private static final long serialVersionUID = -2166408567306279016L;

    public AccountListTable(AccountListTableModel accountListTableModel) {
        super(accountListTableModel);

        ToolTipController.getInstance().unregister(this);
        this.setBackground(null);
        setOpaque(false);
        // this.setShowVerticalLines(false);
        // this.setShowGrid(false);
        // this.setShowHorizontalLines(false);
    }

    @Override
    protected void initAlternateRowHighlighter() {

    }

    protected ExtTableHeaderRenderer createDefaultHeaderRenderer(ExtColumn<AccountEntry> column) {
        ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(column, getTableHeader());

        setHeaderRendererColors(ret);
        return ret;
    }

    @Override
    protected void addSelectionHighlighter() {

    }

    public static void setHeaderRendererColors(ExtTableHeaderRenderer ret) {
        ret.setFocusBackground(new Color(255, 255, 255, 80));
        ret.setBackgroundC(new Color(255, 255, 255, 80));
        ret.setFocusForeground(LAFOptions.getInstance().getColorForTooltipForeground());
        ret.setForegroundC(LAFOptions.getInstance().getColorForTooltipForeground());
    }

    protected void initMouseOverRowHighlighter() {
        Color f = (LAFOptions.getInstance().getColorForTableMouseOverRowForeground());
        Color b = (LAFOptions.getInstance().getColorForTableMouseOverRowBackground());
        f = Color.black;
        this.getModel().addExtComponentRowHighlighter(new ExtComponentRowHighlighter<AccountEntry>(f, b, null) {
            public int getPriority() {
                return Integer.MAX_VALUE - 1;

            }

            @Override
            protected Color getBackground(Color current) {
                return super.getBackground(current);
            }

            @Override
            public boolean accept(ExtColumn<AccountEntry> column, AccountEntry value, boolean selected, boolean focus, int row) {
                return mouseOverRow == row;
            }

        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.swing.exttable.ExtTable#onShortcutDelete(java.util.java.util.List , java.awt.event.KeyEvent, boolean)
     */
    @Override
    protected boolean onShortcutDelete(java.util.List<AccountEntry> selectedObjects, KeyEvent evt, boolean direct) {
        // new RemoveAction(selectedObjects, direct).actionPerformed(null);
        return true;
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
                // popup.add(new NewAction());
                // popup.add(new RemoveAction(selection, false));
                popup.add(new BuyAction());
                popup.add(new RefreshAction(null));
            } else {
                // popup.add(new NewAction());
                // popup.add(new RemoveAction(selection, false));
                popup.add(new RefreshAction(selection));
            }
        }
        return popup;
    }

}

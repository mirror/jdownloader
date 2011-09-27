package jd.gui.swing.jdgui.views.settings.panels.packagizer;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import jd.gui.swing.jdgui.BasicJDTable;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.controlling.packagizer.PackagizerRule;

public class FilterTable extends BasicJDTable<PackagizerRule> {

    private static final long serialVersionUID = 4698030718806607175L;

    public FilterTable() {
        super(new FilterTableModel("FilterTable2"));
        this.setSearchEnabled(true);
        getTableHeader().setReorderingAllowed(false);

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.appwork.swing.exttable.ExtTable#onContextMenu(javax.swing.JPopupMenu
     * , java.lang.Object, java.util.ArrayList,
     * org.appwork.swing.exttable.ExtColumn)
     */
    @Override
    protected JPopupMenu onContextMenu(JPopupMenu popup, PackagizerRule contextObject, ArrayList<PackagizerRule> selection, ExtColumn<PackagizerRule> column) {
        popup.add(new JMenuItem(new NewAction(this).toContextMenuAction()));
        popup.add(new JMenuItem(new RemoveAction(this, selection, false).toContextMenuAction()));
        // popup.add(new JSeparator());
        // popup.add(new JMenuItem(new TestAction()));
        return popup;
    }

    @Override
    protected void onDoubleClick(MouseEvent e, PackagizerRule obj) {
        try {
            Dialog.getInstance().showDialog(new PackagizerFilterRuleDialog(obj));
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }
        getExtTableModel().fireTableDataChanged();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.appwork.swing.exttable.ExtTable#onShortcutDelete(java.util.ArrayList
     * , java.awt.event.KeyEvent, boolean)
     */
    @Override
    protected boolean onShortcutDelete(ArrayList<PackagizerRule> selectedObjects, KeyEvent evt, boolean direct) {
        new RemoveAction(this, selectedObjects, direct).actionPerformed(null);
        return true;
    }
}

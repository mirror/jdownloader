package jd.gui.swing.jdgui.views.settings.panels.packagizer;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.DropMode;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;

import jd.gui.swing.jdgui.BasicJDTable;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtTransferHandler;
import org.appwork.utils.Application;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.controlling.packagizer.PackagizerRule;

public class PackagizerFilterTable extends BasicJDTable<PackagizerRule> {

    private static final long serialVersionUID = 4698030718806607175L;

    public PackagizerFilterTable() {
        super(new FilterTableModel("PackagizerFilterTable"));
        this.setSearchEnabled(true);
        getTableHeader().setReorderingAllowed(false);
        this.setDragEnabled(true);

        setTransferHandler(new ExtTransferHandler<PackagizerRule>());
        if (Application.getJavaVersion() >= Application.JAVA16) setDropMode(DropMode.INSERT_ROWS);

        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.swing.exttable.ExtTable#onContextMenu(javax.swing.JPopupMenu , java.lang.Object, java.util.ArrayList,
     * org.appwork.swing.exttable.ExtColumn)
     */
    @Override
    protected JPopupMenu onContextMenu(JPopupMenu popup, PackagizerRule contextObject, java.util.List<PackagizerRule> selection, ExtColumn<PackagizerRule> column, MouseEvent ev) {
        popup.add(new JMenuItem(new NewAction(this)));
        popup.add(new JMenuItem(new RemoveAction(this, selection, false)));

        popup.add(new JMenuItem(new DuplicateAction(contextObject, this)));

        popup.addSeparator();
        popup.add(new ExportAction(selection));
        return popup;
    }

    @Override
    protected boolean onDoubleClick(MouseEvent e, PackagizerRule obj) {
        try {
            PackagizerFilterRuleDialog.showDialog(obj);

        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }
        getModel().fireTableDataChanged();
        PackagizerController.getInstance().update();
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.swing.exttable.ExtTable#onShortcutDelete(java.util.ArrayList , java.awt.event.KeyEvent, boolean)
     */
    @Override
    protected boolean onShortcutDelete(java.util.List<PackagizerRule> selectedObjects, KeyEvent evt, boolean direct) {
        new RemoveAction(this, selectedObjects, direct).actionPerformed(null);
        return true;
    }
}

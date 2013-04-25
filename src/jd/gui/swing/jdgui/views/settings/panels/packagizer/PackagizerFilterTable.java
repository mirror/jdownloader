package jd.gui.swing.jdgui.views.settings.panels.packagizer;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;

import javax.swing.DropMode;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;

import jd.gui.swing.jdgui.BasicJDTable;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtTransferHandler;
import org.appwork.utils.Application;
import org.appwork.utils.swing.dialog.Dialog;
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

        setRowHeight(24);

        addMouseMotionListener(new MouseMotionListener() {
            private DelayedRunnable delayer;
            private int             index;

            // {
            // delayer = new DelayedRunnable(IOEQ.TIMINGQUEUE, 20, 50) {
            //
            // @Override
            // public void delayedrun() {
            // new EDTRunner() {
            //
            // @Override
            // protected void runInEDT() {
            //
            // }
            // };
            //
            // }
            //
            // };
            // }

            public void mouseMoved(MouseEvent e) {
                index = getRowIndexByPoint(e.getPoint());
                getSelectionModel().setSelectionInterval(index, index);

            }

            public void mouseDragged(MouseEvent e) {
            }
        });

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
        popup.add(new DefaultRulesAction());
        popup.addSeparator();
        popup.add(new ExportAction(selection));
        return popup;
    }

    @Override
    protected boolean onDoubleClick(MouseEvent e, PackagizerRule obj) {
        try {
            Dialog.getInstance().showDialog(new PackagizerFilterRuleDialog(obj));
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }
        getExtTableModel().fireTableDataChanged();
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

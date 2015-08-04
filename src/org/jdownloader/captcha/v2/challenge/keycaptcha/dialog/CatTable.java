package org.jdownloader.captcha.v2.challenge.keycaptcha.dialog;

import java.awt.Cursor;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtTable;
import org.appwork.swing.exttable.ExtTableModel;

public class CatTable extends ExtTable<CatOption> {

    public CatTable(ExtTableModel<CatOption> model) {
        super(model);
        getTableHeader().setReorderingAllowed(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    protected void processMouseEvent(MouseEvent e) {
        if (e.getID() == MouseEvent.MOUSE_RELEASED) {
            final int row = this.rowAtPoint(e.getPoint());
            CatOption obj = this.getModel().getObjectbyRow(row);
            System.out.println(row);
            ExtColumn<CatOption> col = this.getExtColumnAtPoint(e.getPoint());
            if (col != null) {
                col.onSingleClick(e, obj);
                return;
            }
        }
        // super.processMouseEvent(e);

    }

    @Override
    protected void reconfigureColumnButton() {

    }

    @Override
    protected JPopupMenu columnControlMenu(ExtColumn<CatOption> extColumn) {
        return null;
    }
}

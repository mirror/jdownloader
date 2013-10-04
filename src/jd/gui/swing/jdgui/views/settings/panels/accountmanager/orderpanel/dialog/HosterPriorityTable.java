package jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel.dialog;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.DropMode;
import javax.swing.JPopupMenu;

import jd.gui.swing.jdgui.BasicJDTable;
import jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel.AccountInterface;
import jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel.GroupWrapper;

import org.appwork.swing.exttable.DropHighlighter;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtComponentRowHighlighter;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.tree.ExtTreeTableModel;
import org.jdownloader.updatev2.gui.LAFOptions;

public class HosterPriorityTable extends BasicJDTable<AccountInterface> {

    public HosterPriorityTable(ExtTableModel<AccountInterface> tableModel) {
        super(tableModel);
        this.addRowHighlighter(new DropHighlighter(null, new Color(27, 164, 191, 75)));

        this.setTransferHandler(new HosterPriorityTableTransferHandler(this));
        this.setDragEnabled(true);
        this.setDropMode(DropMode.ON_OR_INSERT_ROWS);

        this.getModel().addExtComponentRowHighlighter(new ExtComponentRowHighlighter<AccountInterface>(LAFOptions.getInstance().getColorForTablePackageRowForeground(), LAFOptions.getInstance().getColorForTablePackageRowBackground(), null) {

            @Override
            public boolean accept(ExtColumn<AccountInterface> column, AccountInterface value, boolean selected, boolean focus, int row) {
                return value instanceof GroupWrapper;
            }
        });

    }

    @Override
    protected JPopupMenu onContextMenu(JPopupMenu popup, AccountInterface contextObject, List<AccountInterface> selection, ExtColumn<AccountInterface> column, MouseEvent mouseEvent) {
        return super.onContextMenu(popup, contextObject, selection, column, mouseEvent);
    }

    @Override
    protected boolean onShortcutDelete(List<AccountInterface> selectedObjects, KeyEvent evt, boolean direct) {
        for (AccountInterface ai : selectedObjects) {
            if (ai instanceof GroupWrapper) {
                if (ai.getChildren().size() == 0) {
                    ((ExtTreeTableModel) getModel()).remove(ai);
                } else {
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        }
        return true;
    }
}

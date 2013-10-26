package org.jdownloader.gui.views.components.packagetable.actions;

import java.awt.event.ActionEvent;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtTable;
import org.appwork.utils.Application;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class SortPackagesDownloadOrdnerOnColumn extends AppAction {

    private ExtColumn<?> column;

    public SortPackagesDownloadOrdnerOnColumn(ExtColumn<?> column) {
        setName(_GUI._.SortPackagesDownloadOrdnerOnColumn(column.getName()));
        setIconKey("exttable/sort");
        this.column = column;
        setEnabled(Application.isJared(SortPackagesDownloadOrdnerOnColumn.class));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Dialog.getInstance().showMessageDialog("TODO JIAZ");
        if (column.isSortable(null)) {
            ExtTable<?> table = column.getModel().getTable();
            ExtColumn<?> oldColumn = table.getModel().getSortColumn();
            final String oldIdentifier = oldColumn == null ? null : oldColumn.getSortOrderIdentifier();
            column.doSort();

        }
    }

}

package org.jdownloader.gui.views.components.packagetable.actions;

import java.awt.event.ActionEvent;

import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.PackageController;
import jd.controlling.packagecontroller.PackageControllerComparator;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModel;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class SortPackagesDownloadOrdnerOnColumn extends AppAction {
    private final ExtColumn<?> column;

    public SortPackagesDownloadOrdnerOnColumn(ExtColumn<?> column) {
        setName(_GUI.T.SortPackagesDownloadOrdnerOnColumn(column.getName()));
        setIconKey(IconKey.ICON_EXTTABLE_SORT);
        this.column = column;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (column.isSortable(null)) {
            final ExtTableModel<?> model = column.getModel();
            final PackageController modelController = ((PackageControllerTableModel) model).getController();
            String currentID = column.getModel().getModelID() + ".Column." + column.getID();
            final PackageControllerComparator currentSorter = modelController.getSorter();
            final boolean asc;
            if (currentSorter == null || !currentSorter.getID().equals(currentID)) {
                if (CFG_GUI.CFG.isPrimaryTableSorterDesc()) {
                    asc = true;
                } else {
                    asc = false;
                }
            } else {
                asc = !currentSorter.isAsc();
            }
            currentID = (asc ? ExtColumn.SORT_ASC : ExtColumn.SORT_DESC) + "." + currentID;
            final PackageControllerComparator<? extends AbstractNode> comparator;
            comparator = PackageControllerComparator.getComparator(currentID);
            final boolean sortChildren;
            if (!CrossSystem.isMac() && ((e.getModifiers() & ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK)) {
                sortChildren = false;
            } else {
                sortChildren = true;
            }
            modelController.sort(comparator, sortChildren);
        }
    }
}

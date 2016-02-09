package org.jdownloader.gui.views.components.packagetable.actions;

import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtDefaultRowSorter;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModel;

import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.PackageController;
import jd.controlling.packagecontroller.PackageControllerComparator;

public class SortPackagesDownloadOrdnerOnColumn extends AppAction {

    private ExtColumn<?> column;

    public SortPackagesDownloadOrdnerOnColumn(ExtColumn<?> column) {
        setName(_GUI.T.SortPackagesDownloadOrdnerOnColumn(column.getName()));
        setIconKey(IconKey.ICON_EXTTABLE_SORT);
        this.column = column;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (column.isSortable(null)) {
            ExtTableModel<?> model = column.getModel();
            PackageController modelController = ((PackageControllerTableModel) model).getController();

            final ExtDefaultRowSorter<AbstractNode> sorter = (ExtDefaultRowSorter<AbstractNode>) column.getRowSorter();

            PackageControllerComparator currentComparator = modelController.getSorter();
            final String newID = column.getModel().getModelID() + ".Column." + column.getID();

            final AtomicBoolean asc = new AtomicBoolean(true);
            if (currentComparator != null && newID.equals(currentComparator.getID())) {
                asc.set(!currentComparator.isAsc());
            }
            final boolean sortPackages;
            if (!CrossSystem.isMac() && ((e.getModifiers() & ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK)) {
                sortPackages = false;
            } else {
                sortPackages = true;
            }
            modelController.sort(new PackageControllerComparator<AbstractNode>() {

                @Override
                public int compare(AbstractNode o1, AbstractNode o2) {
                    if (isAsc()) {
                        return sorter.compare(o1, o2);
                    } else {
                        return sorter.compare(o2, o1);
                    }
                }

                @Override
                public String getID() {
                    return newID;
                }

                @Override
                public boolean isAsc() {
                    return asc.get();
                }
            }, sortPackages);

        }
    }

}

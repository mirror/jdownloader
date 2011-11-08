package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Comparator;

import jd.controlling.IOEQ;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.utils.event.queue.Queue.QueuePriority;
import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModel;

public class SortAction extends AppAction {

    /**
     * 
     */
    private static final long       serialVersionUID = -3883739313644803093L;
    private ExtColumn<AbstractNode> column;
    private ArrayList<AbstractNode> selection        = null;
    private static String           sortOrder        = null;

    public SortAction(ArrayList<AbstractNode> selection, ExtColumn<AbstractNode> column) {
        setIconKey("sort");
        setName(_GUI._.SortAction_SortAction_object_(column.getName()));
        this.column = column;
        this.selection = selection;
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>(QueuePriority.HIGH) {

            @SuppressWarnings({ "rawtypes", "unchecked" })
            @Override
            protected Void run() throws RuntimeException {
                if (sortOrder == null || ExtColumn.SORT_DESC == sortOrder) {
                    sortOrder = ExtColumn.SORT_ASC;
                } else {
                    sortOrder = ExtColumn.SORT_DESC;
                }
                if (column.getModel() instanceof PackageControllerTableModel) {
                    PackageControllerTableModel model = (PackageControllerTableModel) column.getModel();
                    Comparator<AbstractNode> comparator = new Comparator<AbstractNode>() {

                        public int compare(AbstractNode o1, AbstractNode o2) {
                            if (ExtColumn.SORT_ASC == sortOrder) {
                                return column.getRowSorter().compare(o2, o1);
                            } else {
                                return column.getRowSorter().compare(o1, o2);
                            }
                        }
                    };
                    for (AbstractNode node : selection) {
                        if (node instanceof AbstractPackageNode) {
                            model.sortPackageChildren((AbstractPackageNode) node, comparator);
                        }
                    }
                }
                return null;
            }
        });
    }

    @Override
    public boolean isEnabled() {
        if (selection != null) {
            for (AbstractNode node : selection) {
                if (node instanceof AbstractPackageNode) return true;
            }
        }
        return false;
    }

}

package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.IOEQ;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.controlling.packagecontroller.ChildComparator;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.utils.event.queue.Queue.QueuePriority;
import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModel;

public class SortAction extends AppAction {

    /**
     * 
     */
    private static final long       serialVersionUID = -3883739313644803093L;
    private ExtColumn<AbstractNode> column;
    private ArrayList<AbstractNode> selection2;
    private AbstractNode            contextObject;

    public SortAction(AbstractNode contextObject, ArrayList<AbstractNode> selection2, ExtColumn<AbstractNode> column2) {
        this.column = column2;
        this.selection2 = selection2;
        this.contextObject = contextObject;
        setIconKey("sort");
        setName(_GUI._.SortAction_SortAction_object_(column.getName()));

    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>(QueuePriority.HIGH) {

            @SuppressWarnings({ "rawtypes", "unchecked" })
            @Override
            protected Void run() throws RuntimeException {
                ArrayList<AbstractPackageNode> selection = LinkTreeUtils.getPackages(contextObject, selection2, new ArrayList<AbstractPackageNode>());

                if (column.getModel() instanceof PackageControllerTableModel) {
                    PackageControllerTableModel model = (PackageControllerTableModel) column.getModel();

                    ChildComparator<AbstractNode> comparator = null;
                    for (AbstractNode node : selection) {
                        if (node instanceof AbstractPackageNode) {
                            if (comparator == null) {
                                if (((AbstractPackageNode) node).getCurrentSorter() == null || !((AbstractPackageNode) node).getCurrentSorter().getID().equals(column.getModel().getModelID() + ".Column." + column.getID()) || ((AbstractPackageNode) node).getCurrentSorter().isAsc()) {
                                    comparator = new ChildComparator<AbstractNode>() {

                                        public int compare(AbstractNode o1, AbstractNode o2) {

                                            return column.getRowSorter().compare(o2, o1);

                                        }

                                        @Override
                                        public String getID() {
                                            return column.getModel().getModelID() + ".Column." + column.getID();
                                        }

                                        @Override
                                        public boolean isAsc() {
                                            return false;
                                        }
                                    };

                                } else {
                                    comparator = new ChildComparator<AbstractNode>() {

                                        public int compare(AbstractNode o1, AbstractNode o2) {

                                            return column.getRowSorter().compare(o1, o2);

                                        }

                                        @Override
                                        public String getID() {
                                            return column.getModel().getModelID() + ".Column." + column.getID();
                                        }

                                        @Override
                                        public boolean isAsc() {
                                            return true;
                                        }
                                    };
                                }
                            }
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
        return contextObject != null && selection2 != null && selection2.size() > 0;
    }
}

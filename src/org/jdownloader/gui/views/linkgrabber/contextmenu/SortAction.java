package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;

import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.controlling.packagecontroller.PackageControllerComparator;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.utils.event.queue.Queue.QueuePriority;
import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.actions.AbstractSelectionContextAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModel;

public class SortAction<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends AbstractSelectionContextAction<PackageType, ChildrenType> {

    /**
     * 
     */
    private static final long       serialVersionUID = -3883739313644803093L;
    private ExtColumn<AbstractNode> column;

    @Override
    public void setSelection(SelectionInfo<PackageType, ChildrenType> selection) {
        super.setSelection(selection);
        if (getSelection() != null) {
            this.column = getSelection().getContextColumn();

            setIconKey("sort");
            setName(_GUI._.SortAction_SortAction_object_(column.getName()));
        } else {
            setIconKey("sort");
            setName(_GUI._.SortAction_SortAction_object_empty());
        }
        setItemVisibleForEmptySelection(true);
    }

    public SortAction(SelectionInfo<PackageType, ChildrenType> si) {
        super(si);

    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        if (column.getModel() instanceof PackageControllerTableModel) {
            PackageControllerTableModel model = (PackageControllerTableModel) column.getModel();
            model.getController().getQueue().add(new QueueAction<Void, RuntimeException>(QueuePriority.HIGH) {

                @SuppressWarnings({ "rawtypes", "unchecked" })
                @Override
                protected Void run() throws RuntimeException {

                    if (column.getModel() instanceof PackageControllerTableModel) {
                        PackageControllerTableModel model = (PackageControllerTableModel) column.getModel();
                        PackageControllerComparator<AbstractNode> comparator = null;
                        for (AbstractNode node : getSelection().getAllPackages()) {
                            if (node instanceof AbstractPackageNode) {
                                if (comparator == null) {
                                    if (((AbstractPackageNode) node).getCurrentSorter() == null || !((AbstractPackageNode) node).getCurrentSorter().getID().equals(column.getModel().getModelID() + ".Column." + column.getID()) || ((AbstractPackageNode) node).getCurrentSorter().isAsc()) {
                                        comparator = new PackageControllerComparator<AbstractNode>() {

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
                                        comparator = new PackageControllerComparator<AbstractNode>() {

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
    }

}

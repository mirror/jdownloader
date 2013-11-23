package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;

import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.controlling.packagecontroller.PackageControllerComparator;
import jd.gui.swing.jdgui.MainTabbedPane;
import jd.gui.swing.jdgui.interfaces.View;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.utils.event.queue.Queue.QueuePriority;
import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo.PackageView;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModel;
import org.jdownloader.gui.views.downloads.DownloadsView;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberView;

public class SortAction<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends CustomizableTableContextAppAction<PackageType, ChildrenType> {

    /**
     * 
     */
    private static final long       serialVersionUID = -3883739313644803093L;
    private ExtColumn<AbstractNode> column;

    @Override
    public void requestUpdate(Object requestor) {
        super.requestUpdate(requestor);

        View view = MainTabbedPane.getInstance().getSelectedView();

        if (view instanceof DownloadsView) {

            this.column = DownloadsTable.getInstance().getMouseOverColumn();
        } else if (view instanceof LinkGrabberView) {
            this.column = LinkGrabberTable.getInstance().getMouseOverColumn();
        }

        if (getSelection() != null) {

            setIconKey("sort");
            setName(_GUI._.SortAction_SortAction_object_(column.getName()));
        } else {
            setIconKey("sort");
            setName(_GUI._.SortAction_SortAction_object_empty());
        }

    }

    @Override
    public void setEnabled(boolean newValue) {
        super.setEnabled(true);
    }

    public SortAction() {
        super(true, true);
        setIconKey("sort");
        setName(_GUI._.SortAction_SortAction_object_empty());

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
                        for (PackageView<PackageType, ChildrenType> node : getSelection().getPackageViews()) {

                            if (comparator == null) {
                                if (node.getPackage().getCurrentSorter() == null || !node.getPackage().getCurrentSorter().getID().equals(column.getModel().getModelID() + ".Column." + column.getID()) || node.getPackage().getCurrentSorter().isAsc()) {
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
                    return null;
                }
            });
        }
    }

}

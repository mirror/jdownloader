package jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel.dialog;

import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.swing.JTable;

import jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel.AccountInterface;
import jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel.GroupWrapper;

import org.appwork.swing.exttable.ExtTransferHandler;
import org.appwork.swing.exttable.tree.ExtTreeTableModel;
import org.appwork.swing.exttable.tree.TreePosition;

public class HosterPriorityTableTransferHandler extends ExtTransferHandler<AccountInterface> {

    public HosterPriorityTableTransferHandler(HosterPriorityTable hosterPriorityTable) {

    }

    public HosterPriorityTableModel getModel() {
        return (HosterPriorityTableModel) getTable().getModel();
    }

    public HosterPriorityTable getTable() {
        return (HosterPriorityTable) super.getTable();
    }

    public boolean importData(final TransferSupport support) {
        if (!canImport(support)) { return false; }
        final JTable.DropLocation dl = (JTable.DropLocation) support.getDropLocation();
        try {
            List<AccountInterface> imports = (java.util.List<AccountInterface>) support.getTransferable().getTransferData(getTable().getDataFlavor());

            if (dl.isInsertRow()) {
                final int dropRow = dl.getRow();
                for (AccountInterface i : imports) {
                    if (i instanceof GroupWrapper) {
                        ArrayList<AccountInterface> transfer = new ArrayList<AccountInterface>();
                        transfer.add(i);
                        // ((GroupWrapper)i).getGroup()
                        TreePosition<AccountInterface> treePosition = getModel().getTreePositionByRow(dropRow);
                        if (treePosition.getParent() instanceof GroupWrapper) {
                            treePosition = getModel().getTreePositionByObject(treePosition.getParent());
                            return getModel().move(treePosition.getParent(), treePosition.getIndex() + 1, imports);
                        } else {
                            return getModel().move(treePosition.getParent(), treePosition.getIndex(), imports);
                        }
                    } else {
                        TreePosition<AccountInterface> treePosition = getModel().getTreePositionByRow(dropRow);
                        return getModel().move(treePosition.getParent(), treePosition.getIndex(), imports);
                    }
                }

                // return getTable().getModel().move(imports, dropRow);

            } else {
                int dropRow = dl.getRow();
                AccountInterface onElement = getTable().getModel().getObjectbyRow(dropRow);
                if (onElement instanceof GroupWrapper) {
                    GroupWrapper group = ((GroupWrapper) onElement);
                    List<AccountInterface> children = ExtTreeTableModel.getAllChildren(imports);
                    List<AccountInterface> parents = ExtTreeTableModel.getAllParents(imports);
                    getModel().removeAll(parents);
                    return getModel().move(group, -1, children);

                }

            }
        } catch (final UnsupportedFlavorException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean canImport(TransferSupport support) {
        try {
            if (!support.isDataFlavorSupported(getTable().getDataFlavor())) return false;
            JTable.DropLocation dl = (JTable.DropLocation) support.getDropLocation();
            boolean isInsert = dl.isInsertRow();
            int dropRow = dl.getRow();
            if (support.isDrop()) {
                List<AccountInterface> data = (java.util.List<AccountInterface>) support.getTransferable().getTransferData(getTable().getDataFlavor());
                boolean containsGroups = false;
                boolean containsAccounts = false;
                for (AccountInterface ai : data) {
                    if (ai instanceof GroupWrapper) {
                        containsGroups = true;

                    } else {
                        containsAccounts = true;
                    }
                }
                if (containsAccounts && containsGroups) return false;
                if (isInsert) {
                    /* handle insert,move here */
                    Object beforeElement = getTable().getModel().getObjectbyRow(dropRow - 1);
                    Object afterElement = getTable().getModel().getObjectbyRow(dropRow);

                    if (containsAccounts) {
                        if (beforeElement == null) {
                            /* no before element, table is empty */
                            return false;
                        }
                        if (beforeElement instanceof GroupWrapper && afterElement != null && afterElement instanceof GroupWrapper) {

                        return false; }
                        if (beforeElement instanceof GroupWrapper && afterElement == null) return false;
                        return true;

                    } else {
                        if (afterElement == null || afterElement instanceof GroupWrapper) return true;
                    }
                    return false;
                } else {
                    /* handle dropOn,merge here */
                    AccountInterface onElement = getTable().getModel().getObjectbyRow(dropRow);

                    if (onElement != null && onElement instanceof GroupWrapper && !new HashSet<AccountInterface>(data).contains(onElement)) {

                    return true; }

                }
            }
        } catch (UnsupportedFlavorException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}

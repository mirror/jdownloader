package org.jdownloader.gui.views.downloads.contextmenumanager.gui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.datatransfer.Transferable;

import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.plaf.TreeUI;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuItemData;

public class ExtTree extends JTree {
    private void expandAll(boolean expand) {
        Object root = getModel().getRoot();

        // Traverse tree from root
        expandAll(new TreePath(root), expand);
    }

    private void expandAll(TreePath parent, boolean expand) {
        // Traverse children
        MenuItemData node = (MenuItemData) parent.getLastPathComponent();
        if (node.getItems() != null) {
            for (MenuItemData mid : node.getItems()) {
                TreePath path = parent.pathByAddingChild(mid);
                expandAll(path, expand);
            }
        }

        if (expand) {
            expandPath(parent);
        } else {
            collapsePath(parent);
        }
    }

    private ManagerTreeModel model;

    public TreeUI getUI() {
        return (TreeUI) ui;
    }

    public ExtTree(ManagerTreeModel model) {
        super(model);
        this.model = model;
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        setOpaque(true);
        setExpandsSelectedPaths(true);
        addTreeWillExpandListener(new TreeWillExpandListener() {

            @Override
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
                throw new ExpandVetoException(event);
            }

        });
        expandAll(true);

        setCellRenderer(new Renderer());
        setRootVisible(false);
        setRowHeight(24);
        setDragEnabled(true);
        // setDropMode(DropMode.ON_OR_INSERT_ROWS);
        setDropMode(DropMode.INSERT);
        setTransferHandler(new TransferHandler() {
            public boolean canImport(TransferHandler.TransferSupport support) {

                if (!support.isDataFlavorSupported(MenuItemTransferAble.NODE_FLAVOR) || !support.isDrop()) { return false; }

                JTree.DropLocation dropLocation = (JTree.DropLocation) support.getDropLocation();

                return dropLocation.getPath() != null;
            }

            public int getSourceActions(JComponent c) {
                return TransferHandler.MOVE;
            }

            protected Transferable createTransferable(JComponent c) {
                //
                expandAll(true);
                return new MenuItemTransferAble(getSelectionPath());

            }

            protected void exportDone(JComponent c, Transferable t, int action) {
                if (action == TransferHandler.MOVE) {
                    // we need to remove items imported from the appropriate source.

                }
            }

            public boolean importData(TransferHandler.TransferSupport support) {
                if (!canImport(support)) { return false; }

                JTree.DropLocation dropLocation = (JTree.DropLocation) support.getDropLocation();

                TreePath path = dropLocation.getPath();

                Transferable transferable = support.getTransferable();
                try {

                    TreePath data = (TreePath) transferable.getTransferData(MenuItemTransferAble.NODE_FLAVOR);
                    int childIndex = dropLocation.getChildIndex();

                    ExtTree.this.model.moveTo(data, (MenuItemData) path.getLastPathComponent(), childIndex);
                    TreePath newPath = path.pathByAddingChild(data.getLastPathComponent());
                    setSelectionPath(newPath);
                    expandAll(true);

                } catch (Exception e) {
                    Dialog.getInstance().showExceptionDialog("Error", e.getMessage(), e);
                    e.printStackTrace();
                }

                return true;
            }
        });
    }

    protected TreeModelListener createTreeModelListener() {
        return new TreeModelHandler() {

            @Override
            public void treeStructureChanged(TreeModelEvent e) {
                super.treeStructureChanged(e);
                expandAll(true);
                if (getSelectionCount() <= 0) {
                    setSelectionRow(0);
                }
            }

        };
    }

    public void paint(Graphics g) {
        super.paint(g);

    }

    public void paintComponent(Graphics g) {

        super.paintComponent(g);
        Composite com = ((Graphics2D) g).getComposite();
        try {
            ((Graphics2D) g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.03f));
            for (int i = 0; i < getHeight() / 24; i++) {
                g.setColor(Color.BLACK);
                g.fillRect(0, i * 2 * 24, getWidth(), 24);
            }

        } finally {
            ((Graphics2D) g).setComposite(com);
        }

        // DropLocation dl = getDropLocation();
        // if (dl != null) {
        // if (dl.getChildIndex() >= 0) {
        // TreePath destPath = dl.getPath().pathByAddingChild(((MenuItemData)
        // dl.getPath().getLastPathComponent()).getItems().get(dl.getChildIndex()));
        // Rectangle pb = getUI().getPathBounds(this, destPath);
        // com = ((Graphics2D) g).getComposite();
        // try {
        // ((Graphics2D) g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        // g.fillRect(pb.x, pb.y - 5, 600, 5);
        // } finally {
        // ((Graphics2D) g).setComposite(com);
        // }
        // // getUI().getRowForPath(tree, path)
        // }
        //
        // }

    }

}

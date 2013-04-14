package org.jdownloader.gui.views.downloads.contextmenumanager.gui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.datatransfer.Transferable;

import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;

import org.appwork.app.gui.BasicGui;
import org.appwork.swing.MigPanel;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.contextmenumanager.DownloadListContextMenuManager;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuItemData;

public class ManagerFrame extends BasicGui {

    public ManagerFrame() {
        super(_GUI._.ManagerFrame_ManagerFrame_());
    }

    private static void expandAll(JTree tree, boolean expand) {
        Object root = tree.getModel().getRoot();

        // Traverse tree from root
        expandAll(tree, new TreePath(root), expand);
    }

    private static void expandAll(JTree tree, TreePath parent, boolean expand) {
        // Traverse children
        MenuItemData node = (MenuItemData) parent.getLastPathComponent();
        if (node.getItems() != null) {
            for (MenuItemData mid : node.getItems()) {
                TreePath path = parent.pathByAddingChild(mid);
                expandAll(tree, path, expand);
            }
        }

        if (expand) {
            tree.expandPath(parent);
        } else {
            tree.collapsePath(parent);
        }
    }

    @Override
    protected void layoutPanel() {
        MigPanel panel = new MigPanel("ins 0", "[grow,fill][]", "[grow,fill]");
        final ManagerTreeModel model = new ManagerTreeModel(new DownloadListContextMenuManager().getMenuData());
        final JTree tree = new JTree(model) {

            public void paintComponent(Graphics g) {

                super.paintComponent(g);

                ((Graphics2D) g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.05f));
                for (int i = 0; i < getHeight() / 24; i++) {
                    g.setColor(Color.BLACK);
                    g.fillRect(0, i * 2 * 24, getWidth(), 24);
                }
                if (getDropLocation() != null) System.out.println(getDropLocation().getChildIndex());

            }
        };
        tree.setExpandsSelectedPaths(true);
        tree.addTreeWillExpandListener(new TreeWillExpandListener() {

            @Override
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
                throw new ExpandVetoException(event);
            }

        });
        expandAll(tree, true);
        tree.putClientProperty("JTree.lineStyle", "Angled");
        tree.setCellRenderer(new Renderer());
        tree.setRootVisible(false);
        tree.setRowHeight(24);
        tree.setDragEnabled(true);
        // tree.setDropMode(DropMode.ON_OR_INSERT_ROWS);
        tree.setDropMode(DropMode.INSERT);
        tree.setTransferHandler(new TransferHandler() {
            public boolean canImport(TransferHandler.TransferSupport support) {

                if (!support.isDataFlavorSupported(MenuItemTransferAble.NODE_FLAVOR) || !support.isDrop()) { return false; }

                JTree.DropLocation dropLocation = (JTree.DropLocation) support.getDropLocation();

                return dropLocation.getPath() != null;
            }

            public int getSourceActions(JComponent c) {
                return TransferHandler.MOVE;
            }

            protected Transferable createTransferable(JComponent c) {
                // NODE_FLAVOR
                expandAll(tree, true);
                return new MenuItemTransferAble(tree.getSelectionPath());

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
                    System.out.println(dropLocation);
                    TreePath data = (TreePath) transferable.getTransferData(MenuItemTransferAble.NODE_FLAVOR);
                    int childIndex = dropLocation.getChildIndex();

                    model.moveTo(data, (MenuItemData) path.getLastPathComponent(), childIndex);
                    TreePath newPath = path.pathByAddingChild(data.getLastPathComponent());
                    tree.setSelectionPath(newPath);
                    expandAll(tree, true);
                    // if (childIndex == -1) {
                    // childIndex = tree.getModel().getChildCount(path.getLastPathComponent());
                    // }
                    System.out.println(childIndex);
                    // DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(transferData);
                    // DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
                    // model.insertNodeInto(newNode, parentNode, childIndex);
                    //
                    // TreePath newPath = path.pathByAddingChild(newNode);
                    // tree.makeVisible(newPath);
                    // tree.scrollRectToVisible(tree.getPathBounds(newPath));
                } catch (Exception e) {
                    Dialog.getInstance().showExceptionDialog("Error", e.getMessage(), e);
                    e.printStackTrace();
                }
                // String transferData;
                // try {
                // transferData = (String) transferable.getTransferData(DataFlavor.stringFlavor);
                // } catch (IOException e) {
                // return false;
                // } catch (UnsupportedFlavorException e) {
                // return false;
                // }
                //
                // int childIndex = dropLocation.getChildIndex();
                // if (childIndex == -1) {
                // childIndex = model.getChildCount(path.getLastPathComponent());
                // }
                //
                // DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(transferData);
                // DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
                // model.insertNodeInto(newNode, parentNode, childIndex);
                //
                // TreePath newPath = path.pathByAddingChild(newNode);
                // tree.makeVisible(newPath);
                // tree.scrollRectToVisible(tree.getPathBounds(newPath));

                return true;
            }
        });
        // tree.set
        // tree.setShowsRootHandles(false);
        panel.add(new JScrollPane(tree));
        getFrame().setContentPane(panel);
    }

    @Override
    protected void requestExit() {
    }
}

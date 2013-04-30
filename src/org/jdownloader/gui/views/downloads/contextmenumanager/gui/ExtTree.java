package org.jdownloader.gui.views.downloads.contextmenumanager.gui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.plaf.TreeUI;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuItemData;
import org.jdownloader.gui.views.downloads.contextmenumanager.SeperatorData;

public class ExtTree extends JTree {
    // private void expandAll(boolean expand) {
    // Object root = getModel().getRoot();
    //
    // // Traverse tree from root
    // expandAll(new TreePath(root), expand);
    // }
    //
    // private void expandAll(TreePath parent, boolean expand) {
    // // Traverse children
    // MenuItemData node = (MenuItemData) parent.getLastPathComponent();
    // if (node.getItems() != null) {
    // for (MenuItemData mid : node.getItems()) {
    // TreePath path = parent.pathByAddingChild(mid);
    // expandAll(path, expand);
    // }
    // }
    //
    // if (expand) {
    // expandPath(parent);
    // } else {
    // collapsePath(parent);
    // }
    // }

    private ManagerTreeModel model;
    private ManagerFrame     managerFrame;

    public TreeUI getUI() {
        return (TreeUI) ui;
    }

    public ExtTree(ManagerFrame mf) {
        super(mf.getModel());
        this.model = mf.getModel();
        model.setTree(this);
        this.managerFrame = mf;
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        setOpaque(true);
        // setExpandsSelectedPaths(true);
        // addTreeWillExpandListener(new TreeWillExpandListener() {
        //
        // @Override
        // public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
        // }
        //
        // @Override
        // public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
        // throw new ExpandVetoException(event);
        // }
        //
        // });
        // expandAll(true);

        setCellRenderer(new Renderer());
        setRootVisible(false);
        setRowHeight(20);
        addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    new RemoveAction(managerFrame).actionPerformed(null);
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {
            }
        });

        addMouseListener(new MouseListener() {

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {

                setSelectionRow(getRowForLocation(e.getX(), e.getY()));

                expandPath(getPathForLocation(e.getX(), e.getY()));
                if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {

                    JPopupMenu popup = new JPopupMenu();

                    // JMenuItem mi = new JMenuItem(T._.jd_plugins_optional_infobar_InfoDialog_hideWindow());
                    // mi.setIcon(NewTheme.I().getIcon("close", -1));
                    // mi.addActionListener(this);
                    popup.add(new AddActionAction(managerFrame));
                    popup.add(new AddSubMenuAction(managerFrame));
                    popup.add(new AddSpecialAction(managerFrame));
                    popup.add(new AddGenericItem(managerFrame, new SeperatorData()));
                    popup.add(new RemoveAction(managerFrame));
                    popup.show(ExtTree.this, e.getPoint().x, e.getPoint().y);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseClicked(MouseEvent e) {
            }
        });
        setDragEnabled(true);
        // setDropMode(DropMode.ON_OR_INSERT_ROWS);
        setDropMode(DropMode.INSERT);
        setTransferHandler(new TransferHandler() {
            public boolean canImport(TransferHandler.TransferSupport support) {

                if (!support.isDataFlavorSupported(MenuItemTransferAble.NODE_FLAVOR) || !support.isDrop()) { return false; }

                JTree.DropLocation dropLocation = (JTree.DropLocation) support.getDropLocation();
                Transferable transferable = support.getTransferable();
                try {
                    TreePath path = dropLocation.getPath();
                    TreePath data = (TreePath) transferable.getTransferData(MenuItemTransferAble.NODE_FLAVOR);
                    MenuItemData oldParent = (MenuItemData) data.getParentPath().getLastPathComponent();
                    MenuItemData item = ((MenuItemData) data.getLastPathComponent());
                    MenuItemData parent = (MenuItemData) path.getLastPathComponent();

                    if (data.isDescendant(path)) return false;
                    if (parent == item) return false;
                    int childIndex = dropLocation.getChildIndex();
                    if (item instanceof SeperatorData) {
                        if (childIndex == 0) return false;
                        if (childIndex == parent.getItems().size()) return false;
                        return true;
                    }
                    if (oldParent == parent) return true;
                    return !parent._getItemIdentifiers().contains(item._getIdentifier());

                } catch (Exception e) {

                }

                return false;
            }

            public int getSourceActions(JComponent c) {
                return TransferHandler.MOVE;
            }

            protected Transferable createTransferable(JComponent c) {
                //
                // expandAll(true);
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
                    // expandAll(true);

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
                // expandAll(true);
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
            for (int i = 0; i < getHeight() / getRowHeight(); i++) {
                g.setColor(Color.BLACK);
                g.fillRect(0, i * 2 * getRowHeight(), getWidth(), getRowHeight());
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

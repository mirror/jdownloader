package jd.gui.skins.simple.components.treetable;

import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.TransferHandler;
import javax.swing.tree.TreePath;

import jd.config.Property;
import jd.gui.skins.simple.components.HTMLTooltip;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.utils.JDLocale;
import jd.utils.JDSounds;
import jd.utils.JDTheme;

public class TreeTableTransferHandler extends TransferHandler {
    private static final long serialVersionUID = 2560352681437669412L;

    private TreePath[] draggingPathes;

    public boolean isDragging = false;
    private DownloadTreeTable treeTable;

    public TreeTableTransferHandler(DownloadTreeTable downloadTreeTable) {
        treeTable = downloadTreeTable;
    }

    public boolean canDrop(JComponent comp, DataFlavor[] transferFlavors) {
        if (draggingPathes == null || draggingPathes.length <= 0) { return false;
        // logger.info("KKK "+info.getDropLocation()+" -
        // "+((JTable.DropLocation)info.getDropLocation()).getRow());
        }

        int row = treeTable.rowAtPoint(treeTable.getMousePosition());

        TreePath current = treeTable.getPathForRow(row);

        for (TreePath path : draggingPathes) {
            if (path.getLastPathComponent() == current.getLastPathComponent()) { return false; }
        }
        if (draggingPathes[0].getLastPathComponent() instanceof FilePackage) {
            if (current.getLastPathComponent() instanceof FilePackage) { return true; }
            return false;
        } else {
            return true;
        }

    }

    @Override
    public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
        return true;

    }

    @Override
    public boolean canImport(TreeTableTransferHandler.TransferSupport info) {

        if (draggingPathes == null || draggingPathes.length <= 0) { return false; }
        // logger.info("KKK "+info.getDropLocation()+" -
        // "+((JTable.DropLocation)info.getDropLocation()).getRow());
        int row = ((JTable.DropLocation) info.getDropLocation()).getRow();

        TreePath current = treeTable.getPathForRow(row);
        if (current == null) { return false; }
        for (TreePath path : draggingPathes) {
            if (path.getLastPathComponent() == current.getLastPathComponent()) { return false; }
        }

        if (draggingPathes[0].getLastPathComponent() instanceof FilePackage) {
            if (current.getLastPathComponent() instanceof FilePackage) { return true; }
            return false;
        } else {
            return true;
        }

    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        isDragging = true;

        int[] rows = treeTable.getSelectedRows();

        Vector<TreePath> packages = new Vector<TreePath>();
        Vector<TreePath> downloadLinks = new Vector<TreePath>();

        for (int element : rows) {
            if (treeTable.getPathForRow(element).getLastPathComponent() instanceof DownloadLink) {
                downloadLinks.add(treeTable.getPathForRow(element));
            } else {
                packages.add(treeTable.getPathForRow(element));
            }

            // draggingPathes[i]=getPathForRow(rows[i]);
        }
        if (downloadLinks.size() > packages.size()) {
            draggingPathes = downloadLinks.toArray(new TreePath[] {});
        } else {
            draggingPathes = packages.toArray(new TreePath[] {});
        }

        return new StringSelection("AFFE");
    }

    private boolean drop(int row, Point point) {
        try {
            isDragging = false;

            final TreePath current = treeTable.getPathForRow(row);
            final TreePath pre = treeTable.getPathForRow(row - 1);
            final TreePath post = treeTable.getPathForRow(row + 1);
            if (current == null) { return false; }
            JPopupMenu popup = new JPopupMenu();

            JMenuItem m;

            if (current.getLastPathComponent() instanceof DownloadLink) {
                final Object preLink = pre == null ? null : pre.getLastPathComponent() instanceof DownloadLink ? pre.getLastPathComponent() : null;
                final Object postLink = post == null ? null : post.getLastPathComponent() instanceof DownloadLink ? post.getLastPathComponent() : null;
                popup.add(m = new JMenuItem(String.format(JDLocale.L("gui.table.draganddrop.before", "Vor '%s' ablegen"), (treeTable.getDownladTreeTableModel().getValueAt(current.getLastPathComponent(), 0) + "").trim())));
                m.setIcon(JDTheme.II("gui.images.go_top", 16, 16));
                m.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {

                        wishSound(treeTable.getDownladTreeTableModel().move(draggingPathes, preLink, current.getLastPathComponent()));

                    }

                });
                popup.add(m = new JMenuItem(String.format(JDLocale.L("gui.table.draganddrop.after", "Nach '%s' ablegen"), (treeTable.getDownladTreeTableModel().getValueAt(current.getLastPathComponent(), 0) + "").trim())));
                m.setIcon(JDTheme.II("gui.images.go_bottom", 16, 16));
                m.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {

                        wishSound(treeTable.getDownladTreeTableModel().move(draggingPathes, current.getLastPathComponent(), postLink));

                    }

                });

                if (post != null && post.getLastPathComponent() == draggingPathes[0].getLastPathComponent()) {

                return wishSound(treeTable.getDownladTreeTableModel().move(draggingPathes, preLink, current.getLastPathComponent()));

                }

                if (pre != null && pre.getLastPathComponent() == draggingPathes[draggingPathes.length - 1].getLastPathComponent()) {

                return wishSound(treeTable.getDownladTreeTableModel().move(draggingPathes, current.getLastPathComponent(), postLink));

                }

            } else {
                if (draggingPathes[0].getLastPathComponent() instanceof DownloadLink) {
                    popup.add(m = new JMenuItem(String.format(JDLocale.L("gui.table.draganddrop.insertinpackagestart", "In Paket '%s' am Anfang einfügen"), (treeTable.getDownladTreeTableModel().getValueAt(current.getLastPathComponent(), 0) + "").trim())));
                    m.setIcon(JDTheme.II("gui.images.go_top", 16, 16));
                    m.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent e) {

                            wishSound(treeTable.getDownladTreeTableModel().moveToPackage(draggingPathes, (FilePackage) current.getLastPathComponent(), true));

                        }

                    });
                    popup.add(m = new JMenuItem(String.format(JDLocale.L("gui.table.draganddrop.insertinpackageend", "In Paket '%s' am Ende einfügen"), (treeTable.getDownladTreeTableModel().getValueAt(current.getLastPathComponent(), 0) + "").trim())));
                    m.setIcon(JDTheme.II("gui.images.go_bottom", 16, 16));
                    m.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent e) {

                            wishSound(treeTable.getDownladTreeTableModel().moveToPackage(draggingPathes, (FilePackage) current.getLastPathComponent(), false));

                        }

                    });
                } else {

                    popup.add(m = new JMenuItem(String.format(JDLocale.L("gui.table.draganddrop.movepackagebefore", "Vor Paket '%s' einfügen"), (treeTable.getDownladTreeTableModel().getValueAt(current.getLastPathComponent(), 0) + "").trim())));
                    m.setIcon(JDTheme.II("gui.images.go_top", 16, 16));
                    m.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent e) {

                            wishSound(treeTable.getDownladTreeTableModel().move(draggingPathes, null, current.getLastPathComponent()));

                        }

                    });
                    popup.add(m = new JMenuItem(String.format(JDLocale.L("gui.table.draganddrop.movepackageend", "Nach Paket '%s' einfügen"), (treeTable.getDownladTreeTableModel().getValueAt(current.getLastPathComponent(), 0) + "").trim())));
                    m.setIcon(JDTheme.II("gui.images.go_bottom", 16, 16));
                    m.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent e) {

                            wishSound(treeTable.getDownladTreeTableModel().move(draggingPathes, current.getLastPathComponent(), null));

                        }

                    });

                    if (post != null && post.getLastPathComponent() == draggingPathes[0].getLastPathComponent()) { return wishSound(treeTable.getDownladTreeTableModel().move(draggingPathes, null, current.getLastPathComponent()));

                    }
                    if (pre != null && pre.getLastPathComponent() == draggingPathes[draggingPathes.length - 1].getLastPathComponent()) { return wishSound(treeTable.getDownladTreeTableModel().move(draggingPathes, current.getLastPathComponent(), null));

                    }

                    // treeTable.getDownladTreeTableModel().move(draggingPathes,
                    // before, after)
                }

            }
            popup.add(m = new JMenuItem(JDLocale.L("gui.table.draganddrop.cancel", "Abbrechen")));
            m.setIcon(JDTheme.II("gui.images.unselected", 16, 16));

            popup.show(treeTable, point.x, point.y);
            for (TreePath path : draggingPathes) {
                ((Property) path.getLastPathComponent()).setProperty(DownloadTreeTable.PROPERTY_SELECTED, true);
            }
            treeTable.updateSelectionAndExpandStatus();
            // treeTable.updateSelectionAndExpandStatus();
            return true;
        } catch (Exception e) {

            e.printStackTrace();
        }
        return false;
    }

    @Override
    public int getSourceActions(JComponent c) {

        return MOVE;
    }

    @Override
    public boolean importData(JComponent comp, Transferable t) {
        isDragging = false;
        if (!canDrop(null, null)) {
            Point p = treeTable.getMousePosition();
            p.x += treeTable.getLocationOnScreen().x;
            p.y += treeTable.getLocationOnScreen().y;
            HTMLTooltip.show("<div>Cannot drop here<div>", p);
            return false;
        }

        int row = treeTable.rowAtPoint(treeTable.getMousePosition());

        return drop(row, treeTable.getMousePosition());

    }

    @Override
    public boolean importData(TreeTableTransferHandler.TransferSupport info) {

        int row = ((JTable.DropLocation) info.getDropLocation()).getRow();
        Point p = ((JTable.DropLocation) info.getDropLocation()).getDropPoint();

        return drop(row, p);
    }

    private boolean wishSound(boolean doit) {
        if (doit) {
            JDSounds.PT("sound.gui.onDragAndDrop");
        }

        return doit;
    }
}
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
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

public class TreeTableTransferHandler extends TransferHandler {
    private TreePath[] draggingPathes;

    private static final long serialVersionUID = 2560352681437669412L;

    private DownloadTreeTable treeTable;
    public boolean isDragging = false;

    public TreeTableTransferHandler(DownloadTreeTable downloadTreeTable) {
        this.treeTable = downloadTreeTable;
    }

    public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
        return true;

    }

    public boolean canDrop(JComponent comp, DataFlavor[] transferFlavors) {
        if (draggingPathes == null || draggingPathes.length <= 0) return false;
        // logger.info("KKK "+info.getDropLocation()+" -
        // "+((JTable.DropLocation)info.getDropLocation()).getRow());

        int row = treeTable.rowAtPoint(treeTable.getMousePosition());

        TreePath current = treeTable.getPathForRow(row);
        if (draggingPathes[0].getLastPathComponent() instanceof FilePackage) {
            if (current.getLastPathComponent() instanceof FilePackage) return true;
            return false;
        } else {
            return true;
        }

    }

    public boolean importData(JComponent comp, Transferable t) {
        // isDragging = false;
        // if (!canDrop(null, null)) {
        // Point p = treeTable.getMousePosition();
        // p.x += treeTable.getLocationOnScreen().x;
        // p.y += treeTable.getLocationOnScreen().y;
        // HTMLTooltip.show("<div>Cannot drop here<div>", p);
        // return false;
        // }
        //
        // int row = treeTable.rowAtPoint(treeTable.getMousePosition());
        // TreePath destPre = treeTable.getPathForRow(row);
        // Object dest = null;
        // if (destPre != null) {
        // dest = destPre.getLastPathComponent();
        // } else {
        // return false;
        // }
        //
        // JPopupMenu popup = new JPopupMenu();
        //
        // popup.add(new
        // JMenuItem(String.format(JDLocale.L("gui.table.draganddrop.before",
        // "Vor %s ablegen"),
        // treeTable.getDownladTreeTableModel().getValueAt(dest, 0) + "")));
        //
        // popup.show(treeTable, treeTable.getMousePosition2().x,
        // treeTable.getMousePosition2().y);
        //
        // treeTable.getDownladTreeTableModel().move(draggingPathes, dest);
        // draggingPathes = new TreePath[0];

        return true;

    }

    public boolean canImport(TreeTableTransferHandler.TransferSupport info) {

        if (draggingPathes == null || draggingPathes.length <= 0) return false;
        // logger.info("KKK "+info.getDropLocation()+" -
        // "+((JTable.DropLocation)info.getDropLocation()).getRow());
        int row = ((JTable.DropLocation) info.getDropLocation()).getRow();

        TreePath current = treeTable.getPathForRow(row);
        if (draggingPathes[0].getLastPathComponent() instanceof FilePackage) {
            if (current.getLastPathComponent() instanceof FilePackage) return true;
            return false;
        } else {
            return true;
        }

    }

    public boolean importData(TreeTableTransferHandler.TransferSupport info) {
        try{
        isDragging = false;
        int row = ((JTable.DropLocation) info.getDropLocation()).getRow();
        final TreePath current = treeTable.getPathForRow(row);
        final TreePath pre = treeTable.getPathForRow(row - 1);
        final TreePath post = treeTable.getPathForRow(row + 1);
        if (current == null) return false;
        JPopupMenu popup = new JPopupMenu();

        JMenuItem m;

        if (current.getLastPathComponent() instanceof DownloadLink) {
            final Object preLink = pre == null ? null : (pre.getLastPathComponent() instanceof DownloadLink) ? pre.getLastPathComponent() : null;
            final Object postLink = post == null ? null : (post.getLastPathComponent() instanceof DownloadLink) ? post.getLastPathComponent() : null;
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
          
            if (post!=null&&post.getLastPathComponent() == this.draggingPathes[0].getLastPathComponent()) {
            
                return wishSound(treeTable.getDownladTreeTableModel().move(draggingPathes, preLink, current.getLastPathComponent()));

            }
          
            if (pre!=null &&pre.getLastPathComponent() == this.draggingPathes[draggingPathes.length - 1].getLastPathComponent()) {
            
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
                    
                        wishSound(treeTable.getDownladTreeTableModel().move(draggingPathes, null, (FilePackage) current.getLastPathComponent()));

                    }

                });
                popup.add(m = new JMenuItem(String.format(JDLocale.L("gui.table.draganddrop.movepackageend", "Nach Paket '%s' einfügen"), (treeTable.getDownladTreeTableModel().getValueAt(current.getLastPathComponent(), 0) + "").trim())));
                m.setIcon(JDTheme.II("gui.images.go_bottom", 16, 16));
                m.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                    
                        wishSound(treeTable.getDownladTreeTableModel().move(draggingPathes, (FilePackage) current.getLastPathComponent(), null));

                    }

                });

                if (post!=null &&post.getLastPathComponent() == this.draggingPathes[0].getLastPathComponent()) {
                
                   return wishSound(treeTable.getDownladTreeTableModel().move(draggingPathes, (FilePackage) current.getLastPathComponent(), null));

                }
                if (pre !=null &&pre.getLastPathComponent() == this.draggingPathes[draggingPathes.length - 1].getLastPathComponent()) {
                    return wishSound(treeTable.getDownladTreeTableModel().move(draggingPathes, null, (FilePackage) current.getLastPathComponent()));

                }

                // treeTable.getDownladTreeTableModel().move(draggingPathes,
                // before, after)
            }

        }
        popup.add(m = new JMenuItem(JDLocale.L("gui.table.draganddrop.cancel", "Abbrechen")));
        m.setIcon(JDTheme.II("gui.images.unselected", 16, 16));
        Point p = ((JTable.DropLocation) info.getDropLocation()).getDropPoint();
        popup.show(treeTable, p.x, p.y);
        for (TreePath path : draggingPathes) {
            ((Property) path.getLastPathComponent()).setProperty(DownloadTreeTable.PROPERTY_SELECTED, true);
        }
        treeTable.updateSelectionAndExpandStatus();
        // treeTable.updateSelectionAndExpandStatus();
        return true;
        }catch(Exception e){
            
            e.printStackTrace();
        }
        return false;
    }
private boolean wishSound(boolean doit){
    if(doit)new Thread(){ public void run(){   JDUtilities.playMp3(JDUtilities.getResourceFile("snd/wish.mp3"));}}.start();
    return doit;
}
    public int getSourceActions(JComponent c) {

        return this.MOVE;
    }

    protected Transferable createTransferable(JComponent c) {

        int[] rows = treeTable.getSelectedRows();
        isDragging = true;
        Vector<TreePath> packages = new Vector<TreePath>();
        Vector<TreePath> downloadLinks = new Vector<TreePath>();

        for (int i = 0; i < rows.length; i++) {
            if (treeTable.getPathForRow(rows[i]).getLastPathComponent() instanceof DownloadLink) {
                downloadLinks.add(treeTable.getPathForRow(rows[i]));
            } else {
                packages.add(treeTable.getPathForRow(rows[i]));
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
}
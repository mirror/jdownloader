package jd.gui.skins.simple.components.treetable;

import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;
import javax.swing.tree.TreePath;

import jd.plugins.DownloadLink;

public class TreeTableTransferHandler extends TransferHandler {
    private TreePath[]        draggingPathes;

    private static final long serialVersionUID = 2560352681437669412L;

    private DownloadTreeTable treeTable;

    public TreeTableTransferHandler(DownloadTreeTable downloadTreeTable) {
        this.treeTable = downloadTreeTable;
    }

    public boolean canImport(TreeTableTransferHandler.TransferSupport info) {
        if (draggingPathes == null || draggingPathes.length <= 0) return false;
        // logger.info("KKK "+info.getDropLocation()+" -
        // "+((JTable.DropLocation)info.getDropLocation()).getRow());
        int row = ((JTable.DropLocation) info.getDropLocation()).getRow();
        TreePath destPre = treeTable.getPathForRow(row - 1);
        TreePath destPost = treeTable.getPathForRow(row);
        TreePath src = draggingPathes[0];
        TreePath lastSrc=draggingPathes[draggingPathes.length-1];
        
        if(destPre!=null){
            if(destPre.equals(src)||destPre.equals(lastSrc))return false;
            
        }
        if(destPost!=null){
            if(destPost.equals(src)||destPost.equals(lastSrc))return false;
            
        }
        // logger.info(dest+" - "+src);

        if (src.getLastPathComponent() instanceof DownloadLink) {
            if (destPre == null && destPost == null) return true;
            if (destPre == null) destPre = destPost;
            if (destPost == null) destPost = destPre;
            return (destPre.getLastPathComponent() instanceof DownloadLink || destPost.getLastPathComponent() instanceof DownloadLink);
        }
        if (destPre == null && destPost == null) return true;
        if (destPre == null) destPre = destPost;
        if (destPost == null) destPost = destPre;
        if (destPre.getLastPathComponent().getClass() != src.getLastPathComponent().getClass()) {
            return false;
        }

        return true;
    }

    public boolean importData(TreeTableTransferHandler.TransferSupport info) {
        int row = ((JTable.DropLocation) info.getDropLocation()).getRow();
        TreePath destPre = treeTable.getPathForRow(row);
        Object dest = null;
        if (destPre != null) dest = destPre.getLastPathComponent();
        treeTable.getDownladTreeTableModel().move(draggingPathes, dest);
        draggingPathes = new TreePath[0];
      
       // treeTable.updateSelectionAndExpandStatus();
        return true;
    }

    public int getSourceActions(JComponent c) {
      
        return COPY_OR_MOVE;
    }

    protected Transferable createTransferable(JComponent c) {

        int[] rows = treeTable.getSelectedRows();

        Vector<TreePath> packages = new Vector<TreePath>();
        Vector<TreePath> downloadLinks = new Vector<TreePath>();

        for (int i = 0; i < rows.length; i++) {
            if (treeTable.getPathForRow(rows[i]).getLastPathComponent() instanceof DownloadLink) {
                downloadLinks.add(treeTable.getPathForRow(rows[i]));
            }
            else {
                packages.add(treeTable.getPathForRow(rows[i]));
            }

            // draggingPathes[i]=getPathForRow(rows[i]);
        }
        if (downloadLinks.size() > packages.size()) {
            draggingPathes = downloadLinks.toArray(new TreePath[] {});
        }
        else {
            draggingPathes = packages.toArray(new TreePath[] {});
        }

        return new StringSelection("AFFE");
    }
}
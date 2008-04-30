/**
 * 
 */
package jd.gui.skins.simple.components.treetable;

import java.util.Vector;

import javax.swing.tree.TreePath;

import jd.gui.skins.simple.DownloadLinksTreeTablePanel;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

import org.jdesktop.swingx.tree.TreeModelSupport;
import org.jdesktop.swingx.treetable.AbstractTreeTableModel;

public class DownloadTreeTableModel extends AbstractTreeTableModel {

    /** table column names */
    static protected String[] COLUMN_NAMES = { JDLocale.L("gui.treetable.header_1.tree", "Pakete"), JDLocale.L("gui.treetable.header_2.files", "Dateien"), JDLocale.L("gui.treetable.header_3.hoster", "Anbieter"), JDLocale.L("gui.treetable.header_4.status", "Status"), JDLocale.L("gui.treetable.header_5.progress", "Fortschritt") };

    private DownloadLinksTreeTablePanel owner;

    /** index of tree column */

    public static final int COL_PART = 0;

    public static final int COL_FILE = 1;

    public static final int COL_HOSTER = 2;

    public static final int COL_STATUS = 3;

    public static final int COL_PROGRESS = 4;

    /**
     * Creates a {@link ProjectsTreeTableModel}
     * 
     * @param allLinks
     * @param downloadLinksTreeTable
     * 
     * @param aList
     *            the ProjectList to start out with.
     */
    public DownloadTreeTableModel(DownloadLinksTreeTablePanel treeTable) {
        super("root");

        this.owner = treeTable;
    }

    public TreeModelSupport getModelSupporter() {
        return modelSupport;
    }

    /**
     * How many columns do we display
     */
    public int getColumnCount() {

        return COLUMN_NAMES.length;
    }

    public Class<?> getColumnClass(int column) {
        switch (column) {
        case COL_PART:
            return String.class;

        case COL_FILE:
            return String.class;

        case COL_HOSTER:
            return String.class;

        case COL_STATUS:
            return String.class;

        case COL_PROGRESS:
            return Object.class;

        }
        return Object.class;
    }

    @Override
    public String getColumnName(int column) {

        return COLUMN_NAMES[column];
    }

    public void setValueAt(Object value, Object node, int col) {
        JDUtilities.getLogger().info("NNNN");
    }

    public boolean isCellEditable(Object node, int column) {
        return true;
    }

    public boolean move(TreePath[] from, Object before, Object after) {

        if (from.length == 0) return false;
        if (from[0].getLastPathComponent() instanceof DownloadLink) { return moveDownloadLinks(from, (DownloadLink) before, (DownloadLink) after); }
        return movePackages(from, (FilePackage)before, (FilePackage)after);

    }

    public boolean moveToPackage(TreePath[] from, FilePackage filePackage, boolean position) {
        if (!(from[0].getLastPathComponent() instanceof DownloadLink)) return false;
        Vector<DownloadLink> links = new Vector<DownloadLink>();

        for (TreePath path : from) {
            links.add((DownloadLink) path.getLastPathComponent());
        }
        if(filePackage.size()==0)return false;
    
        if (position) {
            return JDUtilities.getController().moveLinks(links, null, filePackage.get(0));
        } else {
            return JDUtilities.getController().moveLinks(links, filePackage.lastElement(), null);
        }

    }

    private boolean movePackages(TreePath[] from, FilePackage before,FilePackage after) {
        Vector<FilePackage> fps = new Vector<FilePackage>();
        for (TreePath path : from) {
            fps.add((FilePackage) path.getLastPathComponent());
        }
        if (after != null && !(after instanceof FilePackage)) return false;
        return JDUtilities.getController().movePackages(fps, before,after);

    }

    private boolean moveDownloadLinks(TreePath[] from, DownloadLink before, DownloadLink after) {

        Vector<DownloadLink> links = new Vector<DownloadLink>();

        for (TreePath path : from) {
            links.add((DownloadLink) path.getLastPathComponent());
        }

        return JDUtilities.getController().moveLinks(links, before, after);

    }

    /**
     * What is shown in a cell column for a node.
     */
    public Object getValueAt(Object node, int column) {
        String value = null;

        if (node instanceof DownloadLink) {
            DownloadLink downloadLink = (DownloadLink) node;
            switch (column) {
            case COL_PART:
                if (downloadLink.getLinkType() == DownloadLink.LINKTYPE_JDU) {
                    value = JDLocale.L("gui.treetable.part.label_update", "Update ") + "->" + downloadLink.getSourcePluginComment().split("_")[1];
                } else {
                    int id = downloadLink.getPartByName();
                    value = JDLocale.L("gui.treetable.part.label", "Datei ") + (id < 0 ? "" : JDUtilities.fillInteger(id, 3, "0")) + "  ";
                }
                break;
            case COL_FILE:

                value = downloadLink.getName();
                if (downloadLink.getLinkType() == DownloadLink.LINKTYPE_JDU) {
                    value = value.substring(0, value.length() - 4);
                }
                break;

            case COL_HOSTER:
                value = downloadLink.getHost();
                break;
            case COL_STATUS:
                value = downloadLink.getStatusText();
                break;
            case COL_PROGRESS:
                return downloadLink;

            }
        } else if (node instanceof FilePackage) {
            FilePackage filePackage = (FilePackage) node;
            switch (column) {
            case COL_PART:
                value = JDLocale.L("gui.treetable.part.label_package", "Paket ") + filePackage.getName();
                // value="";
                break;
            case COL_FILE:
                value = filePackage.getDownloadLinks().size() + " " + JDLocale.L("gui.treetable.parts", "Teile");
                break;

            case COL_HOSTER:
                value = "";
                break;
            case COL_STATUS:
                value = "";

                if (filePackage.getLinksInProgress() > 0) {
                    value = filePackage.getLinksInProgress() + "/" + filePackage.size() + " " + JDLocale.L("gui.treetable.packagestatus.links_active", "aktiv");
                }
                if (filePackage.getTotalDownloadSpeed() > 0) value = "[" + filePackage.getLinksInProgress() + "/" + filePackage.size() + "] " + "ETA " + JDUtilities.formatSeconds(filePackage.getETA()) + " @ " + JDUtilities.formatKbReadable(filePackage.getTotalDownloadSpeed() / 1024) + "/s";
                break;

            case COL_PROGRESS:
                return filePackage;
            }
        } else if (node instanceof String) {
            value = (0 == column) ? node.toString() : "";
        } else {
            System.out.println("node.class: " + node.getClass());
        }

        return value;
    }

    /**
     * Returns the child of <code>parent</code> at index <code>index</code>
     * in the parent's child array. <code>parent</code> must be a node
     * previously obtained from this data source. This should not return
     * <code>null</code> if <code>index</code> is a valid index for
     * <code>parent</code> (that is <code>index >= 0 &&
     * index < getChildCount(parent</code>)).
     * 
     * @param parent
     *            a node in the tree, obtained from this data source
     * @return the child of <code>parent</code> at index <code>index</code>
     * 
     * Have to implement this:
     */
    public Vector<FilePackage> getPackages() {
        return owner.getPackages();
    }

    public boolean containesPackage(FilePackage fp) {
        return owner.getPackages().contains(fp);
    }

    public Object getChild(Object parent, int index) {
        Object child = null;

        if (parent instanceof String) {
            child = getPackages().get(index);
        } else if (parent instanceof FilePackage) {
            FilePackage pack = (FilePackage) parent;
            child = pack.getDownloadLinks().get(index);
        } else if (parent instanceof DownloadLink) {
            // for now, DownloadLinks do not have Children
        }

        return child;
    }

    /**
     * Returns the number of children of <code>parent</code>. Returns 0 if
     * the node is a leaf or if it has no children. <code>parent</code> must
     * be a node previously obtained from this data source.
     * 
     * @param parent
     *            a node in the tree, obtained from this data source
     * @return the number of children of the node <code>parent</code>
     */
    public int getChildCount(Object parent) {
        int count = 0;

        if (parent instanceof String) {
            count = getPackages().size();
        } else if (parent instanceof FilePackage) {
            FilePackage pack = (FilePackage) parent;
            count = pack.getDownloadLinks().size();
        } else if (parent instanceof DownloadLink) {
            count = 0;
        }

        return count;
    }

    public int getIndexOfChild(Object parent, Object child) {
        int index = -1;
        if (parent instanceof String) {
            index = getPackages().indexOf(child);
        } else if (parent instanceof FilePackage) {
            index = ((FilePackage) parent).getDownloadLinks().indexOf(child);
        } else if (parent instanceof DownloadLink) {
            index = -1;
        }

        return index;
    }

}
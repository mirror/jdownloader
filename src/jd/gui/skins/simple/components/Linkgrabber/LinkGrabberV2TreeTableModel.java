package jd.gui.skins.simple.components.Linkgrabber;

import java.util.Vector;

import jd.gui.skins.simple.components.ComboBrowseFile;
import jd.plugins.DownloadLink;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

import org.jdesktop.swingx.tree.TreeModelSupport;
import org.jdesktop.swingx.treetable.AbstractTreeTableModel;

public class LinkGrabberV2TreeTableModel extends AbstractTreeTableModel {

    public static final int COL_FILE = 0;

    public static final int COL_SIZE = 1;

    public static final int COL_HOSTER = 2;

    public static final int COL_STATUS = 3;

    public static final int COL_DLFOLDER = 4;

    /** table column names */
    static protected String[] COLUMN_NAMES = { JDLocale.L("gui.linkgrabber.header.treeandfiles", "Packages/Files"), JDLocale.L("gui.linkgrabber.header.size", "Size"), JDLocale.L("gui.linkgrabber.header.hoster", "Anbieter"), JDLocale.L("gui.linkgrabber.header.status", "Status"), JDLocale.L("gui.linkgrabber.header.downloaddir", "Downloadfolder") };

    private LinkGrabberV2 owner;

    /**
     * Creates a {@link ProjectsTreeTableModel}
     * 
     * @param allLinks
     * @param downloadLinksTreeTable
     * 
     * @param aList
     *            the ProjectList to start out with.
     */
    public LinkGrabberV2TreeTableModel(LinkGrabberV2 treeTable) {
        super("root");
        owner = treeTable;
    }

    public boolean containesPackage(LinkGrabberV2FilePackage fp) {
        return owner.getPackages().contains(fp);
    }

    public Object getChild(Object parent, int index) {
        Object child = null;
        if (parent instanceof String) {
            child = getPackages().get(index);
        } else if (parent instanceof LinkGrabberV2FilePackage) {
            LinkGrabberV2FilePackage pack = (LinkGrabberV2FilePackage) parent;
            child = pack.getDownloadLinks().get(index);
        } else if (parent instanceof DownloadLink) {
            // for now, DownloadLinks do not have Children
            /* mirrors here */
        }
        return child;
    }

    /**
     * Returns the number of children of <code>parent</code>. Returns 0 if the
     * node is a leaf or if it has no children. <code>parent</code> must be a
     * node previously obtained from this data source.
     * 
     * @param parent
     *            a node in the tree, obtained from this data source
     * @return the number of children of the node <code>parent</code>
     */
    public int getChildCount(Object parent) {
        int count = 0;
        if (parent instanceof String) {
            count = getPackages().size();
        } else if (parent instanceof LinkGrabberV2FilePackage) {
            LinkGrabberV2FilePackage pack = (LinkGrabberV2FilePackage) parent;
            count = pack.getDownloadLinks().size();
        } else if (parent instanceof DownloadLink) {
            count = 0;
        }
        return count;
    }

    @Override
    public Class<?> getColumnClass(int column) {
        switch (column) {
        case COL_FILE:
            return String.class;
        case COL_SIZE:
            return String.class;
        case COL_HOSTER:
            return String.class;
        case COL_STATUS:
            return String.class;
        case COL_DLFOLDER:
            return ComboBrowseFile.class;
        }
        return Object.class;
    }

    /**
     * How many columns do we display
     */
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    public int getIndexOfChild(Object parent, Object child) {
        int index = -1;
        if (parent instanceof String) {
            index = getPackages().indexOf(child);
        } else if (parent instanceof LinkGrabberV2FilePackage) {
            index = ((LinkGrabberV2FilePackage) parent).getDownloadLinks().indexOf(child);
        } else if (parent instanceof DownloadLink) {
            index = -1;
        }

        return index;
    }

    public TreeModelSupport getModelSupporter() {
        return modelSupport;
    }

    /**
     * Returns the child of <code>parent</code> at index <code>index</code> in
     * the parent's child array. <code>parent</code> must be a node previously
     * obtained from this data source. This should not return <code>null</code>
     * if <code>index</code> is a valid index for <code>parent</code> (that is
     * <code>index >= 0 &&
     * index < getChildCount(parent</code>)).
     * 
     * @param parent
     *            a node in the tree, obtained from this data source
     * @return the child of <code>parent</code> at index <code>index</code>
     * 
     *         Have to implement this:
     */
    public Vector<LinkGrabberV2FilePackage> getPackages() {
        return owner.getPackages();
    }

    /**
     * What is shown in a cell column for a node.
     */
    public Object getValueAt(Object node, int column) {

        if (node instanceof DownloadLink) {
            DownloadLink downloadLink = (DownloadLink) node;
            switch (column) {
            case COL_FILE:
                return downloadLink.getName();
            case COL_SIZE:
                return downloadLink.getDownloadSize() > 0 ? JDUtilities.formatBytesToMB(downloadLink.getDownloadSize()) : "~";
            case COL_HOSTER:
                return downloadLink.getPlugin().getHost();
            case COL_STATUS:
                if (!downloadLink.isAvailabilityChecked()) return "availability unchecked!";
                if (downloadLink.isAvailable()) {
                    return "online";
                } else
                    return "offline";
            default:
                return "";
            }
        } else if (node instanceof LinkGrabberV2FilePackage) {
            LinkGrabberV2FilePackage filePackage = (LinkGrabberV2FilePackage) node;
            switch (column) {
            case COL_FILE:
                return filePackage.getName();
            case COL_SIZE:
                return "";
            case COL_HOSTER:
                return filePackage.getHoster();
            case COL_STATUS:
                return "";
            case COL_DLFOLDER:
                return filePackage.getComboBrowseFile();
            default:
                return "";
            }
        } else if (node instanceof String) {
            return (column == 0) ? node.toString() : "";
        } else {
            System.out.println("node.class: " + node.getClass());
        }

        return null;
    }

    @Override
    public boolean isCellEditable(Object node, int column) {
        return true;
    }

}

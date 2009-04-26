package jd.gui.skins.simple.components.Linkgrabber;

import java.util.Vector;

import jd.controlling.LinkGrabberController;
import jd.plugins.DownloadLink;
import jd.utils.JDLocale;

import org.jdesktop.swingx.tree.TreeModelSupport;
import org.jdesktop.swingx.treetable.AbstractTreeTableModel;

public class LinkGrabberTreeTableModel extends AbstractTreeTableModel {

    public static final int COL_HIDDEN = 0;
    public static final int COL_PACK_FILE = 1;
    public static final int COL_SIZE = 2;
    public static final int COL_HOSTER = 3;
    public static final int COL_STATUS = 4;

    /** table column names */
    static protected String[] COLUMN_NAMES = { "hidden", JDLocale.L("gui.linkgrabber.header.packagesfiles", "Pakete/Dateien"), JDLocale.L("gui.treetable.header.size", "Größe"), JDLocale.L("gui.treetable.header_3.hoster", "Anbieter"), JDLocale.L("gui.treetable.header_4.status", "Status") };

    private LinkGrabberController lgi;

    /**
     * Creates a {@link ProjectsTreeTableModel}
     * 
     * @param allLinks
     * @param downloadLinksTreeTable
     * 
     * @param aList
     *            the ProjectList to start out with.
     */
    public LinkGrabberTreeTableModel(LinkGrabberPanel treeTable) {
        super("root");
        lgi=LinkGrabberController.getInstance();
    }

    public boolean containesPackage(LinkGrabberFilePackage fp) {
        return lgi.getPackages().contains(fp);
    }

    public Object getChild(Object parent, int index) {
        Object child = null;
        if (parent instanceof String) {
            child = getPackages().get(index);
        } else if (parent instanceof LinkGrabberFilePackage) {
            LinkGrabberFilePackage pack = (LinkGrabberFilePackage) parent;
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
        } else if (parent instanceof LinkGrabberFilePackage) {
            LinkGrabberFilePackage pack = (LinkGrabberFilePackage) parent;
            count = pack.getDownloadLinks().size();
        } else if (parent instanceof DownloadLink) {
            count = 0;
        }
        return count;
    }

    @Override
    public Class<?> getColumnClass(int column) {
        switch (column) {
        case COL_HIDDEN:
            return String.class;
        case COL_PACK_FILE:
            return Object.class;
        case COL_SIZE:
            return String.class;
        case COL_HOSTER:
            return String.class;
        case COL_STATUS:
            return String.class;
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

    public static int getIDFormHeaderLabel(String label) {
        for (int i = 0; i < COLUMN_NAMES.length; i++) {
            if (COLUMN_NAMES[i].equals(label)) { return i; }
        }
        return -1;
    }

    public int getIndexOfChild(Object parent, Object child) {
        int index = -1;
        if (parent instanceof String) {
            index = getPackages().indexOf(child);
        } else if (parent instanceof LinkGrabberFilePackage) {
            index = ((LinkGrabberFilePackage) parent).getDownloadLinks().indexOf(child);
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
    public Vector<LinkGrabberFilePackage> getPackages() {
        return lgi.getPackages();
    }

    /**
     * What is shown in a cell column for a node.
     */
    public Object getValueAt(Object node, int column) {
        if (node instanceof DownloadLink) {
            DownloadLink downloadLink = (DownloadLink) node;
            switch (column) {
            case COL_PACK_FILE:
                return downloadLink;
            case COL_SIZE:
                return downloadLink;
            case COL_HOSTER:
                return downloadLink;
            case COL_STATUS:
                return downloadLink;
            }
        } else if (node instanceof LinkGrabberFilePackage) {
            LinkGrabberFilePackage filePackage = (LinkGrabberFilePackage) node;
            switch (column) {
            case COL_PACK_FILE:
                return filePackage;
            case COL_SIZE:
                return filePackage;
            case COL_HOSTER:
                return filePackage;
            case COL_STATUS:
                return filePackage;
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

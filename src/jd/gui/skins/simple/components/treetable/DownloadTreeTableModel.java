//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.skins.simple.components.treetable;

import java.util.Vector;

import javax.swing.tree.TreePath;

import jd.gui.skins.simple.DownloadLinksTreeTablePanel;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.update.PackageData;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

import org.jdesktop.swingx.tree.TreeModelSupport;
import org.jdesktop.swingx.treetable.AbstractTreeTableModel;

public class DownloadTreeTableModel extends AbstractTreeTableModel {

    public static final int COL_FILE = 2;

    public static final int COL_HOSTER = 3;

    /** index of tree column */
    public static final int COL_HIDDEN = 0;
    public static final int COL_PART = 1;

    public static final int COL_PROGRESS = 5;

    public static final int COL_STATUS = 4;

    /** table column names */
    static protected String[] COLUMN_NAMES = {"hidden", JDLocale.L("gui.treetable.header_1.tree", "Pakete"), JDLocale.L("gui.treetable.header_2.files", "Dateien"), JDLocale.L("gui.treetable.header_3.hoster", "Anbieter"), JDLocale.L("gui.treetable.header_4.status", "Status"), JDLocale.L("gui.treetable.header_5.progress", "Fortschritt") };

    private DownloadLinksTreeTablePanel owner;

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
        owner = treeTable;
    }

    public static int getIDFormHeaderLabel(String label) {
        for (int i = 0; i < COLUMN_NAMES.length; i++) {
            if (COLUMN_NAMES[i].equals(label)) { return i; }

        }
        return -1;
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
        } else if (parent instanceof FilePackage) {
            FilePackage pack = (FilePackage) parent;
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
        case COL_PART:
            return Object.class;
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
        } else if (parent instanceof FilePackage) {
            index = ((FilePackage) parent).getDownloadLinks().indexOf(child);
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
    public Vector<FilePackage> getPackages() {
        return owner.getPackages();
    }

    /**
     * What is shown in a cell column for a node.
     */
    public Object getValueAt(Object node, int column) {

        if (node instanceof DownloadLink) {
            DownloadLink downloadLink = (DownloadLink) node;
            switch (column) {
            case COL_PART:
                return downloadLink;
//                if (downloadLink.getLinkType() == DownloadLink.LINKTYPE_JDU) {
//                    PackageData pd = (PackageData) downloadLink.getProperty("JDU");
//                    return JDLocale.L("gui.treetable.part.label_update", "Update") + " " + pd.getInstalledVersion() + " -> " + pd.getStringProperty("version");
//                } else {
//                    int id = downloadLink.getPartByName();
//                    return JDLocale.L("gui.treetable.part.label", "Datei") + " " + (id < 0 ? "" : JDUtilities.fillInteger(id, 3, "0"));
//                }
            case COL_FILE:
                return downloadLink.getName().replaceAll("\\.jdu", "");
            case COL_HOSTER:
                return downloadLink;
            case COL_STATUS:
                return downloadLink.getLinkStatus().getStatusString();
            case COL_PROGRESS:
                return downloadLink;
            }
        } else if (node instanceof FilePackage) {
            FilePackage filePackage = (FilePackage) node;
            switch (column) {
            case COL_PART:
                return filePackage;
            case COL_FILE:
                return filePackage.getDownloadLinks().size() + " " + JDLocale.L("gui.treetable.parts", "Teil(e)");
            case COL_HOSTER:
                return filePackage.getHoster();
            case COL_STATUS:
                return filePackage;
            case COL_PROGRESS:
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

    public boolean move(TreePath[] from, Object before, Object after) {

        if (from.length == 0) { return false; }
        if (from[0].getLastPathComponent() instanceof DownloadLink) { return moveDownloadLinks(from, (DownloadLink) before, (DownloadLink) after); }
        return movePackages(from, (FilePackage) before, (FilePackage) after);

    }

    private boolean moveDownloadLinks(TreePath[] from, DownloadLink before, DownloadLink after) {

        Vector<DownloadLink> links = new Vector<DownloadLink>();

        for (TreePath path : from) {
            links.add((DownloadLink) path.getLastPathComponent());
        }

        return JDUtilities.getController().moveLinks(links, before, after);

    }

    private boolean movePackages(TreePath[] from, FilePackage before, FilePackage after) {
        Vector<FilePackage> fps = new Vector<FilePackage>();
        for (TreePath path : from) {
            fps.add((FilePackage) path.getLastPathComponent());
        }
        if (after != null && !(after instanceof FilePackage)) { return false; }
        return JDUtilities.getController().movePackages(fps, before, after);

    }

    public boolean moveToPackage(TreePath[] from, FilePackage filePackage, boolean position) {
        if (!(from[0].getLastPathComponent() instanceof DownloadLink)) { return false; }
        Vector<DownloadLink> links = new Vector<DownloadLink>();

        for (TreePath path : from) {
            links.add((DownloadLink) path.getLastPathComponent());
        }
        if (filePackage.size() == 0) { return false; }

        if (position) {
            return JDUtilities.getController().moveLinks(links, null, filePackage.get(0));
        } else {
            return JDUtilities.getController().moveLinks(links, filePackage.lastElement(), null);
        }

    }

    @Override
    public void setValueAt(Object value, Object node, int col) {
        JDUtilities.getLogger().info("NNNN");
    }

}
//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://wnu.org/licenses/>.

package jd.gui.skins.simple;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;

import jd.gui.skins.simple.tree.PackageTree;
import jd.gui.skins.simple.tree.PackageTreeCellRenderer;
import jd.gui.skins.simple.tree.PackageTreeModel;
import jd.plugins.DownloadLink;
import jd.utils.JDUtilities;

public class LinkListPanel extends JPanel implements TreeModelListener {
    private final int            COL_INDEX        = 0;

    private static final long    serialVersionUID = 3033753799006526304L;

    /**
     * Diese Tabelle enthält die eigentlichen DownloadLinks
     */

    /**
     * Dieser Vector enthält alle Downloadlinks
     */
    private Vector<DownloadLink> allLinks         = new Vector<DownloadLink>();

    /**
     * Der Logger für Meldungen
     */
    private Logger               logger           = JDUtilities.getLogger();

    private SimpleGUI            parent;

    private PackageTree          tree;

    private PackageTreeModel     treeModel;

    /**
     * Erstellt eine neue Tabelle
     * 
     * @param parent Das aufrufende Hauptfenster
     */
    public LinkListPanel(SimpleGUI parent) {
        super(new BorderLayout());
        this.parent = parent;

        treeModel = new PackageTreeModel(this);
        // tree.setModel(treeModel);
        treeModel.addTreeModelListener(this);
        tree = new PackageTree(treeModel);
        tree.setRootVisible(false);
        tree.setCellRenderer(new PackageTreeCellRenderer());
        tree.setBackground(Color.RED);
        tree.setRowHeight(-1);
        tree.setScrollsOnExpand(true);
        tree.setToggleClickCount(1);
        UIManager.put("Tree.line", Color.GREEN);
        tree.putClientProperty("JTree.lineStyle", "Horizontal");
        // tree.setCellRenderer(new InternalTreeRenderer());

        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setPreferredSize(new Dimension(800, 450));
        // table.setPreferredSize(new Dimension(800,450));
        add(tree);
    }

    public void setDownloadLinks(DownloadLink links[]) {
        treeModel.setLinks(links);
    }

    public void addLinks(DownloadLink links[]) {

        treeModel.addLinks(links);

    }

    public Vector<DownloadLink> getLinks() {
        return treeModel.getLinks();
    }

    /**
     * Hiermit wird die Tabelle aktualisiert Die Markierte reihe wird nach dem
     * ändern wieder neu gesetzt TODO: Selection verwaltung
     */
    public void refresh() {

        treeModel.fireTreeNodesChanged(null);
        // treeModel.fireTreeStructureChanged(null);
    }

    public void moveSelectedItems(int id) {
    // TODO Auto-generated method stub

    }

    public void removeSelectedLinks() {
    // TODO Auto-generated method stub

    }

    public void treeNodesChanged(TreeModelEvent e) {

    }

    public void treeNodesInserted(TreeModelEvent e) {}

    public void treeNodesRemoved(TreeModelEvent e) {}

    public void treeStructureChanged(TreeModelEvent e) {

    }
}

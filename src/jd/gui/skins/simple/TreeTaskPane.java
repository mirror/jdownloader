package jd.gui.skins.simple;

import java.awt.Color;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

public class TreeTaskPane extends TaskPanel implements TreeSelectionListener {
    /**
     * 
     */
    private static final long serialVersionUID = 6598257233260798160L;
    private static final String DEBUG = "";
    protected SideTreeModel root;

    protected TreeTabbedNode rootNode;
    protected JTree tree;

    public TreeTaskPane(String title, Icon icon) {
        this.setTitle(title);
        this.setIcon(icon);
        rootNode = new TreeTabbedNode("Root Node");
        this.root = new SideTreeModel(rootNode);
        initGUI();

    }

    private void initGUI() {

        tree = new JTree();
        tree.addTreeSelectionListener(this);
        tree.setModel(root);
        tree.setShowsRootHandles(false);

        // tree.putClientProperty("JTree.lineStyle", "Horizontal");
        UIManager.put("Tree.rendererFillBackground", false);
        //UIManager.put("Tree.textBackground", null);
        UIManager.put("Tree.selectionForeground", Color.black);
       
        tree.setCellRenderer(new SidebarTreeRenderer());
        // tree.setLayout(new MigLayout("debug"));
        tree.setRootVisible(false);
        tree.setOpaque(false);
        // tree.setCellRenderer(new DefaultTreeCellRenderer());
        ;

        this.add(tree);

        // SINGLE_TREE_SELECTION);
        // return tree;
    }

    public SideTreeModel getRoot() {
        return root;
    }

    public void valueChanged(TreeSelectionEvent e) {
        TreeTabbedNode node = (TreeTabbedNode) tree.getLastSelectedPathComponent();
        if (node == null) return;
        getTabbedPane().display(node);

    }

}

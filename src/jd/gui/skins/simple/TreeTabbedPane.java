package jd.gui.skins.simple;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.border.LineBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeSelectionModel;

import net.miginfocom.swing.MigLayout;

public class TreeTabbedPane extends JPanel implements TreeSelectionListener {
    private static final String DEBUG = "";
    private SideTreeModel root;
    private JTabbedPanel rightPanel;

    private TreeTabbedNode rootNode;
    private JTree tree;

    public TreeTabbedPane(SimpleGUI simpleGUI) {
        rootNode = new TreeTabbedNode("Root Node");
        this.root = new SideTreeModel(rootNode);
        initGUI();

    }

    private void initGUI() {
        this.setLayout(new MigLayout(DEBUG + "ins 0", "[140px!,fill]push[grow,fill]", "[fill]"));
        this.add(getLeftPanel(), "push, gap 0 0 0 0");

    }

    private Component getLeftPanel() {
        tree = new JTree();
        tree.addTreeSelectionListener(this);

        tree.setModel(root);
        tree.setShowsRootHandles(true);
        tree.setBorder(new LineBorder(Color.gray, 1));
        tree.putClientProperty("JTree.lineStyle", "Horizontal");
        tree.setCellRenderer(new SidebarTreeRenderer());
        // tree.setLayout(new MigLayout("debug"));
        tree.setRootVisible(false);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        return tree;
    }

    public SideTreeModel getRoot() {
        return root;
    }

    public void display(int... args) {
        TreeTabbedNode node = this.rootNode;

        for (int i : args) {
            node = (TreeTabbedNode) root.getChild(node, i);
        }
        if (this.rightPanel != null) {
            rightPanel.onHide();
        }
        displayPanel(node.getPanel());
        node.getPanel().onDisplay(node.increaseShowCounter());

    }

    public void valueChanged(TreeSelectionEvent e) {
        TreeTabbedNode node = (TreeTabbedNode) tree.getLastSelectedPathComponent();
        if (node == null) return;
        display(node);

    }

    private void displayPanel(JTabbedPanel panel) {
        if(rightPanel!=null)
        this.remove(rightPanel);

        rightPanel = panel;

        this.add(rightPanel, "cell 1 0");
        this.revalidate();
        this.repaint();

    }

    public void display(TreeTabbedNode node) {
        JTabbedPanel panel = node.getPanel();

        if (panel != null) {
            if (this.rightPanel != null) {
                rightPanel.onHide();
            }
            displayPanel(panel);
            node.getPanel().onDisplay(node.increaseShowCounter());
        }
        ;

    }

}

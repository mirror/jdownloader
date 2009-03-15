package jd.gui.skins.simple;

import java.awt.Component;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;

public class TabbedPane extends JXPanel {
    private static final String DEBUG = "";

    private JTabbedPanel rightPanel;

    private JXTaskPaneContainer taskPane;

    public TabbedPane(SimpleGUI simpleGUI) {

        initGUI();

    }

    private void initGUI() {
        this.setLayout(new MigLayout(DEBUG + "ins 0", "[fill]push[grow,fill]", "[fill]"));
        this.add(getLeftPanel(), "push, gap 0 0 0 0");

    }

    public void add(TaskPanel pane) {
        pane.setTabbedPane(this);
        taskPane.add(pane);
    }

    private Component getLeftPanel() {

        taskPane = new JXTaskPaneContainer();

        // panel.setLayout(new MigLayout("debug, ins 5","[]"));

        // JXTaskPane download = new JXTaskPane();
        // download.setTitle("Download");
        // download.setSpecial(true);
        // download.setIcon(JDTheme.II("gui.images.down"));
        //        
        // panel.add(download);
        //
        // // create another taskPane, it will show details of the selected file
        // JXTaskPane config = new JXTaskPane();
        // config.setTitle("Configuration");
        // config.setIcon(JDTheme.II("gui.images.configuration"));
        // panel.add(config);
        //
        //      
        //
        // tree = new JTree();
        // tree.addTreeSelectionListener(this);
        //        
        // tree.setModel(root);
        // tree.setShowsRootHandles(true);
        // tree.setBorder(new LineBorder(Color.gray, 1));
        // tree.putClientProperty("JTree.lineStyle", "Horizontal");
        // tree.setCellRenderer(new SidebarTreeRenderer());
        // // tree.setLayout(new MigLayout("debug"));
        // tree.setRootVisible(false);
        // config.add(tree);
        return taskPane;
        // SINGLE_TREE_SELECTION);
        // return tree;
    }

    //
    // public void display(int... args) {
    // TreeTabbedNode node = this.rootNode;
    //
    // for (int i : args) {
    // node = (TreeTabbedNode) root.getChild(node, i);
    // }
    // if (this.rightPanel != null) {
    // rightPanel.onHide();
    // }
    // displayPanel(node.getPanel());
    // node.getPanel().onDisplay(node.increaseShowCounter());
    //
    // }

    // public void valueChanged(TreeSelectionEvent e) {
    // TreeTabbedNode node = (TreeTabbedNode)
    // tree.getLastSelectedPathComponent();
    // if (node == null) return;
    // display(node);
    //
    // }

    public void displayPanel(JTabbedPanel panel) {
        if (rightPanel != null) this.remove(rightPanel);

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

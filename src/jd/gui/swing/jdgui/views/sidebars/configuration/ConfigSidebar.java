package jd.gui.swing.jdgui.views.sidebars.configuration;

import java.awt.event.MouseEvent;

import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import jd.gui.swing.GuiRunnable;
import jd.gui.swing.jdgui.interfaces.SideBarPanel;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.jdgui.views.ConfigurationView;
import jd.gui.swing.jdgui.views.sidebars.configuration.ConfigTreeModel.TreeEntry;
import net.miginfocom.swing.MigLayout;

public class ConfigSidebar extends SideBarPanel {

    private static final long serialVersionUID = 6456662020047832983L;
    private static ConfigSidebar INSTANCE = null;
    private JTree tree;
    private ConfigurationView view;

    private ConfigSidebar(ConfigurationView configurationView) {
        this.view = configurationView;
        this.setLayout(new MigLayout("ins 0", "[grow,fill]", "[grow,fill]"));

        this.add(tree = new JTree(getTreeModel()) {
            private static final long serialVersionUID = -5018817191000357595L;

            /**
             * workaround a synthetica layout bug with doubleclick
             */
            public void processMouseEvent(MouseEvent m) {
                if (m.getClickCount() > 1) return;
                super.processMouseEvent(m);
            }

        });

        tree.setCellRenderer(new TreeRenderer());
        tree.setOpaque(false);
        tree.setRootVisible(false);
        tree.setRowHeight(24);
        tree.setExpandsSelectedPaths(true);
        tree.setBackground(null);
        tree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {

            private TreeEntry entry;

            public void valueChanged(TreeSelectionEvent e) {
                new GuiRunnable<Object>() {

                    @Override
                    public Object runSave() {
                        if (tree.getSelectionPath() == null) return null;
                        entry = (TreeEntry) tree.getSelectionPath().getLastPathComponent();
                        tree.expandPath(tree.getSelectionPath());
                        if (entry.getPanel() == null) {
                            entry = entry.getEntries().get(0);
                        }
                        view.setContent(entry.getPanel().getPanel());
                        return null;
                    }
                }.start();
            }

        });
        TreePath rootPath = new TreePath(tree.getModel().getRoot());
        tree.expandPath(rootPath);
        TreeEntry node = (TreeEntry) rootPath.getLastPathComponent();
        for (TreeEntry n : node.getEntries()) {
            TreePath path = rootPath.pathByAddingChild(n);
            tree.expandPath(path);
        }

        tree.setSelectionRow(0);
    }

    public void expandAll(JTree tree, boolean expand) {
        TreeEntry root = (TreeEntry) tree.getModel().getRoot();
        expandAll(tree, new TreePath(root), expand);
    }

    public void expandAll(JTree tree, TreePath parent, boolean expand) {
        TreeEntry node = (TreeEntry) parent.getLastPathComponent();
        if (node.size() >= 0) {
            for (TreeEntry n : node.getEntries()) {
                TreePath path = parent.pathByAddingChild(n);
                expandAll(tree, path, expand);
            }
        }
        if (expand) {
            tree.expandPath(parent);
        } else {
            tree.collapsePath(parent);
        }
    }

    private TreeModel getTreeModel() {
        return new ConfigTreeModel();
    }

    @Override
    protected void onHide() {
    }

    @Override
    protected void onShow() {
    }

    public void setSelectedTreeEntry(Class<? extends SwitchPanel> class1) {

        TreeEntry root = (TreeEntry) tree.getModel().getRoot();
        TreePath path = getEntry(new TreePath(root), TreeEntry.getTreeByClass(class1));
        tree.setSelectionPath(path);

    }

    private TreePath getEntry(TreePath parent, TreeEntry treeEntry) {
        TreeEntry node = (TreeEntry) parent.getLastPathComponent();
        if (node == treeEntry) return parent;
        if (node.size() >= 0) {
            for (TreeEntry n : node.getEntries()) {
                TreePath path = parent.pathByAddingChild(n);

                TreePath res = getEntry(path, treeEntry);
                if (res != null) return res;
            }
        }

        return null;

    }

    /**
     * CReate a singlton instance of the config sidebar
     * 
     * @param configurationView
     * @return
     */
    public static ConfigSidebar getInstance(ConfigurationView configurationView) {
        if (INSTANCE == null && configurationView != null) INSTANCE = new ConfigSidebar(configurationView);
        return INSTANCE;
    }

    /**
     * Updates The addon subtree
     */
    public void updateAddons() {
        TreePath path = ((ConfigTreeModel) tree.getModel()).updateAddons();
        expandAll(tree, path, true);

    }

}

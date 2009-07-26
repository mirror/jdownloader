package jd.gui.skins.jdgui.views.sidebars.configuration;

import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import jd.gui.skins.jdgui.interfaces.SideBarPanel;
import jd.gui.skins.jdgui.swing.GuiRunnable;
import jd.gui.skins.jdgui.views.ConfigurationView;
import jd.gui.skins.jdgui.views.sidebars.configuration.ConfigTreeModel.TreeEntry;
import net.miginfocom.swing.MigLayout;

public class ConfigSidebar extends SideBarPanel {

    private static final long serialVersionUID = 6456662020047832983L;
    private JTree tree;
    private ConfigurationView view;

    public ConfigSidebar(ConfigurationView configurationView) {
        this.view = configurationView;
        this.setLayout(new MigLayout("ins 0 ", "[grow,fill]", "[grow,fill]"));

        this.add(tree = new JTree(getTreeModel()));
        tree.setCellRenderer(new TreeRenderer());        
        tree.setOpaque(false);
        tree.setRootVisible(false);
        tree.setExpandsSelectedPaths(true);
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

}

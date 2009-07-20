package jd.gui.skins.jdgui.views.sidebars.configuration;

import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import jd.gui.skins.jdgui.interfaces.SideBarPanel;
import jd.gui.skins.jdgui.views.ConfigurationView;
import jd.gui.skins.jdgui.views.sidebars.configuration.ConfigTreeModel.TreeEntry;
import jd.gui.skins.simple.GuiRunnable;
import net.miginfocom.swing.MigLayout;

public class ConfigSidebar extends SideBarPanel {
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
                        entry = (TreeEntry) tree.getSelectionPath().getLastPathComponent();
                        if (entry.getPanel() == null) {
                            entry = entry.getEntries().get(0);
                        }
                        view.setContent(entry.getPanel().getPanel());
                        view.revalidate();
                        return null;
                    }
                }.start();
            }

        });
        expandAll(tree, true);
        tree.setSelectionRow(0);
    }

    public void expandAll(JTree tree, boolean expand) {
        TreeEntry root = (TreeEntry) tree.getModel().getRoot();
        expandAll(tree, new TreePath(root), expand);
    }

    private void expandAll(JTree tree, TreePath parent, boolean expand) {

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
        // TODO Auto-generated method stub

    }

    @Override
    protected void onShow() {
        // TODO Auto-generated method stub

    }

}

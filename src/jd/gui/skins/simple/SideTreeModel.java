package jd.gui.skins.simple;

import javax.swing.tree.DefaultTreeModel;

class SideTreeModel extends DefaultTreeModel {

    public SideTreeModel(TreeTabbedNode root) {
        super(root);

    }

    public void insertNodeInto(TreeTabbedNode newChild, TreeTabbedNode parent, int index) {
        if (parent == null) parent = (TreeTabbedNode) this.getRoot();
        if (index < 0) index = parent.getChildCount();
        super.insertNodeInto(newChild, parent, index);
        nodeStructureChanged(parent);

    }

    public void removeAll() {
        for (int i = 0; i < this.getChildCount(root); i++) {
            this.removeNodeFromParent((TreeTabbedNode) this.getChild(root, i));

        }
    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public void insertNodeInto(TreeTabbedNode treeTabbedNode) {
        insertNodeInto(treeTabbedNode, null, -1);

    }

    public void insertNodeInto(TreeTabbedNode treeTabbedNode, TreeTabbedNode config) {
        insertNodeInto(treeTabbedNode, config, -1);

    }

}
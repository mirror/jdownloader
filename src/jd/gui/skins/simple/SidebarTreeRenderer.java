package jd.gui.skins.simple;

import java.awt.Component;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

public class SidebarTreeRenderer extends DefaultTreeCellRenderer {
    public SidebarTreeRenderer() {

    }

    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        TreeTabbedNode node = (TreeTabbedNode) value;
        if (node.getIcon() != null) {

            if (leaf) {
                this.setLeafIcon(node.getIcon());
            } else {
                this.setOpenIcon(node.getIcon());
                this.setClosedIcon(node.getIcon());
            }
        }
        Component ret = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        return ret;
    }

}

package jd.gui.skins.jdgui.views.sidebars.configuration;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import jd.gui.skins.jdgui.views.sidebars.configuration.ConfigTreeModel.TreeEntry;

public class TreeRenderer extends DefaultTreeCellRenderer {

    private static final long serialVersionUID = -3927390875702401200L;
    private TreeEntry te;

    public TreeRenderer() {
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        te = (TreeEntry) value;
        setText(te.getTitle());
        setIcon(te.getIcon());
        setToolTipText(te.getTooltip());
        this.setPreferredSize(new Dimension(200, 20));
        return this;
    }

    public Color getBackgroundNonSelectionColor() {
        return null;
    }

    public Color getBackground() {
        return null;
    }

}

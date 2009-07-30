package jd.gui.swing.jdgui.views.sidebars.configuration;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import jd.gui.swing.jdgui.views.sidebars.configuration.ConfigTreeModel.TreeEntry;
import jd.utils.JDTheme;

public class TreeRenderer extends DefaultTreeCellRenderer {

    private static final long serialVersionUID = -3927390875702401200L;
    private TreeEntry te;
    private Font orgFont;
    private Font boldFont;
    private JLabel label;

    public TreeRenderer() {
        label = new JLabel();
        label.setBackground(null);

    }

    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {

        te = (TreeEntry) value;
        if (orgFont == null && label.getFont() != null) {
            orgFont = label.getFont();
            boldFont = label.getFont().deriveFont(label.getFont().getStyle() ^ Font.BOLD);
        }

        label.setText(te.getTitle());
        if (!sel && te.getIcon() != null) {

            label.setFont(orgFont);

            label.setIcon(JDTheme.II(te.getIconKey(), 16, 16));
        } else {
            label.setFont(boldFont);

            label.setIcon(te.getIcon());
        }
        label.setToolTipText(te.getTooltip());

        label.setPreferredSize(new Dimension(200, 20));
        return label;
    }

}

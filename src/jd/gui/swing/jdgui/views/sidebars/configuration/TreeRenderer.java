package jd.gui.swing.jdgui.views.sidebars.configuration;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import jd.gui.swing.jdgui.views.sidebars.configuration.ConfigTreeModel.TreeEntry;
import jd.utils.JDTheme;

public class TreeRenderer extends DefaultTreeCellRenderer {

    private static final long serialVersionUID = -3927390875702401200L;
    private TreeEntry te;
    private Font orgFont;
    private Font boldFont;

    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        te = (TreeEntry) value;
        if (orgFont == null && getFont() != null) {
            orgFont = getFont();
            boldFont = getFont().deriveFont(getFont().getStyle() ^ Font.BOLD);
        }
      
        setText(te.getTitle());
        if (!sel && te.getIcon() != null) {

            setFont(orgFont);

            setIcon(JDTheme.II(te.getIconKey(), 16, 16));
        } else {
            setFont(boldFont);

            setIcon(te.getIcon());
        }
        setToolTipText(te.getTooltip());

        this.setPreferredSize(new Dimension(200, 20));
        return this;
    }

}

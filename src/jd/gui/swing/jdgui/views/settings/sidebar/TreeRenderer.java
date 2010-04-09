//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.swing.jdgui.views.settings.sidebar;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

public class TreeRenderer extends DefaultTreeCellRenderer {

    private static final long serialVersionUID = -3927390875702401200L;

    private final JLabel label;
    private final Font orgFont;
    private final Font boldFont;

    private TreeEntry te;

    public TreeRenderer() {
        label = new JLabel();
        label.setBackground(null);

        orgFont = label.getFont();
        boldFont = label.getFont().deriveFont(label.getFont().getStyle() ^ Font.BOLD);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        te = (TreeEntry) value;

        label.setText(te.getTitle());
        if (sel) {
            label.setFont(boldFont);
            label.setIcon(te.getIcon());
        } else {
            label.setFont(orgFont);
            label.setIcon(te.getIconSmall());
        }
        label.setPreferredSize(new Dimension(200, 20));
        return label;
    }

}

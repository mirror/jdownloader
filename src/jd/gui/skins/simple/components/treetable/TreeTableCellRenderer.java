package jd.gui.skins.simple.components.treetable;

import java.awt.Color;
import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.SwingConstants;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

import org.jdesktop.swingx.renderer.DefaultTreeRenderer;

public class TreeTableCellRenderer extends DefaultTreeRenderer {

    private static final long serialVersionUID = 1L;

    private Color FONT_COLOR;

    private Color FONT_COLOR_SELECTED;

    private JLabel lbl_fp_closed;

    private JLabel lbl_fp_opened;

    private JLabel lbl_link;

    public TreeTableCellRenderer() {
        super();

        FONT_COLOR = JDTheme.C("gui.color.downloadlist.font", "ff0000");
        FONT_COLOR_SELECTED = JDTheme.C("gui.color.downloadlist.font_selected", "ffffff");
        this.lbl_link = new JLabel("", new ImageIcon(JDUtilities.getImage(JDTheme.V("gui.images.link"))), SwingConstants.LEFT);
        this.lbl_fp_closed = new JLabel("", new ImageIcon(JDUtilities.getImage(JDTheme.V("gui.images.package_closed"))), SwingConstants.LEFT);
        this.lbl_fp_opened = new JLabel("", new ImageIcon(JDUtilities.getImage(JDTheme.V("gui.images.package_opened"))), SwingConstants.LEFT);

        lbl_link.setOpaque(false);
        lbl_fp_closed.setOpaque(false);
        lbl_fp_opened.setOpaque(false);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {

        if (value instanceof DownloadLink) {
            int id = ((DownloadLink) value).getPartByName();
            if (((DownloadLink) value).getLinkType() == DownloadLink.LINKTYPE_JDU) {
                lbl_link.setText(JDLocale.L("gui.treetable.part.label_update", "Update ") + "-> v." + ((DownloadLink) value).getSourcePluginComment().split("_")[1]);
            } else {
                lbl_link.setText(JDLocale.L("gui.treetable.part.label", "Datei ") + (id < 0 ? "" : JDUtilities.fillInteger(id, 3, "0")) + "  ");
            }

            if (selected) {
                lbl_link.setForeground(FONT_COLOR_SELECTED);
                lbl_link.setBackground(FONT_COLOR_SELECTED);
            } else {
                lbl_link.setForeground(FONT_COLOR);
                lbl_link.setBackground(FONT_COLOR);
            }
            return lbl_link;
        } else if (value instanceof FilePackage) {

            if (expanded) {
                lbl_fp_opened.setText(((FilePackage) value).getName());
                if (selected) {
                    lbl_fp_opened.setForeground(FONT_COLOR_SELECTED);
                    lbl_fp_opened.setBackground(FONT_COLOR_SELECTED);
                } else {
                    lbl_fp_opened.setForeground(FONT_COLOR);
                    lbl_fp_opened.setBackground(FONT_COLOR);
                }
                return lbl_fp_opened;
            } else {
                lbl_fp_closed.setText(((FilePackage) value).getName());
                if (selected) {
                    lbl_fp_closed.setForeground(FONT_COLOR_SELECTED);
                    lbl_fp_closed.setBackground(FONT_COLOR_SELECTED);
                } else {
                    lbl_fp_closed.setForeground(FONT_COLOR);
                    lbl_fp_closed.setBackground(FONT_COLOR);
                }
                return lbl_fp_closed;
            }

        }
        if (selected) {
            lbl_link.setForeground(FONT_COLOR_SELECTED);
            lbl_link.setBackground(FONT_COLOR_SELECTED);
        } else {
            lbl_link.setForeground(FONT_COLOR);
            lbl_link.setBackground(FONT_COLOR);
        }

        return lbl_link;
    }
}
package jd.gui.skins.simple.components.Linkgrabber;

import java.awt.Color;
import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.SwingConstants;

import jd.plugins.DownloadLink;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

import org.jdesktop.swingx.renderer.DefaultTreeRenderer;

public class LinkGrabberV2TreeTableCellRenderer extends DefaultTreeRenderer {

    private static final long serialVersionUID = 1L;

    private Color FONT_COLOR;

    private Color FONT_COLOR_SELECTED;

    private JLabel lbl_fp_closed;

    private JLabel lbl_fp_opened;

    private JLabel lbl_link;

    public LinkGrabberV2TreeTableCellRenderer() {
        super();

        FONT_COLOR = JDTheme.C("gui.color.downloadlist.font", "ff0000");
        FONT_COLOR_SELECTED = JDTheme.C("gui.color.downloadlist.font_selected", "ffffff");
        lbl_link = new JLabel("", new ImageIcon(JDUtilities.getImage(JDTheme.V("gui.images.link"))), SwingConstants.LEFT);
        lbl_fp_closed = new JLabel("", new ImageIcon(JDUtilities.getImage(JDTheme.V("gui.images.package_closed"))), SwingConstants.LEFT);
        lbl_fp_opened = new JLabel("", new ImageIcon(JDUtilities.getImage(JDTheme.V("gui.images.package_opened"))), SwingConstants.LEFT);

        lbl_link.setOpaque(false);
        lbl_fp_closed.setOpaque(false);
        lbl_fp_opened.setOpaque(false);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        if (value instanceof DownloadLink) {
            lbl_link.setText(((DownloadLink) value).getName());
            if (selected) {
                lbl_link.setForeground(FONT_COLOR_SELECTED);
                lbl_link.setBackground(FONT_COLOR_SELECTED);
            } else {
                lbl_link.setForeground(FONT_COLOR);
                lbl_link.setBackground(FONT_COLOR);
            }
            return lbl_link;
        } else if (value instanceof LinkGrabberV2FilePackage) {
            LinkGrabberV2FilePackage fp = (LinkGrabberV2FilePackage) value;
            if (expanded) {
                lbl_fp_opened.setText(fp.getName());
                if (selected) {
                    lbl_fp_opened.setForeground(FONT_COLOR_SELECTED);
                    lbl_fp_opened.setBackground(FONT_COLOR_SELECTED);
                } else {
                    lbl_fp_opened.setForeground(FONT_COLOR);
                    lbl_fp_opened.setBackground(FONT_COLOR);
                }
                return lbl_fp_opened;
            } else {
                lbl_fp_closed.setText(fp.getName());
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

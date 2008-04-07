package jd.gui.skins.simple.components.treetable;

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

//TreeCellRenderer
public class TreeTableCellRenderer extends DefaultTreeRenderer {

    private JLabel lbl_link;

    private JLabel lbl_fp_closed;

    private JLabel lbl_fp_opened;

    public TreeTableCellRenderer() {
        super();
        this.lbl_link = new JLabel("", new ImageIcon(JDUtilities.getImage(JDTheme.I("gui.images.link"))), SwingConstants.LEFT);
        this.lbl_fp_closed = new JLabel("", new ImageIcon(JDUtilities.getImage(JDTheme.I("gui.images.package_closed"))), SwingConstants.LEFT);
        this.lbl_fp_opened = new JLabel("", new ImageIcon(JDUtilities.getImage(JDTheme.I("gui.images.package_opened"))), SwingConstants.LEFT);
    
        lbl_link.setOpaque(false);
        lbl_fp_closed.setOpaque(false);
        lbl_fp_opened.setOpaque(false);
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {

        // Component c = super.getTreeCellRendererComponent(tree, value,
        // selected, expanded, leaf, row, hasFocus);
        if (value instanceof DownloadLink) {

            int id = ((DownloadLink) value).getPartByName();

            lbl_link.setText(JDLocale.L("gui.treetable.part.label", "Datei ") + (id < 0 ? "" : JDUtilities.fillInteger(id, 3, "0") + "    "));
            return lbl_link;
        }
        else if (value instanceof FilePackage) {

            if (expanded) {
                lbl_fp_opened.setText(((FilePackage) value).getName());
                return lbl_fp_opened;
            }
            else {
                lbl_fp_closed.setText(((FilePackage) value).getName());
                return lbl_fp_closed;
            }

        }

        return lbl_link;
    }
}
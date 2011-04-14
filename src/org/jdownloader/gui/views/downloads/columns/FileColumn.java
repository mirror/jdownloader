package org.jdownloader.gui.views.downloads.columns;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.border.Border;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PackageLinkNode;
import jd.utils.JDTheme;

import org.appwork.utils.swing.renderer.RenderLabel;
import org.appwork.utils.swing.table.ExtColumn;
import org.appwork.utils.swing.table.ExtTable;

public class FileColumn extends ExtColumn<PackageLinkNode> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private DownloadLink      dLink;
    private Border            leftGap;
    private ImageIcon         icon_fp_open;

    private ImageIcon         icon_fp_closed;

    private FilePackage       fp;
    private RenderLabel       jlr;

    public FileColumn() {
        super("FilePackage", null);
        leftGap = BorderFactory.createEmptyBorder(0, 32, 0, 0);
        icon_fp_open = JDTheme.II("gui.images.package_opened_tree", 32, 16);
        icon_fp_closed = JDTheme.II("gui.images.package_closed_tree", 32, 16);
        jlr = new RenderLabel();
    }

    @Override
    public Object getCellEditorValue() {
        return null;
    }

    @Override
    public boolean isEditable(PackageLinkNode obj) {
        return false;
    }

    @Override
    public boolean isEnabled(PackageLinkNode obj) {
        return false;
    }

    @Override
    public boolean isSortable(PackageLinkNode obj) {
        return false;
    }

    @Override
    public void setValue(Object value, PackageLinkNode object) {
    }

    public JComponent getRendererComponent(ExtTable<PackageLinkNode> table, PackageLinkNode value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof FilePackage) {
            fp = (FilePackage) value;
            jlr.setText(fp.getName());
            jlr.setIcon(fp.isExpanded() ? icon_fp_open : icon_fp_closed);
            jlr.setBorder(null);
        } else {
            dLink = (DownloadLink) value;
            jlr.setIcon(dLink.getIcon());
            jlr.setText(dLink.getName());
            jlr.setBorder(leftGap);
        }
        return jlr;
    }

    @Override
    public boolean isHidable() {
        return false;
    }

}

package org.jdownloader.gui.views.downloads.columns;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.border.Border;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PackageLinkNode;

import org.appwork.utils.swing.renderer.RenderLabel;
import org.appwork.utils.swing.table.ExtColumn;
import org.jdownloader.images.NewTheme;

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
    private RenderLabel       renderer;

    public FileColumn() {
        super("FilePackage", null);
        leftGap = BorderFactory.createEmptyBorder(0, 32, 0, 0);
        icon_fp_open = NewTheme.I().getIcon("tree_package_open", 32);
        icon_fp_closed = NewTheme.I().getIcon("tree_package_closed", 32);
        renderer = new RenderLabel();
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
        return obj.isEnabled();
    }

    @Override
    public boolean isSortable(PackageLinkNode obj) {
        return false;
    }

    @Override
    public void setValue(Object value, PackageLinkNode object) {
    }

    public void configureRendererComponent(PackageLinkNode value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof FilePackage) {
            fp = (FilePackage) value;
            renderer.setBorder(null);
            renderer.setText(fp.getName());
            renderer.setIcon(fp.isExpanded() ? icon_fp_open : icon_fp_closed);
        } else {
            dLink = (DownloadLink) value;
            renderer.setBorder(leftGap);
            renderer.setIcon(dLink.getIcon());
            renderer.setText(dLink.getName());
        }

    }

    @Override
    public boolean isHidable() {
        return false;
    }

    @Override
    public JComponent getEditorComponent(PackageLinkNode value, boolean isSelected, int row, int column) {
        return null;
    }

    @Override
    public JComponent getRendererComponent(PackageLinkNode value, boolean isSelected, boolean hasFocus, int row, int column) {
        return renderer;
    }

    @Override
    public void resetEditor() {
    }

    @Override
    public void resetRenderer() {

    }

    @Override
    public void configureEditorComponent(PackageLinkNode value, boolean isSelected, int row, int column) {
    }

}

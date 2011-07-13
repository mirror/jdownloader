package org.jdownloader.gui.views.downloads.columns;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.border.Border;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PackageLinkNode;

import org.appwork.utils.swing.table.columns.ExtTextColumn;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class FileColumn extends ExtTextColumn<PackageLinkNode> {

    private Border    leftGapBorder;
    private ImageIcon iconPackageOpen;
    private ImageIcon iconPackageClosed;

    public FileColumn() {
        super(_GUI._.filecolumn_title());
        leftGapBorder = BorderFactory.createEmptyBorder(0, 32, 0, 0);
        iconPackageOpen = NewTheme.I().getIcon("tree_package_open", 32);
        iconPackageClosed = NewTheme.I().getIcon("tree_package_closed", 32);

    }

    @Override
    public boolean isEnabled(PackageLinkNode obj) {
        return obj.isEnabled();
    }

    @Override
    public boolean isSortable(PackageLinkNode obj) {
        return true;
    }

    //
    @Override
    public int getDefaultWidth() {
        return 250;
    }

    public void configureRendererComponent(PackageLinkNode value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof FilePackage) {
            FilePackage fp = (FilePackage) value;
            renderer.setBorder(null);
            renderer.setText(fp.getName());
            renderer.setIcon(fp.isExpanded() ? iconPackageOpen : iconPackageClosed);
        } else {
            DownloadLink dLink = (DownloadLink) value;
            renderer.setBorder(leftGapBorder);
            renderer.setIcon(dLink.getIcon());
            renderer.setText(dLink.getName());
        }

    }

    @Override
    public boolean isHidable() {
        return false;
    }

    @Override
    public final String getStringValue(PackageLinkNode value) {
        if (value instanceof FilePackage) {
            FilePackage fp = (FilePackage) value;
            return fp.getName();
        } else {
            DownloadLink dLink = (DownloadLink) value;
            return dLink.getName();
        }
    }

}

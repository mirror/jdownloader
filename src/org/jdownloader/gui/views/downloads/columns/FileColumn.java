package org.jdownloader.gui.views.downloads.columns;

import javax.swing.BorderFactory;
import javax.swing.Icon;
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

    @Override
    public boolean isEditable(PackageLinkNode obj) {
        return false;
    }

    @Override
    protected void setStringValue(String value, PackageLinkNode object) {
        if (object instanceof FilePackage) {
            ((FilePackage) object).setName(value);
        } else {
            // ((DownloadLink) object).setName(value);
        }
    }

    @Override
    protected Icon getIcon(PackageLinkNode value) {
        if (value instanceof FilePackage) {

            return (((FilePackage) value).isExpanded() ? iconPackageOpen : iconPackageClosed);
        } else {
            return (((DownloadLink) value).getIcon());

        }
    }

    public void configureRendererComponent(PackageLinkNode value, boolean isSelected, boolean hasFocus, int row, int column) {
        super.configureRendererComponent(value, isSelected, hasFocus, row, column);
        if (value instanceof FilePackage) {
            renderer.setBorder(null);
        } else {
            renderer.setBorder(leftGapBorder);

        }

    }

    @Override
    public void configureEditorComponent(PackageLinkNode value, boolean isSelected, int row, int column) {
        super.configureEditorComponent(value, isSelected, row, column);
        if (value instanceof FilePackage) {
            editor.setBorder(null);
        } else {
            editor.setBorder(leftGapBorder);
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

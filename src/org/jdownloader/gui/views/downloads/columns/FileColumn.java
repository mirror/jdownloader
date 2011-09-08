package org.jdownloader.gui.views.downloads.columns;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.border.Border;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class FileColumn extends ExtTextColumn<AbstractNode> {

    private Border    leftGapBorder;
    private ImageIcon iconPackageOpen;
    private ImageIcon iconPackageClosed;

    public FileColumn() {
        super(_GUI._.filecolumn_title());
        leftGapBorder = BorderFactory.createEmptyBorder(0, 32, 0, 0);
        iconPackageOpen = NewTheme.I().getIcon("tree_package_open", 32);
        iconPackageClosed = NewTheme.I().getIcon("tree_package_closed", 32);

    }

    public boolean isPaintWidthLockIcon() {
        return false;
    }

    @Override
    public boolean isEnabled(AbstractNode obj) {
        return obj.isEnabled();
    }

    @Override
    public boolean isSortable(AbstractNode obj) {
        return true;
    }

    //
    @Override
    public int getDefaultWidth() {
        return 350;
    }

    @Override
    public boolean isEditable(AbstractNode obj) {
        return false;
    }

    @Override
    protected void setStringValue(String value, AbstractNode object) {
        if (object instanceof FilePackage) {
            ((FilePackage) object).setName(value);
        } else {
            // ((DownloadLink) object).setName(value);
        }
    }

    @Override
    protected Icon getIcon(AbstractNode value) {
        if (value instanceof AbstractPackageNode) {
            return (((AbstractPackageNode<?, ?>) value).isExpanded() ? iconPackageOpen : iconPackageClosed);
        } else if (value instanceof DownloadLink) {
            return (((DownloadLink) value).getIcon());
        } else if (value instanceof CrawledLink) { return (((CrawledLink) value).getIcon()); }
        return null;
    }

    public void configureRendererComponent(AbstractNode value, boolean isSelected, boolean hasFocus, int row, int column) {
        super.configureRendererComponent(value, isSelected, hasFocus, row, column);
        if (value instanceof AbstractPackageNode) {
            renderer.setBorder(null);
        } else {
            renderer.setBorder(leftGapBorder);
        }

    }

    @Override
    public void configureEditorComponent(AbstractNode value, boolean isSelected, int row, int column) {
        super.configureEditorComponent(value, isSelected, row, column);
        if (value instanceof AbstractPackageNode) {
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
    public final String getStringValue(AbstractNode value) {
        return value.getName();
    }

}

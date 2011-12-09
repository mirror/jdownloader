package org.jdownloader.gui.views.downloads.columns;

import java.awt.event.FocusEvent;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.border.Border;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.plugins.DownloadLink;

import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.StringUtils;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class FileColumn extends ExtTextColumn<AbstractNode> {

    /**
     * 
     */
    private static final long serialVersionUID = -2963955407564917958L;
    private Border            leftGapBorder;
    private ImageIcon         iconPackageOpen;
    private ImageIcon         iconPackageClosed;

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
        if (obj instanceof CrawledPackage) { return ((CrawledPackage) obj).getView().isEnabled(); }
        return obj.isEnabled();
    }

    @Override
    public boolean isSortable(AbstractNode obj) {
        return true;
    }

    @Override
    public int getDefaultWidth() {
        return 350;
    }

    @Override
    public boolean isEditable(AbstractNode obj) {
        if (obj instanceof CrawledPackage) return true;
        if (obj instanceof CrawledLink) return true;
        return true;
    }

    protected boolean isEditable(final AbstractNode obj, final boolean enabled) {

        return isEditable(obj);
    }

    @Override
    protected void setStringValue(final String value, final AbstractNode object) {
        if (StringUtils.isEmpty(value)) return;
        if (object instanceof CrawledPackage) {
            ((CrawledPackage) object).setCustomName(value);
        } else if (object instanceof CrawledLink) {
            ((CrawledLink) object).setName(value);
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
    public void focusGained(final FocusEvent e) {
        String txt = editorField.getText();
        int point = txt.lastIndexOf(".");
        /* select filename only, try to keep the extension/filetype */
        if (point > 0) {
            editorField.select(0, point);
        } else {
            this.editorField.selectAll();
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

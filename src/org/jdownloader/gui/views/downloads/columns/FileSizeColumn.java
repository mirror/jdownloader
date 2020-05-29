package org.jdownloader.gui.views.downloads.columns;

import java.text.DecimalFormat;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtDefaultRowSorter;
import org.appwork.utils.swing.renderer.RenderLabel;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;
import org.jdownloader.settings.GraphicalUserInterfaceSettings.SIZEUNIT;

public class FileSizeColumn extends ExtColumn<AbstractNode> {
    /**
     *
     */
    private final RenderLabel   sizeRenderer;
    private final DecimalFormat formatter;
    private final String        zeroString;
    private final SIZEUNIT      maxSizeUnit;

    public JPopupMenu createHeaderPopup() {
        return FileColumn.createColumnPopup(this, getMinWidth() == getMaxWidth() && getMaxWidth() > 0);
    }

    @Override
    public boolean isDefaultVisible() {
        return false;
    }

    public FileSizeColumn() {
        super(_GUI.T.FileSizeColumn_FileSizeColumn(), null);
        this.sizeRenderer = new RenderLabel();
        this.sizeRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        this.zeroString = _GUI.T.SizeColumn_getSizeString_zero();
        maxSizeUnit = JsonConfig.create(GraphicalUserInterfaceSettings.class).getMaxSizeUnit();
        this.setRowSorter(new ExtDefaultRowSorter<AbstractNode>() {
            @Override
            public int compare(final AbstractNode o1, final AbstractNode o2) {
                final long s1 = getBytes(o1);
                final long s2 = getBytes(o2);
                if (s1 == s2) {
                    return 0;
                } else if (this.getSortOrderIdentifier() != ExtColumn.SORT_ASC) {
                    return s1 > s2 ? -1 : 1;
                } else {
                    return s1 < s2 ? -1 : 1;
                }
            }
        });
        this.formatter = new DecimalFormat("0.00");
    }

    @Override
    public void configureEditorComponent(final AbstractNode value, final boolean isSelected, final int row, final int column) {
    }

    @Override
    public void configureRendererComponent(final AbstractNode value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
        this.sizeRenderer.setText(getSizeString(getBytes(value)));
    }

    @Override
    public Object getCellEditorValue() {
        return null;
    }

    /**
     * @return
     */
    @Override
    public JComponent getEditorComponent(final AbstractNode value, final boolean isSelected, final int row, final int column) {
        return null;
    }

    /**
     * @return
     */
    @Override
    public JComponent getRendererComponent(final AbstractNode value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
        return this.sizeRenderer;
    }

    private final String getSizeString(final long fileSize) {
        if (fileSize < 0) {
            return zeroString;
        } else {
            return SIZEUNIT.formatValue(maxSizeUnit, formatter, fileSize);
        }
    }

    @Override
    protected String getTooltipText(final AbstractNode value) {
        final long sizeValue = this.getBytes(value);
        if (sizeValue < 0) {
            return _GUI.T.SizeColumn_getSizeString_zero_tt();
        } else {
            return getSizeString(sizeValue);
        }
    }

    @Override
    public boolean isEditable(final AbstractNode obj) {
        return false;
    }

    @Override
    public boolean isSortable(final AbstractNode obj) {
        return true;
    }

    @Override
    public void resetEditor() {
        // TODO Auto-generated method stub
    }

    @Override
    public void resetRenderer() {
        this.sizeRenderer.setEnabled(true);
        this.sizeRenderer.setOpaque(false);
        this.sizeRenderer.setBorder(ExtColumn.DEFAULT_BORDER);
    }

    @Override
    public void setValue(final Object value, final AbstractNode object) {
    }

    @Override
    protected boolean isDefaultResizable() {
        return false;
    }

    @Override
    public int getDefaultWidth() {
        return 70;
    }

    @Override
    public int getMinWidth() {
        return getDefaultWidth();
    }

    protected long getBytes(AbstractNode o2) {
        if (o2 instanceof CrawledPackage) {
            return ((CrawledPackage) o2).getView().getFileSize();
        } else if (o2 instanceof CrawledLink) {
            return ((CrawledLink) o2).getSize();
        } else if (o2 instanceof DownloadLink) {
            return ((DownloadLink) o2).getView().getBytesTotal();
        } else if (o2 instanceof FilePackage) {
            return ((FilePackage) o2).getView().getSize();
        } else {
            return -1;
        }
    }

    @Override
    public boolean isEnabled(AbstractNode obj) {
        if (obj instanceof CrawledPackage) {
            return ((CrawledPackage) obj).getView().isEnabled();
        } else {
            return obj.isEnabled();
        }
    }
}

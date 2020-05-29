package org.jdownloader.gui.views.downloads.columns;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;

import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtDefaultRowSorter;
import org.appwork.utils.swing.renderer.RenderLabel;
import org.jdownloader.gui.translate._GUI;

public class FileCountColumn extends ExtColumn<AbstractNode> {
    private final RenderLabel countRenderer;

    public JPopupMenu createHeaderPopup() {
        return FileColumn.createColumnPopup(this, getMinWidth() == getMaxWidth() && getMaxWidth() > 0);
    }

    public FileCountColumn() {
        super(_GUI.T.FileCountColumn_FileCountColumn(), null);
        this.countRenderer = new RenderLabel();
        this.countRenderer.setHorizontalAlignment(SwingConstants.LEFT);
        this.setRowSorter(new ExtDefaultRowSorter<AbstractNode>() {
            @Override
            public int compare(final AbstractNode o1, final AbstractNode o2) {
                final int s1 = getNumberOfItems(o1);
                final int s2 = getNumberOfItems(o2);
                if (s1 == s2) {
                    return 0;
                } else if (this.getSortOrderIdentifier() != ExtColumn.SORT_ASC) {
                    return s1 > s2 ? -1 : 1;
                } else {
                    return s1 < s2 ? -1 : 1;
                }
            }
        });
    }

    @Override
    public void configureEditorComponent(final AbstractNode value, final boolean isSelected, final int row, final int column) {
    }

    @Override
    public void configureRendererComponent(final AbstractNode value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
        this.countRenderer.setText(String.valueOf(getNumberOfItems(value)));
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
        return this.countRenderer;
    }

    @Override
    protected String getTooltipText(final AbstractNode value) {
        return String.valueOf(getNumberOfItems(value));
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
        this.countRenderer.setEnabled(true);
        this.countRenderer.setOpaque(false);
        this.countRenderer.setBorder(ExtColumn.DEFAULT_BORDER);
    }

    @Override
    public void setValue(final Object value, final AbstractNode object) {
    }

    @Override
    protected boolean isDefaultResizable() {
        return false;
    }

    @Override
    public boolean isDefaultVisible() {
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

    protected int getNumberOfItems(AbstractNode o) {
        if (o instanceof AbstractPackageNode) {
            return ((AbstractPackageNode) o).getView().size();
        } else {
            return 1;
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

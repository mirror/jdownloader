package org.jdownloader.gui.views.downloads.columns;

import java.text.DecimalFormat;
import java.text.FieldPosition;

import javax.swing.JComponent;
import javax.swing.SwingConstants;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtDefaultRowSorter;
import org.appwork.utils.swing.renderer.RenderLabel;
import org.appwork.utils.swing.renderer.RendererMigPanel;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class SizeColumn extends ExtColumn<AbstractNode> {

    /**
     * 
     */

    protected RenderLabel    sizeRenderer;
    protected long           sizeValue;
    private StringBuffer     sb;
    private DecimalFormat    formatter;
    private RenderLabel      countRenderer;
    private RendererMigPanel renderer;
    private boolean          fileCountVisible;

    public SizeColumn() {
        super(_GUI._.SizeColumn_SizeColumn(), null);
        this.sizeRenderer = new RenderLabel();

        this.sizeRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        this.countRenderer = new RenderLabel();

        this.countRenderer.setHorizontalAlignment(SwingConstants.LEFT);
        fileCountVisible = JsonConfig.create(GraphicalUserInterfaceSettings.class).isFileCountInSizeColumnVisible();
        this.renderer = new RendererMigPanel("ins 0,debug", "[]0[grow,fill]", "[grow,fill]");
        if (fileCountVisible) {
            renderer.add(countRenderer);
            renderer.add(sizeRenderer);
        } else {
            renderer.add(sizeRenderer, "spanx,pushx,growx");
        }
        this.setRowSorter(new ExtDefaultRowSorter<AbstractNode>() {
            /**
             * sorts the icon by hashcode
             */
            @Override
            public int compare(final AbstractNode o1, final AbstractNode o2) {
                final long s1 = getBytes(o1);
                final long s2 = getBytes(o2);
                if (s1 == s2) return 0;
                if (this.getSortOrderIdentifier() != ExtColumn.SORT_ASC) {
                    return s1 > s2 ? -1 : 1;
                } else {
                    return s1 < s2 ? -1 : 1;
                }
            }

        });

        this.sb = new StringBuffer();

        this.formatter = new DecimalFormat("0.00") {

            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            @Override
            public StringBuffer format(final double number, final StringBuffer result, final FieldPosition pos) {
                sb.setLength(0);
                return super.format(number, sb, pos);
            }
        };
    }

    @Override
    public void configureEditorComponent(final AbstractNode value, final boolean isSelected, final int row, final int column) {
        // TODO Auto-generated method stub

    }

    @Override
    public void configureRendererComponent(final AbstractNode value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
        if ((this.sizeValue = this.getBytes(value)) < 0) {
            this.sizeRenderer.setText(this.getInvalidValue());
        } else {
            this.sizeRenderer.setText(this.getSizeString(this.sizeValue));
        }
        if (fileCountVisible) {
            if (value instanceof AbstractPackageNode) {

                countRenderer.setText("[" + ((AbstractPackageNode) value).getView().getItems().size() + "]");
            } else {
                countRenderer.setText("");
            }
        }

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
    protected String getInvalidValue() {
        return "";
    }

    /**
     * @return
     */
    @Override
    public JComponent getRendererComponent(final AbstractNode value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
        return this.renderer;
    }

    private String getSizeString(final long fileSize) {
        if (fileSize >= 1024 * 1024 * 1024 * 1024l) { return this.formatter.format(fileSize / (1024 * 1024 * 1024 * 1024.0)) + " TiB"; }
        if (fileSize >= 1024 * 1024 * 1024l) { return this.formatter.format(fileSize / (1024 * 1024 * 1024.0)) + " GiB"; }
        if (fileSize >= 1024 * 1024l) { return this.formatter.format(fileSize / (1024 * 1024.0)) + " MiB"; }
        if (fileSize >= 1024l) { return this.formatter.format(fileSize / 1024.0) + " KiB"; }
        return fileSize + " B";
    }

    @Override
    protected String getTooltipText(final AbstractNode value) {
        if ((this.sizeValue = this.getBytes(value)) < 0) {
            return this.getInvalidValue();
        } else {
            return this.getSizeString(this.sizeValue);
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
        this.renderer.setEnabled(true);
        this.renderer.setOpaque(false);
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
        return fileCountVisible ? 90 : 70;
    }

    // @Override
    // public int getMaxWidth() {
    //
    // return 83;
    // }

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
            return ((DownloadLink) o2).getDownloadSize();
        } else if (o2 instanceof FilePackage) {
            return ((FilePackage) o2).getView().getSize();
        } else
            return -1;
    }

    @Override
    public boolean isEnabled(AbstractNode obj) {
        if (obj instanceof CrawledPackage) { return ((CrawledPackage) obj).getView().isEnabled(); }
        return obj.isEnabled();
    }

}

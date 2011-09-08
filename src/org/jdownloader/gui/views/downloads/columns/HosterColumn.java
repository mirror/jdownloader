package org.jdownloader.gui.views.downloads.columns;

import java.awt.Point;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JComponent;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForHost;

import org.appwork.app.gui.MigPanel;
import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.swing.components.tooltips.IconLabelToolTip;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtDefaultRowSorter;
import org.appwork.utils.swing.renderer.RenderLabel;
import org.appwork.utils.swing.renderer.RendererMigPanel;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.HosterToolTip;

public class HosterColumn extends ExtColumn<AbstractNode> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private int               maxIcons         = 10;
    private MigPanel          panel;
    private RenderLabel[]     labels;

    public HosterColumn() {
        super(_GUI._.HosterColumn_HosterColumn(), null);
        panel = new RendererMigPanel("ins 0 5 0 5", "[]", "[grow,fill]");
        labels = new RenderLabel[maxIcons];

        // panel.add(Box.createGlue(), "pushx,growx");
        for (int i = 0; i < maxIcons; i++) {
            labels[i] = new RenderLabel() {

                /**
                 * 
                 */
                private static final long serialVersionUID = 1L;

                @SuppressWarnings("deprecation")
                @Override
                public void show(boolean b) {
                    if (b) {
                        show();
                    } else {
                        hide();
                    }
                }

            };
            labels[i].setOpaque(false);
            labels[i].setBackground(null);
            panel.add(labels[i], "gapleft 1,hidemode 3");

        }
        panel.add(Box.createGlue(), "pushx,growx");
        setRowSorter(new ExtDefaultRowSorter<AbstractNode>() {

            /*
             * (non-Javadoc)
             * 
             * @see
             * org.appwork.swing.exttable.ExtDefaultRowSorter#compare(java.lang
             * .Object, java.lang.Object)
             */
            @Override
            public int compare(AbstractNode o1, AbstractNode o2) {
                final long l1 = getHosterCounter(o1);
                final long l2 = getHosterCounter(o2);
                if (l1 == l2) { return 0; }
                if (this.getSortOrderIdentifier() == ExtColumn.SORT_ASC) {
                    return l1 > l2 ? -1 : 1;
                } else {
                    return l1 < l2 ? -1 : 1;
                }
            }

        });

        resetRenderer();
    }

    @Override
    public Object getCellEditorValue() {
        return null;
    }

    @Override
    protected boolean isDefaultResizable() {
        return false;
    }

    @Override
    public boolean isEditable(AbstractNode obj) {
        return false;
    }

    @Override
    public boolean isEnabled(AbstractNode obj) {
        return obj.isEnabled();
    }

    public boolean isPaintWidthLockIcon() {
        return false;
    }

    @Override
    public boolean isSortable(AbstractNode obj) {
        return true;
    }

    @Override
    public void setValue(Object value, AbstractNode object) {

    }

    @Override
    public int getDefaultWidth() {
        return 65;
    }

    // @Override
    // public int getMaxWidth() {
    //
    // return 150;
    // }

    // public JToolTip createToolTip(final AbstractNode obj) {
    // if (obj instanceof DownloadLink) {
    // tip.setExtText(((DownloadLink) obj).getHost());
    // return tip;
    // } else if (obj instanceof FilePackage) {
    // tooltip.setObj(obj);
    // return tooltip;
    // }
    // return null;
    //
    // }

    public void configureRendererComponent(AbstractNode value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof FilePackage) {
            int i = 0;
            for (PluginForHost link : ((FilePackage) value).getFilePackageInfo().getIcons()) {
                if (i == maxIcons) break;
                ImageIcon icon = link.getHosterIconScaled();
                if (icon != null) {
                    labels[i].setVisible(true);
                    labels[i].setIcon(icon);
                    i++;
                }
            }
        } else if (value instanceof CrawledPackage) {
            int i = 0;
            for (PluginForHost link : ((CrawledPackage) value).getCrawledPackageInfo().getIcons()) {
                if (i == maxIcons) break;
                ImageIcon icon = link.getHosterIconScaled();
                if (icon != null) {
                    labels[i].setVisible(true);
                    labels[i].setIcon(icon);
                    i++;
                }
            }
        } else if (value instanceof DownloadLink) {
            ImageIcon icon = ((DownloadLink) value).getHosterIcon(true);
            if (icon != null) {
                labels[0].setVisible(true);
                labels[0].setIcon(icon);
            }
        } else if (value instanceof CrawledLink) {
            ImageIcon icon = ((CrawledLink) value).getHosterIcon(true);
            if (icon != null) {
                labels[0].setVisible(true);
                labels[0].setIcon(icon);
            }
        }
    }

    @Override
    public ExtTooltip createToolTip(Point position, AbstractNode obj) {
        if (obj instanceof DownloadLink) {
            return new IconLabelToolTip(((DownloadLink) obj).getHost(), ((DownloadLink) obj).getHosterIcon(true));
        } else if (obj instanceof CrawledLink) {
            return new IconLabelToolTip(((CrawledLink) obj).getHost(), ((CrawledLink) obj).getHosterIcon(true));
        } else if (obj instanceof FilePackage) { return new HosterToolTip((FilePackage) obj); }
        return null;
    }

    @Override
    public JComponent getEditorComponent(AbstractNode value, boolean isSelected, int row, int column) {
        return null;
    }

    @Override
    public JComponent getRendererComponent(AbstractNode value, boolean isSelected, boolean hasFocus, int row, int column) {
        return panel;
    }

    @Override
    public void resetEditor() {
    }

    @Override
    public void resetRenderer() {
        for (int i = 0; i < maxIcons; i++) {
            labels[i].setVisible(false);
        }
        this.panel.setOpaque(false);
        this.panel.setBackground(null);
    }

    @Override
    public void configureEditorComponent(AbstractNode value, boolean isSelected, int row, int column) {
    }

    private int getHosterCounter(AbstractNode value) {
        if (value instanceof FilePackage) {
            return ((FilePackage) value).getFilePackageInfo().getIcons().length;
        } else if (value instanceof CrawledPackage) { return ((CrawledPackage) value).getCrawledPackageInfo().getIcons().length; }
        return 1;
    }

}
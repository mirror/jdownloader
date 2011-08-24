package org.jdownloader.gui.views.downloads.columns;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JToolTip;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PackageLinkNode;

import org.appwork.app.gui.MigPanel;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.utils.swing.renderer.RenderLabel;
import org.appwork.utils.swing.renderer.RendererMigPanel;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.HosterToolTip;

public class HosterColumn extends ExtColumn<PackageLinkNode> {

    private int           maxIcons = 10;
    private MigPanel      panel;
    private RenderLabel[] labels;
    private HosterToolTip tooltip;

    public HosterColumn() {
        super(_GUI._.HosterColumn_HosterColumn(), null);
        panel = new RendererMigPanel("ins 0 5 0 5", "[]", "[grow,fill]");
        labels = new RenderLabel[maxIcons];
        // panel.add(Box.createGlue(), "pushx,growx");
        for (int i = 0; i < maxIcons; i++) {
            labels[i] = new RenderLabel();
            labels[i].setOpaque(false);
            labels[i].setBackground(null);
            panel.add(labels[i], "gapleft 1,hidemode 3");

        }
        panel.add(Box.createGlue(), "pushx,growx");
        tooltip = new HosterToolTip();
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
    public boolean isEditable(PackageLinkNode obj) {
        return false;
    }

    @Override
    public boolean isEnabled(PackageLinkNode obj) {
        return obj.isEnabled();
    }

    public boolean isPaintWidthLockIcon() {
        return false;
    }

    @Override
    public boolean isSortable(PackageLinkNode obj) {
        return false;
    }

    @Override
    public void setValue(Object value, PackageLinkNode object) {

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

    public JToolTip createToolTip(final PackageLinkNode obj) {
        if (obj instanceof DownloadLink) {
            tip.setExtText(((DownloadLink) obj).getHost());
            return tip;
        } else if (obj instanceof FilePackage) {
            tooltip.setObj(obj);
            return tooltip;
        }
        return null;

    }

    public void configureRendererComponent(PackageLinkNode value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof FilePackage) {
            int i = 0;
            for (DownloadLink link : ((FilePackage) value).getChildren()) {
                if (i == maxIcons) break;
                ImageIcon icon = link.getHosterIcon(true);
                if (icon != null) {
                    labels[i].setWorkaroundNotVisible(false);
                    labels[i].setIcon(icon);
                    i++;
                }
            }
        } else if (value instanceof DownloadLink) {
            ImageIcon icon = ((DownloadLink) value).getHosterIcon(true);
            if (icon != null) {
                labels[0].setWorkaroundNotVisible(false);
                labels[0].setIcon(icon);
            }

        }

    }

    @Override
    public JComponent getEditorComponent(PackageLinkNode value, boolean isSelected, int row, int column) {
        return null;
    }

    @Override
    public JComponent getRendererComponent(PackageLinkNode value, boolean isSelected, boolean hasFocus, int row, int column) {
        return panel;
    }

    @Override
    public void resetEditor() {
    }

    @Override
    public void resetRenderer() {
        for (int i = 0; i < maxIcons; i++) {
            labels[i].setWorkaroundNotVisible(true);
        }
        this.panel.setOpaque(false);
        this.panel.setBackground(null);
    }

    @Override
    public void configureEditorComponent(PackageLinkNode value, boolean isSelected, int row, int column) {
    }

}
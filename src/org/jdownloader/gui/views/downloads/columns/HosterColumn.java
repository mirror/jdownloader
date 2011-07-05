package org.jdownloader.gui.views.downloads.columns;

import java.util.HashMap;

import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JToolTip;

import jd.HostPluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PackageLinkNode;

import org.appwork.app.gui.MigPanel;
import org.appwork.utils.swing.renderer.RenderLabel;
import org.appwork.utils.swing.table.ExtColumn;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class HosterColumn extends ExtColumn<PackageLinkNode> {

    private static final int           SIZE = 14;
    private MigPanel                   panel;
    private RenderLabel[]              labels;
    private StringBuilder              sb;
    private HashMap<String, ImageIcon> iconCache;
    private PackageLinkNode            value;
    private HosterToolTip              tooltip;

    public HosterColumn() {
        super(_GUI._.HosterColumn_HosterColumn(), null);
        panel = new MigPanel("ins 0 5 0 5", "[]", "[grow,fill]");
        labels = new RenderLabel[10];
        // panel.add(Box.createGlue(), "pushx,growx");
        for (int i = 0; i < 10; i++) {
            labels[i] = new RenderLabel();
            labels[i].setOpaque(false);
            labels[i].setBackground(null);
            panel.add(labels[i], "gapleft 1,hidemode 3");

        }
        panel.add(Box.createGlue(), "pushx,growx");
        tooltip = new HosterToolTip();
        resetRenderer();
        sb = new StringBuilder();
        iconCache = new HashMap<String, ImageIcon>();
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

    public JToolTip createToolTip(final PackageLinkNode obj) {
        tooltip.setObj(obj);
        return tooltip;
    }

    public void configureRendererComponent(PackageLinkNode value, boolean isSelected, boolean hasFocus, int row, int column) {
        this.value = value;
        if (value instanceof FilePackage) {

            int i = 0;
            for (String hoster : DownloadLink.getHosterList(((FilePackage) value).getControlledDownloadLinks())) {
                labels[i].setVisible(true);

                labels[i].setIcon(getIcon(hoster));
                i++;
            }

        } else if (value instanceof DownloadLink) {
            labels[0].setVisible(true);
            labels[0].setIcon(getIcon(((DownloadLink) value).getHost()));

        }

    }

    private Icon getIcon(String hoster) {
        ImageIcon ret = iconCache.get(hoster);
        if (ret == null) {
            for (HostPluginWrapper hw : HostPluginWrapper.getHostWrapper()) {
                if (hoster.equalsIgnoreCase(hw.getHost())) {
                    ImageIcon icon = hw.getIconUnscaled();
                    ret = NewTheme.I().getScaledInstance(icon, SIZE);
                    iconCache.put(hoster, ret);
                    break;
                }
            }
        }
        return ret;

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
        for (int i = 0; i < 10; i++) {
            labels[i].setVisible(false);
        }
        this.panel.setOpaque(false);
        this.panel.setBackground(null);
    }

    @Override
    public void configureEditorComponent(PackageLinkNode value, boolean isSelected, int row, int column) {
    }

}
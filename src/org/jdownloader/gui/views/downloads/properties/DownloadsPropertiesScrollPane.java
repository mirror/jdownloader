package org.jdownloader.gui.views.downloads.properties;

import java.awt.Container;

import javax.swing.JComponent;

import jd.controlling.packagecontroller.AbstractNode;

import org.jdownloader.gui.components.OverviewHeaderScrollPane;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;

public class DownloadsPropertiesScrollPane extends OverviewHeaderScrollPane implements PropertiesScrollPaneInterface {

    private final DownloadPropertiesBasePanel panel;
    private DownloadPropertiesHeader          header;
    private AbstractNode                      selectedNode;

    public DownloadsPropertiesScrollPane(DownloadPropertiesBasePanel loverView, DownloadsTable table) {
        super(loverView);
        this.panel = loverView;
    }

    public void setColumnHeaderView(DownloadPropertiesHeader linkgrabberPropertiesHeader) {
        super.setColumnHeaderView(linkgrabberPropertiesHeader);
        this.header = linkgrabberPropertiesHeader;
    }

    @Override
    public void setVisible(boolean aFlag) {
        super.setVisible(aFlag);
    }

    public void update(AbstractNode objectbyRow) {
        header.update(objectbyRow);
        panel.update(objectbyRow);
        Container parent = getParent();
        if (parent != null && parent instanceof JComponent && !equals(selectedNode == null ? null : selectedNode.getClass(), objectbyRow == null ? null : objectbyRow.getClass())) {
            // revalidate the parent, because the panel hight may have changed
            ((JComponent) parent).revalidate();
        }
        selectedNode = objectbyRow;
    }

    private boolean equals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }

    public void refresh() {
    }

    public void refreshAfterTabSwitch() {
        panel.refreshAfterTabSwitch();
    }

    public void save() {
        panel.save();
    }

}

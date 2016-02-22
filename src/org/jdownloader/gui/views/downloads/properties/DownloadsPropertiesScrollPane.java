package org.jdownloader.gui.views.downloads.properties;

import java.awt.Container;
import java.util.Objects;

import javax.swing.JComponent;

import org.jdownloader.gui.components.OverviewHeaderScrollPane;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;

import jd.controlling.packagecontroller.AbstractNode;

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

        if (parent != null && parent instanceof JComponent && !Objects.equals(selectedNode == null ? null : selectedNode.getClass(), objectbyRow == null ? null : objectbyRow.getClass())) {
            // revalidate the parent, because the panel hight may have changed

            ((JComponent) parent).revalidate();
        }
        selectedNode = objectbyRow;
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

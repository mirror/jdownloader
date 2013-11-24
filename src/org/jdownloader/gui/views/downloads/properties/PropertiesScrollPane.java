package org.jdownloader.gui.views.downloads.properties;

import jd.controlling.packagecontroller.AbstractNode;

import org.jdownloader.gui.components.OverviewHeaderScrollPane;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;

public class PropertiesScrollPane extends OverviewHeaderScrollPane {

    private DownloadPropertiesBasePanel panel;
    private DownloadPropertiesHeader    header;

    public PropertiesScrollPane(DownloadPropertiesBasePanel loverView, DownloadsTable table) {
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

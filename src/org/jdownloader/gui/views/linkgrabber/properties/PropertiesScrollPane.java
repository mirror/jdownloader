package org.jdownloader.gui.views.linkgrabber.properties;

import jd.controlling.packagecontroller.AbstractNode;

import org.jdownloader.gui.components.OverviewHeaderScrollPane;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;

public class PropertiesScrollPane extends OverviewHeaderScrollPane {

    private LinkgrabberProperties       panel;
    private LinkgrabberPropertiesHeader header;

    public PropertiesScrollPane(LinkgrabberProperties loverView, LinkGrabberTable table) {
        super(loverView);
        this.panel = loverView;

    }

    public void setColumnHeaderView(LinkgrabberPropertiesHeader linkgrabberPropertiesHeader) {
        super.setColumnHeaderView(linkgrabberPropertiesHeader);
        this.header = linkgrabberPropertiesHeader;
    }

    public void update(AbstractNode objectbyRow) {
        header.update(objectbyRow);
        panel.update(objectbyRow);
    }

    @Override
    public void setVisible(boolean aFlag) {

        super.setVisible(aFlag);
        // important that save is AFTER setVisible.
        // panel.save();
    }

    public void refreshAfterTabSwitch() {
        panel.refreshAfterTabSwitch();
    }

    public void save() {
        panel.save();
    }

}

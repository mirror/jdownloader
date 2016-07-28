package org.jdownloader.gui.views.linkgrabber.properties;

import java.awt.Container;
import java.awt.Dimension;

import javax.swing.JComponent;

import jd.controlling.packagecontroller.AbstractNode;

import org.jdownloader.gui.components.OverviewHeaderScrollPane;
import org.jdownloader.gui.views.downloads.properties.PropertiesScrollPaneInterface;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;

public class LinkgrabberPropertiesScrollPane extends OverviewHeaderScrollPane implements PropertiesScrollPaneInterface {

    private final LinkgrabberProperties panel;
    private LinkgrabberPropertiesHeader header;
    private AbstractNode                selectedNode;

    public LinkgrabberPropertiesScrollPane(LinkgrabberProperties loverView, LinkGrabberTable table) {
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

    @Override
    public Dimension getPreferredSize() {
        Dimension ret = super.getPreferredSize();
        // System.out.println(panel.getPreferredSize());
        return ret;
    }

    @Override
    public void setVisible(boolean aFlag) {
        super.setVisible(aFlag);
    }

    public void refreshAfterTabSwitch() {
        panel.refreshAfterTabSwitch();
    }

    public void save() {
        panel.save();
    }

}

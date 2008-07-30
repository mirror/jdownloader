package jd.gui.skins.simple;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JScrollPane;

import jd.event.ControlEvent;
import jd.gui.skins.simple.components.treetable.DownloadTreeTable;
import jd.gui.skins.simple.components.treetable.DownloadTreeTableModel;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.utils.JDUtilities;

public class DownloadLinksTreeTablePanel extends DownloadLinksView {

    private static final long serialVersionUID = 1L;

    private DownloadTreeTable internalTreeTable;

    public DownloadLinksTreeTablePanel(SimpleGUI parent) {
        super(parent, new BorderLayout());
        this.setVisible(false);
        internalTreeTable = new DownloadTreeTable(new DownloadTreeTableModel(this));
        JScrollPane scrollPane = new JScrollPane(internalTreeTable);
        scrollPane.setPreferredSize(new Dimension(800, 450));
        this.add(scrollPane);
    }

    
    @Override
    public void fireTableChanged(int id, Object param) {
        if (id == DownloadLinksView.REFRESH_DATA_AND_STRUCTURE_CHANGED) this.setVisible(false);
        internalTreeTable.fireTableChanged(id, param);
        if (id == DownloadLinksView.REFRESH_DATA_AND_STRUCTURE_CHANGED && !this.isVisible()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            this.setVisible(true);
        }
    }

    public void moveSelectedItems(int id) {
        internalTreeTable.moveSelectedItems(id);
    }

    public void removeSelectedLinks() {
        Vector<DownloadLink> links = internalTreeTable.getSelectedDownloadLinks();
        Vector<FilePackage> fps = internalTreeTable.getSelectedFilePackages();
        for (Iterator<FilePackage> it = fps.iterator(); it.hasNext();)
            links.addAll(it.next().getDownloadLinks());
        JDUtilities.getController().removeDownloadLinks(links);
        JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, this));
    }

}

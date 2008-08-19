//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.skins.simple;

import java.awt.BorderLayout;
import java.awt.Dimension;
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
        setVisible(false);
        internalTreeTable = new DownloadTreeTable(new DownloadTreeTableModel(this));
        JScrollPane scrollPane = new JScrollPane(internalTreeTable);
        scrollPane.setPreferredSize(new Dimension(800, 450));
        this.add(scrollPane);
    }

    @Override
    public void fireTableChanged(int id, Object param) {
        if (id == DownloadLinksView.REFRESH_DATA_AND_STRUCTURE_CHANGED) {
            setVisible(false);
        }
        internalTreeTable.fireTableChanged(id, param);
        if (id == DownloadLinksView.REFRESH_DATA_AND_STRUCTURE_CHANGED && !isVisible()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            setVisible(true);
        }
    }

    public void moveSelectedItems(int id) {
        internalTreeTable.moveSelectedItems(id);
    }

    public void removeSelectedLinks() {
        Vector<DownloadLink> links = internalTreeTable.getSelectedDownloadLinks();
        Vector<FilePackage> fps = internalTreeTable.getSelectedFilePackages();
        for (FilePackage filePackage : fps) {
            links.addAll(filePackage.getDownloadLinks());
        }
        JDUtilities.getController().removeDownloadLinks(links);
        JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, this));
    }

}

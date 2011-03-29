//    jDownloader - Downloadmanager
//    Copyright (C) 2010  JD-Team support@jdownloader.org
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

package org.jdownloader.extensions.folderwatch;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import jd.gui.swing.components.table.JDTable;

public class FolderWatchTable extends JDTable implements MouseListener {

    private static final long serialVersionUID = 675244442903646117L;

    public FolderWatchTable() {
        super(new FolderWatchTableModel("folderwatch.historyview"));
        addMouseListener(this);
        setRowHeight(22);
        // setBackground(new Color(255, 255, 255));
        setGridColor(new Color(200, 200, 200));
    }

    @Override
    public FolderWatchTableModel getModel() {
        return (FolderWatchTableModel) super.getModel();
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
        FolderWatchPanel.getInstance().getInfoPanel().update();
    }

}

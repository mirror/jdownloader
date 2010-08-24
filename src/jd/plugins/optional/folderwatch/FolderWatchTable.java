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

package jd.plugins.optional.folderwatch;

import java.awt.Color;

import jd.gui.swing.components.table.JDTable;

public class FolderWatchTable extends JDTable {

    private static final long serialVersionUID = 675244442903646117L;

    public FolderWatchTable() {
        super(new FolderWatchTableModel("folderwatch.historyview"));
        setRowHeight(22);
        setBackground(new Color(255, 255, 255));
        setGridColor(new Color(200, 200, 200));
        // setShowHorizontalLines(true);
    }

    @Override
    public FolderWatchTableModel getModel() {
        return (FolderWatchTableModel) super.getModel();
    }

}

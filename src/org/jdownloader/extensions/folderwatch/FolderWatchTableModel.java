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

import java.util.ArrayList;

import org.jdownloader.extensions.folderwatch.columns.FilenameColumn;
import org.jdownloader.extensions.folderwatch.columns.FilepathColumn;
import org.jdownloader.extensions.folderwatch.columns.FiletypeColumn;
import org.jdownloader.extensions.folderwatch.columns.ImportdateColumn;
import org.jdownloader.extensions.folderwatch.data.History;
import org.jdownloader.extensions.folderwatch.data.HistoryEntry;

import jd.gui.swing.components.table.JDTableModel;
import jd.utils.locale.JDL;

public class FolderWatchTableModel extends JDTableModel {

    private static final long   serialVersionUID = 5047870839332563506L;
    private static final String JDL_PREFIX       = "jd.plugins.optional.folderwatch.tablemodel.";

    public FolderWatchTableModel(String configname) {
        super(configname);
    }

    @Override
    protected void initColumns() {
        this.addColumn(new FilenameColumn(JDL.L(JDL_PREFIX + "filename", "Filename"), this));
        this.addColumn(new FiletypeColumn(JDL.L(JDL_PREFIX + "filetype", "Container type"), this));
        this.addColumn(new FilepathColumn(JDL.L(JDL_PREFIX + "filepath", "Path"), this));
        this.addColumn(new ImportdateColumn(JDL.L(JDL_PREFIX + "importdate", "Import date"), this));
    }

    @Override
    public void refreshModel() {
        synchronized (list) {
            list.clear();
            list.addAll((ArrayList<HistoryEntry>) History.getEntries());
        }
    }
}

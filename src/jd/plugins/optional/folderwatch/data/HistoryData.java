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

package jd.plugins.optional.folderwatch.data;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

public class HistoryData extends ArrayList<HistoryDataEntry> implements Serializable {

    private static final long serialVersionUID = -4826024531349749042L;

    public HistoryData() {
    }

    public HistoryData(ArrayList<HistoryDataEntry> entries) {
        super(entries);

        updateEntries();
    }

    public void updateEntries() {
        ArrayList<HistoryDataEntry> entries = this;

        boolean value;
        for (HistoryDataEntry entry : entries) {
            if (entry.isPhysical()) {
                value = isFileExisting(entry.getAbsolutePath());
                entry.setPhysical(value);
            }
        }
    }

    private boolean isFileExisting(String path) {
        File file = new File(path);

        return file.exists();
    }

}

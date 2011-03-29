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

package org.jdownloader.extensions.folderwatch.data;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

public class History implements Serializable {

    private static final long serialVersionUID = 2752413666878389709L;

    private static ArrayList<HistoryEntry> entries = new ArrayList<HistoryEntry>();

    public static void setEntries(ArrayList<HistoryEntry> entries) {
        History.entries = entries;
    }

    public static ArrayList<HistoryEntry> getEntries() {
        return entries;
    }

    public static void add(HistoryEntry entry) {
        entries.add(entry);
    }

    public static void updateEntries() {
        for (HistoryEntry entry : entries) {
            updateEntry(entry);
        }
    }

    public static HistoryEntry updateEntry(HistoryEntry entry) {
        boolean value = isFileExisting(entry.getAbsolutePath());
        entry.setExisting(value);
        return entry;
    }

    public static void updateEntry(String filename) {
        for (HistoryEntry entry : entries) {
            if (entry.getFilename().equals(filename)) {
                updateEntry(entry);
                break;
            }
        }
    }

    public static void clear() {
        entries.clear();
    }

    private static boolean isFileExisting(String path) {
        File file = new File(path);
        return file.exists();
    }

}

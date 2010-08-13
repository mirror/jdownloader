package jd.plugins.optional.folderwatch.history;

import java.io.File;
import java.util.ArrayList;

public class FolderWatchHistory extends ArrayList<FolderWatchHistoryEntry> {

    private static final long serialVersionUID = 9207966324383256468L;

    public FolderWatchHistory() {
    }

    public FolderWatchHistory(ArrayList<FolderWatchHistoryEntry> entries) {
        super(entries);

        updateEntries();
    }

    public void updateEntries() {
        ArrayList<FolderWatchHistoryEntry> entries = this;

        boolean value;
        for (FolderWatchHistoryEntry entry : entries) {
            value = isFileExisting(entry.getAbsolutePath());
            entry.setPhysical(value);
        }
    }

    private boolean isFileExisting(String path) {
        File file = new File(path);

        return file.exists();
    }

}

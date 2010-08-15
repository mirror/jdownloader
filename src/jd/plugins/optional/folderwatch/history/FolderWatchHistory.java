package jd.plugins.optional.folderwatch.history;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

public class FolderWatchHistory extends ArrayList<FolderWatchHistoryEntry> implements Serializable {

    private static final long serialVersionUID = -4826024531349749042L;

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

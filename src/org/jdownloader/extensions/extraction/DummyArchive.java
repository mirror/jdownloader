package org.jdownloader.extensions.extraction;

import java.util.ArrayList;

public class DummyArchive {

    private final String name;
    private int          missingCount    = 0;
    private int          incompleteCount = 0;
    private final String archiveType;

    public String getArchiveType() {
        return archiveType;
    }

    public int getIncompleteCount() {
        return incompleteCount;
    }

    public int getMissingCount() {
        return missingCount;
    }

    private final java.util.List<DummyArchiveFile> list;

    public java.util.List<DummyArchiveFile> getList() {
        return list;
    }

    public DummyArchive(Archive archive, String archiveType) {
        name = archive.getName();
        this.archiveType = archiveType;
        list = new ArrayList<DummyArchiveFile>();
    }

    public void add(DummyArchiveFile e) {
        list.add(e);
        if (e.isMissing()) {
            missingCount++;
        }
        if (e.isIncomplete()) {
            incompleteCount++;
        }
    }

    public boolean isComplete() {
        return missingCount == 0 && incompleteCount == 0;
    }

    public int getSize() {
        return list.size();
    }

    public String getName() {
        return name;
    }

}

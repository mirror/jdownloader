package org.jdownloader.extensions.extraction;

import java.util.ArrayList;
import java.util.List;

public class DummyArchive {

    private Archive archive;
    private int     missingCount    = 0;
    private int     incompleteCount = 0;

    public int getIncompleteCount() {
        return incompleteCount;
    }

    private java.util.List<DummyArchiveFile> list;

    public java.util.List<DummyArchiveFile> getList() {
        return list;
    }

    public DummyArchive(Archive archive) {
        this.archive = archive;
        list = new ArrayList<DummyArchiveFile>();
    }

    public void add(DummyArchiveFile e) {
        list.add(e);
        if (e.isMissing()) missingCount++;
        if (e.isIncomplete()) incompleteCount++;

    }

    public boolean isComplete() {
        return missingCount == 0 && incompleteCount == 0;
    }

    public int getSize() {
        return list.size();
    }

    public static DummyArchive create(Archive archive2, List<String> missing) {
        DummyArchive ret = new DummyArchive(archive2);

        if (missing != null) {
            for (String miss : missing) {
                ret.add(new DummyArchiveFile(miss, archive2.getFolder()));
            }
        }
        for (ArchiveFile af : archive2.getArchiveFiles()) {
            ret.add(new DummyArchiveFile(af));
        }
        return ret;
    }

    public String getName() {
        return archive.getName();
    }

}

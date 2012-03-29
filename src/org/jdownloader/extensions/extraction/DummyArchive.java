package org.jdownloader.extensions.extraction;

import java.util.ArrayList;
import java.util.List;

public class DummyArchive {

    private Archive                     archive;
    private int                         missingCount = 0;
    private ArrayList<DummyArchiveFile> list;

    public ArrayList<DummyArchiveFile> getList() {
        return list;
    }

    public DummyArchive(Archive archive) {
        this.archive = archive;
        list = new ArrayList<DummyArchiveFile>();
    }

    public void add(DummyArchiveFile e) {
        list.add(e);
        if (!e.isExists()) missingCount++;
    }

    public boolean isComplete() {
        return missingCount == 0;
    }

    public int getSize() {
        return list.size();
    }

    public static DummyArchive create(Archive archive2, List<String> missing) {
        DummyArchive ret = new DummyArchive(archive2);

        if (missing != null) {
            for (String miss : missing) {
                ret.add(new DummyArchiveFile(miss).setExists(false));
            }
        }
        for (ArchiveFile af : archive2.getArchiveFiles()) {
            ret.add(new DummyArchiveFile(af));
        }
        return ret;
    }

}

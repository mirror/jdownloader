package org.jdownloader.extensions.extraction;

import java.util.ArrayList;

import org.jdownloader.extensions.extraction.multi.ArchiveType;
import org.jdownloader.extensions.extraction.split.SplitType;

public class DummyArchive {
    private final String      name;
    private int               missingCount    = 0;
    private int               incompleteCount = 0;
    private final ArchiveType archiveType;

    public ArchiveType getArchiveType() {
        return archiveType;
    }

    public SplitType getSplitType() {
        return splitType;
    }

    private final SplitType splitType;

    public String getType() {
        if (archiveType != null) {
            return archiveType.name();
        } else if (splitType != null) {
            return splitType.name();
        } else {
            return null;
        }
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

    public DummyArchive(Archive archive, ArchiveType archiveType) {
        name = archive.getName();
        this.archiveType = archiveType;
        this.splitType = null;
        list = new ArrayList<DummyArchiveFile>();
    }

    public DummyArchive(Archive archive, SplitType splitType) {
        name = archive.getName();
        this.splitType = splitType;
        this.archiveType = null;
        list = new ArrayList<DummyArchiveFile>();
    }

    public void add(DummyArchiveFile e) {
        list.add(e);
        if (e.isMissing()) {
            missingCount++;
        } else if (Boolean.TRUE.equals(e.isIncomplete())) {
            incompleteCount++;
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Archive:");
        sb.append(getName());
        sb.append("\r\n");
        sb.append("Type:");
        sb.append(getType());
        for (final DummyArchiveFile dummyArchiveFile : getList()) {
            sb.append("\r\n");
            if (dummyArchiveFile.isMissing()) {
                sb.append("Missing:");
            } else {
                sb.append("Existing:");
            }
            sb.append(dummyArchiveFile.toString());
        }
        sb.append("\r\n");
        sb.append("Complete:");
        sb.append(isComplete());
        return sb.toString();
    }

    public boolean isComplete() {
        return missingCount == 0 && incompleteCount == 0 && list.size() > 0;
    }

    public int getSize() {
        return list.size();
    }

    public String getName() {
        return name;
    }
}

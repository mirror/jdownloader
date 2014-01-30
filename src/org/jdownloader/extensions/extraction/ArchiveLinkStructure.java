package org.jdownloader.extensions.extraction;

import org.appwork.storage.Storable;

public class ArchiveLinkStructure implements Storable {
    public ArchiveLinkStructure(/* storable */) {

    }

    private long[]   firstFile;
    private long[][] allFiles;

    public long[] getFirstFile() {
        return firstFile;
    }

    public void setFirstFile(long[] firstFile) {
        this.firstFile = firstFile;
    }

    public long[][] getAllFiles() {
        return allFiles;
    }

    public void setAllFiles(long[][] allFiles) {
        this.allFiles = allFiles;
    }

}

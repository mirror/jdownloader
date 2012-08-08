package org.jdownloader.extensions.extraction;

import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;

import org.appwork.storage.Storable;

public class ArchiveItem implements Storable {
    private String path;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    private long    size;
    private boolean folder;

    public boolean isFolder() {
        return folder;
    }

    public void setFolder(boolean folder) {
        this.folder = folder;
    }

    private ArchiveItem(/* storable */) {

    }

    public static ArchiveItem create(ISimpleInArchiveItem item) throws SevenZipException {
        ArchiveItem ret = new ArchiveItem();
        ret.path = item.getPath();
        ret.size = item.getSize();
        ret.folder = item.isFolder();
        return ret;
    }
}

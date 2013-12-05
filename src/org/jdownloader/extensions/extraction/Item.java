package org.jdownloader.extensions.extraction;

import java.io.File;

public class Item {

    private String path;

    public String getPath() {
        return path;
    }

    public File getFile() {
        return file;
    }

    private File file;
    private long size;

    public long getSize() {
        return size;
    }

    public Item(String path, long size, File extractTo) {
        this.path = path;
        this.file = extractTo;
        this.size = size;
    }

}

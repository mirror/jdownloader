package org.jdownloader.extensions.extraction;

import java.io.File;

public class Item {

    private final String path;

    public String getPath() {
        return path;
    }

    public File getFile() {
        return file;
    }

    private final File file;
    private final long size;

    public long getSize() {
        return size;
    }

    public Item(String path, Long size, File extractTo) {
        this.path = path;
        if (size != null) {
            this.size = size;
        } else {
            this.size = -1;
        }
        this.file = extractTo;
    }

}

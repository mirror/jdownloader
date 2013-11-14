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

    public Item(String path, File extractTo) {
        this.path = path;
        this.file = extractTo;
    }

}

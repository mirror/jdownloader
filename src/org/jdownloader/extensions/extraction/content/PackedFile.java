package org.jdownloader.extensions.extraction.content;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PackedFile implements ContentNode {

    private volatile long size;

    public long getSize() {
        return size;
    }

    public synchronized List<PackedFile> list() {
        return new ArrayList<PackedFile>(children.values());
    }

    private final String                  path;
    private final boolean                 directory;
    private final Map<String, PackedFile> children = new HashMap<String, PackedFile>();

    public Map<String, PackedFile> getChildren() {
        return children;
    }

    private volatile long directorySize = 0;
    private volatile int  fileCount     = 0;

    public int getFileCount() {
        return fileCount;
    }

    public int getDirectoryCount() {
        return directoryCount;
    }

    private int directoryCount = 0;

    public long getDirectorySize() {
        return directorySize;
    }

    public PackedFile(boolean folder, String path, long size) {
        this.size = size;
        directorySize = 0l;
        this.path = path;
        directory = folder;

    }

    public boolean isDirectory() {
        return directory;
    }

    public String getParent() {
        return new File(path).getParent();
    }

    public synchronized void add(PackedFile packedFile) {
        children.put(packedFile.getName(), packedFile);
        if (!packedFile.isDirectory()) {
            directorySize += packedFile.getSize();
            fileCount++;
        } else {
            directoryCount++;
        }
    }

    public String getName() {
        return new File(path).getName();
    }

}

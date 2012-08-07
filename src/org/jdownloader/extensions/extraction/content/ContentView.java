package org.jdownloader.extensions.extraction.content;

import java.io.File;

public class ContentView extends PackedFile {

    private int totalFolderCount = 0;

    public int getTotalFolderCount() {
        return totalFolderCount;
    }

    public int getTotalFileCount() {
        return totalFileCount;
    }

    public long getTotalSize() {
        return totalSize;
    }

    private int  totalFileCount = 0;
    private long totalSize      = 0;

    public ContentView() {
        super(true, "/", -1);
    }

    @Override
    public void add(PackedFile packedFile) {
        ContentNode p = mkParent(packedFile.getParent());

        if (!packedFile.isDirectory()) {
            totalFileCount++;
            if (packedFile.getSize() > 0) totalSize += packedFile.getSize();
        } else {
            if (p.getChildren().containsKey(packedFile.getName())) {
                // we already created this folder;
                return;

            }
            totalFolderCount++;
        }
        if (p == this) {
            super.add(packedFile);
        } else {
            p.add(packedFile);
        }
    }

    private ContentNode mkParent(String parent) {
        if (parent == null) return this;
        String[] path = parent.split("[/\\\\]");
        StringBuilder cPath = new StringBuilder();
        ContentNode n = this;
        for (String s : path) {
            if (cPath.length() > 0) cPath.append(File.separatorChar);
            cPath.append(s);
            PackedFile ret = n.getChildren().get(s);
            if (ret == null) {
                ret = new PackedFile(true, cPath.toString(), -1);
                totalFolderCount++;
                n.getChildren().put(s, ret);
            }
            n = ret;
        }
        return n;
    }

}

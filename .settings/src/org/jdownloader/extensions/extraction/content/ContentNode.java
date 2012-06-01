package org.jdownloader.extensions.extraction.content;

import java.util.List;
import java.util.Map;

public interface ContentNode {
    public long getDirectorySize();

    public Map<String, PackedFile> getChildren();

    public void add(PackedFile packedFile);

    public List<PackedFile> list();
}

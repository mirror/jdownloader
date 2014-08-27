package org.jdownloader.extensions.streaming.mediaarchive;

import javax.swing.Icon;

public interface MediaNode {
    public String getName();

    public long getSize();

    public Icon getIcon();

    public String getUniqueID();

    public void setRoot(MediaRoot root);

    public void setParent(MediaFolder mediaFolder);

    public String getThumbnailPath();
}

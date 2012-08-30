package org.jdownloader.extensions.streaming.mediaarchive;

import javax.swing.ImageIcon;

public interface MediaNode {
    public String getName();

    public long getSize();

    public ImageIcon getIcon();

    public String getUniqueID();

    public void setRoot(MediaRoot root);

    public void setParent(MediaFolder mediaFolder);
}

package org.jdownloader.extensions.streaming.mediaarchive;

import jd.controlling.packagecontroller.AbstractPackageChildrenNode;

import org.jdownloader.DomainInfo;

public class MediaItem implements MediaNode, AbstractPackageChildrenNode<MediaFolder> {

    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void setEnabled(boolean b) {
    }

    @Override
    public long getCreated() {
        return 0;
    }

    @Override
    public long getFinishedDate() {
        return 0;
    }

    @Override
    public MediaFolder getParentNode() {
        return null;
    }

    @Override
    public void setParentNode(MediaFolder parent) {
    }

    @Override
    public DomainInfo getDomainInfo() {
        return null;
    }

}

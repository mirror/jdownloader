package org.jdownloader.extensions.streaming.mediaarchive;

import java.util.List;

import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.controlling.packagecontroller.ChildComparator;
import jd.controlling.packagecontroller.ChildrenView;
import jd.controlling.packagecontroller.PackageController;

public class MediaFolder implements MediaNode, AbstractPackageNode<MediaItem, MediaFolder> {

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
    public void nodeUpdated(MediaItem source, jd.controlling.packagecontroller.AbstractNodeNotifier.NOTIFY notify) {
    }

    @Override
    public PackageController<MediaFolder, MediaItem> getControlledBy() {
        return null;
    }

    @Override
    public void setControlledBy(PackageController<MediaFolder, MediaItem> controller) {
    }

    @Override
    public List<MediaItem> getChildren() {
        return null;
    }

    @Override
    public void sort() {
    }

    @Override
    public void setCurrentSorter(ChildComparator<MediaItem> comparator) {
    }

    @Override
    public ChildComparator<MediaItem> getCurrentSorter() {
        return null;
    }

    @Override
    public ChildrenView<MediaItem> getView() {
        return null;
    }

    @Override
    public boolean isExpanded() {
        return false;
    }

    @Override
    public void setExpanded(boolean b) {
    }

    @Override
    public int indexOf(MediaItem child) {
        return 0;
    }

}

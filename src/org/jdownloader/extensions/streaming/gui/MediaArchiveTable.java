package org.jdownloader.extensions.streaming.gui;

import jd.controlling.packagecontroller.AbstractNode;

import org.appwork.swing.exttable.ExtColumn;
import org.jdownloader.extensions.streaming.mediaarchive.MediaFolder;
import org.jdownloader.extensions.streaming.mediaarchive.MediaItem;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;

public class MediaArchiveTable extends PackageControllerTable<MediaFolder, MediaItem> {

    public MediaArchiveTable(MediaArchiveTableModel model) {
        super(model);
    }

    @Override
    public ExtColumn<AbstractNode> getExpandCollapseColumn() {
        return null;
    }

}

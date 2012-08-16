package org.jdownloader.extensions.streaming.gui;

import jd.controlling.packagecontroller.AbstractNode;

import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.jdownloader.extensions.streaming.MediaArchiveController;
import org.jdownloader.extensions.streaming.mediaarchive.MediaFolder;
import org.jdownloader.extensions.streaming.mediaarchive.MediaItem;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModel;

public class MediaArchiveTableModel extends PackageControllerTableModel<MediaFolder, MediaItem> {

    public MediaArchiveTableModel(MediaArchiveController mediaArchiveController) {
        super(mediaArchiveController, "MediaArchiveTableModel");
    }

    @Override
    protected void initColumns() {
        addColumn(new ExtTextColumn<AbstractNode>("Test") {

            @Override
            public String getStringValue(AbstractNode value) {
                return value + "";
            }
        });
    }

}

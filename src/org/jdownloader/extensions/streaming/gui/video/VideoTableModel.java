package org.jdownloader.extensions.streaming.gui.video;

import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.jdownloader.extensions.streaming.gui.MediaTableModel;
import org.jdownloader.extensions.streaming.mediaarchive.MediaArchiveController;
import org.jdownloader.extensions.streaming.mediaarchive.VideoListController;
import org.jdownloader.extensions.streaming.mediaarchive.VideoMediaItem;

public class VideoTableModel extends MediaTableModel<VideoMediaItem, VideoListController> {

    public VideoTableModel(MediaArchiveController mediaArchiveController) {
        super("VideoTableModel", mediaArchiveController.getVideoController());

    }

    @Override
    protected void initColumns() {
        addColumn(new ExtTextColumn<VideoMediaItem>("Test") {

            @Override
            public String getStringValue(VideoMediaItem value) {
                return value + "";
            }
        });
    }

}

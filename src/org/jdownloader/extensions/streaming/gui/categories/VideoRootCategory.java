package org.jdownloader.extensions.streaming.gui.categories;

import javax.swing.JComponent;

import org.jdownloader.extensions.streaming.StreamingExtension;
import org.jdownloader.extensions.streaming.T;
import org.jdownloader.extensions.streaming.gui.video.VideoTable;
import org.jdownloader.extensions.streaming.gui.video.VideoTableModel;

public class VideoRootCategory extends RootCategory {

    private VideoTableModel model;
    private VideoTable      table;

    public VideoRootCategory(StreamingExtension plg) {
        super(plg, T._.SettingsSidebarModel_video(), "video");
        model = new VideoTableModel(plg.getMediaArchiveController());
        table = new VideoTable(model);
    }

    @Override
    public JComponent getView() {
        return table;
    }

}

package org.jdownloader.extensions.streaming.gui.categories;

import javax.swing.JComponent;

import org.jdownloader.extensions.streaming.StreamingExtension;
import org.jdownloader.extensions.streaming.T;
import org.jdownloader.extensions.streaming.gui.audio.AudioTable;
import org.jdownloader.extensions.streaming.gui.audio.AudioTableModel;

public class AudioRootCategory extends RootCategory {

    private AudioTableModel model;
    private AudioTable      table;

    public AudioRootCategory(StreamingExtension plg) {
        super(plg, T._.SettingsSidebarModel_audio(), "audio");
        model = new AudioTableModel(plg, plg.getMediaArchiveController());
        table = new AudioTable(model);
    }

    @Override
    public JComponent getView() {
        return table;

    }

}

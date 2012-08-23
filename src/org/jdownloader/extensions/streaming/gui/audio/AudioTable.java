package org.jdownloader.extensions.streaming.gui.audio;

import org.jdownloader.extensions.streaming.gui.MediaTable;
import org.jdownloader.extensions.streaming.mediaarchive.AudioMediaItem;

public class AudioTable extends MediaTable<AudioMediaItem> {

    public AudioTable(AudioTableModel model) {
        super(model);
        this.setRowHeight(32);
    }

}

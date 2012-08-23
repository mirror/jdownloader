package org.jdownloader.extensions.streaming.gui.image;

import org.jdownloader.extensions.streaming.gui.MediaTable;
import org.jdownloader.extensions.streaming.mediaarchive.ImageMediaItem;

public class ImageTable extends MediaTable<ImageMediaItem> {

    public ImageTable(ImageTableModel model) {
        super(model);
        this.setRowHeight(32);
    }

}

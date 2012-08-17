package org.jdownloader.extensions.streaming.gui;

import jd.gui.swing.jdgui.BasicJDTable;

import org.appwork.swing.exttable.ExtTableModel;
import org.jdownloader.extensions.streaming.mediaarchive.MediaItem;

public class MediaTable<T extends MediaItem> extends BasicJDTable<T> {

    public MediaTable(ExtTableModel<T> tableModel) {
        super(tableModel);
    }

}

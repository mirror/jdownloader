package org.jdownloader.extensions.streaming.gui;

import jd.gui.swing.jdgui.BasicJDTable;

import org.jdownloader.extensions.streaming.mediaarchive.prepare.PrepareJob;

public class MediaArchivePrepareTable extends BasicJDTable<PrepareJob> {

    public MediaArchivePrepareTable(MediaArchivePrepareTableModel model) {
        super(model);

    }

}

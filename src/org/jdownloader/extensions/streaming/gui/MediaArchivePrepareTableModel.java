package org.jdownloader.extensions.streaming.gui;

import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.jdownloader.extensions.streaming.mediaarchive.MediaArchiveController;
import org.jdownloader.extensions.streaming.mediaarchive.MediaArchiveListener;
import org.jdownloader.extensions.streaming.mediaarchive.prepare.PrepareJob;

public class MediaArchivePrepareTableModel extends ExtTableModel<PrepareJob> implements MediaArchiveListener {

    private MediaArchiveController controller;

    public MediaArchivePrepareTableModel(MediaArchiveController mediaArchiveController) {
        super("MediaArchivePrepareTableModel");
        controller = mediaArchiveController;
        controller.getEventSender().addListener(this, true);
    }

    @Override
    protected void initColumns() {
        addColumn(new ExtTextColumn<PrepareJob>("Test") {

            @Override
            public String getStringValue(PrepareJob value) {
                return value + "";
            }
        });
    }

    @Override
    public void onPrepareQueueUpdated(MediaArchiveController caller) {
        System.out.println("Jobs: " + caller.getPreparerJobs().size());
        this._fireTableStructureChanged(caller.getPreparerJobs(), true);
    }

}

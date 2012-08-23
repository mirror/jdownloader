package org.jdownloader.extensions.streaming.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.jdownloader.extensions.streaming.mediaarchive.MediaArchiveController;
import org.jdownloader.extensions.streaming.mediaarchive.MediaArchiveListener;
import org.jdownloader.extensions.streaming.mediaarchive.prepare.PrepareJob;

public class MediaArchivePrepareTableModel extends ExtTableModel<PrepareJob> implements MediaArchiveListener, ActionListener {

    private MediaArchiveController controller;
    private Timer                  timer;

    public MediaArchivePrepareTableModel(MediaArchiveController mediaArchiveController) {
        super("MediaArchivePrepareTableModel");
        controller = mediaArchiveController;
        controller.getEventSender().addListener(this, true);

    }

    @Override
    protected void initColumns() {
        addColumn(new ExtTextColumn<PrepareJob>("Job") {

            @Override
            public String getStringValue(PrepareJob value) {
                return value.getName();
            }
        });
        addColumn(new ExtTextColumn<PrepareJob>("Status") {

            @Override
            public String getStringValue(PrepareJob value) {
                return value.getStatus();
            }
        });
    }

    @Override
    public void onPrepareQueueUpdated(MediaArchiveController caller) {
        System.out.println("Jobs: " + caller.getPreparerJobs().size());
        this._fireTableStructureChanged(caller.getPreparerJobs(), true);
        // TODO:synchronize start/stop
        synchronized (this) {

            if (caller.isPreparerQueueEmpty()) {
                if (timer != null) {

                    timer.stop();
                    timer = null;
                }

            } else {
                timer = new Timer(1000, this);
                timer.setRepeats(true);
                timer.start();
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        fireTableDataChanged();
    }

}

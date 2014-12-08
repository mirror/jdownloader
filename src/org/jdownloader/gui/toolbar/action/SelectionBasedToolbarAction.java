package org.jdownloader.gui.toolbar.action;

import jd.gui.swing.jdgui.interfaces.View;

import org.appwork.swing.exttable.ExtTableEvent;
import org.appwork.swing.exttable.ExtTableListener;
import org.jdownloader.gui.event.GUIListener;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;
import org.jdownloader.gui.views.downloads.DownloadsView;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTableModel;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberView;

public abstract class SelectionBasedToolbarAction extends AbstractToolBarAction implements GUIListener, ExtTableListener {

    private PackageControllerTable<?, ?> table;

    public SelectionBasedToolbarAction() {
        super();

    }

    @Override
    public void onGuiMainTabSwitch(View oldView, View newView) {
        try {
            LinkGrabberTableModel.getInstance().getTable().getEventSender().removeListener(this);
            DownloadsTableModel.getInstance().getTable().getEventSender().removeListener(this);

            if (newView instanceof LinkGrabberView) {
                table = LinkGrabberTableModel.getInstance().getTable();
                setEnabled(true);
            } else if (newView instanceof DownloadsView) {
                table = DownloadsTableModel.getInstance().getTable();
                setEnabled(true);
            } else {
                table = null;
                setEnabled(false);
            }
            if (table != null) {
                table.getEventSender().addListener(this, true);
            }
            onSelectionUpdate();
        } catch (Exception e) {
            onSelectionUpdate();
            setEnabled(false);
        }

        super.onGuiMainTabSwitch(oldView, newView);

    }

    @Override
    public void onExtTableEvent(ExtTableEvent<?> event) {
        switch (event.getType()) {
        case SELECTION_CHANGED:

            onSelectionUpdate();

        }

    }

    public PackageControllerTable<?, ?> getTable() {
        return table;
    }

    protected abstract void onSelectionUpdate();

}

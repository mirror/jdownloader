package org.jdownloader.gui.toolbar.action;

import java.util.List;

import jd.controlling.packagecontroller.AbstractNode;
import jd.gui.swing.jdgui.MainTabbedPane;
import jd.gui.swing.jdgui.interfaces.View;

import org.appwork.swing.exttable.ExtTableEvent;
import org.appwork.swing.exttable.ExtTableListener;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.gui.event.GUIEventSender;
import org.jdownloader.gui.event.GUIListener;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;
import org.jdownloader.gui.views.downloads.DownloadsView;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTableModel;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberView;

public abstract class SelectionBasedToolbarAction extends ToolBarAction implements GUIListener, ExtTableListener {

    private PackageControllerTable<?, ?> table;

    public SelectionBasedToolbarAction() {
        super();
        GUIEventSender.getInstance().addListener(this, true);
        onGuiMainTabSwitch(null, MainTabbedPane.getInstance().getSelectedView());
    }

    @Override
    public void onGuiMainTabSwitch(View oldView, View newView) {

        try {
            LinkGrabberTableModel.getInstance().getTable().getEventSender().removeListener(this);
            DownloadsTableModel.getInstance().getTable().getEventSender().removeListener(this);

            if (newView instanceof LinkGrabberView) {
                table = LinkGrabberTableModel.getInstance().getTable();

            } else if (newView instanceof DownloadsView) {
                table = DownloadsTableModel.getInstance().getTable();

            } else {
                table = null;
                setEnabled(false);
            }
            if (table != null) {

                table.getEventSender().addListener(this, true);
            }
            onSelectionUpdate(table == null ? null : table.getModel().getSelectedObjects());
        } catch (Exception e) {
            onSelectionUpdate(table == null ? null : table.getModel().getSelectedObjects());
            setEnabled(false);
        }

        if (isshowInAllViews()) {
            setVisible(true);
            return;
        }
        if (isShowInDownloadView() && table != null && table instanceof DownloadsTable) {
            setVisible(true);
            return;
        }
        if (isshowInLinkgrabberView() && table != null && table instanceof LinkGrabberTable) {
            setVisible(true);
            return;
        }
        setVisible(false);
    }

    @Override
    public void onExtTableEvent(ExtTableEvent<?> event) {
        switch (event.getType()) {
        case SELECTION_CHANGED:
            onSelectionUpdate(table == null ? null : table.getModel().getSelectedObjects());
        }

    }

    public PackageControllerTable<?, ?> getTable() {
        return table;
    }

    protected abstract void onSelectionUpdate(List<AbstractNode> list);

    private boolean showInAllViews        = true;

    private boolean showInDownloadView    = true;

    private boolean showInLinkgrabberView = true;

    @Customizer(name = "Show in all Views")
    public boolean isshowInAllViews() {
        return showInAllViews;
    }

    @Customizer(name = "Show in Download view")
    public boolean isShowInDownloadView() {
        return showInDownloadView;
    }

    @Customizer(name = "Show in Linkgrabber view")
    public boolean isshowInLinkgrabberView() {
        return showInLinkgrabberView;
    }

    public void setshowInAllViews(boolean clearListAfterConfirm) {
        this.showInAllViews = clearListAfterConfirm;
    }

    public void setShowInDownloadView(boolean clearListAfterConfirm) {
        this.showInDownloadView = clearListAfterConfirm;
    }

    public void setshowInLinkgrabberView(boolean clearListAfterConfirm) {
        this.showInLinkgrabberView = clearListAfterConfirm;
    }
}

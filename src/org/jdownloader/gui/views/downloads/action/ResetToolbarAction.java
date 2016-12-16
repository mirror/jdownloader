package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;

import jd.gui.swing.jdgui.MainTabbedPane;
import jd.gui.swing.jdgui.interfaces.View;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.swing.exttable.ExtTableEvent;
import org.appwork.swing.exttable.ExtTableListener;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.event.GUIEventSender;
import org.jdownloader.gui.toolbar.action.AbstractToolBarAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.DownloadsView;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;

public class ResetToolbarAction extends AbstractToolBarAction implements ExtTableListener {
    /**
     *
     */
    private static final long   serialVersionUID = 1L;
    private final static String NAME             = _GUI.T.gui_table_contextmenu_reset();

    public ResetToolbarAction() {
        setIconKey(IconKey.ICON_UNDO);
        setName(NAME);
        GUIEventSender.getInstance().addListener(this, true);
        onGuiMainTabSwitch(null, MainTabbedPane.getInstance().getSelectedView());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (isEnabled()) {
            final SelectionInfo<FilePackage, DownloadLink> rawSelection = DownloadsTable.getInstance().getSelectionInfo();
            ResetAction.reset(rawSelection.getChildren());
        }
    }

    @Override
    public void onGuiMainTabSwitch(View oldView, final View newView) {
        if (newView instanceof DownloadsView) {
            DownloadsTableModel.getInstance().getTable().getEventSender().addListener(this, true);
            updateState();
        } else {
            DownloadsTableModel.getInstance().getTable().getEventSender().removeListener(this);
            setEnabled(false);
        }
    }

    private void updateState() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                setEnabled(!DownloadsTable.getInstance().getSelectionInfo().isEmpty());
            }
        };
    }

    @Override
    protected String createTooltip() {
        return NAME;
    }

    @Override
    public void onExtTableEvent(ExtTableEvent<?> event) {
        switch (event.getType()) {
        case SELECTION_CHANGED:
            updateState();
            break;
        default:
            break;
        }
    }
}

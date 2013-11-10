package org.jdownloader.gui.views.components.packagetable.context;

import java.awt.event.ActionEvent;

import javax.swing.KeyStroke;

import jd.gui.swing.jdgui.MainTabbedPane;
import jd.gui.swing.jdgui.interfaces.View;

import org.jdownloader.controlling.contextmenu.CustomizableSelectionAppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.DownloadsPanel;
import org.jdownloader.gui.views.downloads.DownloadsView;
import org.jdownloader.gui.views.downloads.columns.FileColumn;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberPanel;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberView;

public class RenameAction extends CustomizableSelectionAppAction {

    public RenameAction() {

        super();
        setName(_GUI._.RenameAction_RenameAction());
        setTooltipText(_GUI._.RenameAction_RenameAction_tt());
        setIconKey("edit");
        setAccelerator(KeyStroke.getKeyStroke("F2"));

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            // LinkgrabberContextMenuManager.getInstance().getPanel().getTable().get

            View view = MainTabbedPane.getInstance().getSelectedView();

            if (view instanceof DownloadsView) {
                DownloadsTable table = ((DownloadsPanel) ((DownloadsView) view).getContent()).getTable();
                FileColumn col = table.getModel().getColumnByClass(FileColumn.class);

                col.startEditing(hasSelection() && getSelection().getRawContext() != null ? getSelection().getRawContext() : table.getModel().getObjectbyRow(table.getSelectionModel().getLeadSelectionIndex()));
            } else if (view instanceof LinkGrabberView) {
                LinkGrabberTable table = ((LinkGrabberPanel) ((LinkGrabberView) view).getContent()).getTable();
                FileColumn col = table.getModel().getColumnByClass(FileColumn.class);
                col.startEditing(hasSelection() && getSelection().getRawContext() != null ? getSelection().getRawContext() : table.getModel().getObjectbyRow(table.getSelectionModel().getLeadSelectionIndex()));

            }

        } catch (Exception ee) {
            // many casts here.... let's catch everything - just to be sure

        }

    }

}

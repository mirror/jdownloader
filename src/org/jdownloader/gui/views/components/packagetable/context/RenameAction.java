package org.jdownloader.gui.views.components.packagetable.context;

import java.awt.event.ActionEvent;

import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.packagetable.context.rename.RenameDialog;
import org.jdownloader.gui.views.downloads.DownloadsPanel;
import org.jdownloader.gui.views.downloads.DownloadsView;
import org.jdownloader.gui.views.downloads.columns.FileColumn;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberPanel;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberView;
import org.jdownloader.translate._JDT;

import jd.gui.swing.jdgui.MainTabbedPane;
import jd.gui.swing.jdgui.interfaces.View;

public class RenameAction extends CustomizableTableContextAppAction implements ActionContext {

    public RenameAction() {

        super();
        setName(_GUI.T.RenameAction_RenameAction());
        setTooltipText(_GUI.T.RenameAction_RenameAction_tt());
        setIconKey(IconKey.ICON_EDIT);
        setAccelerator(KeyStroke.getKeyStroke("F2"));

    }

    private boolean simpleMode = false;

    public static String getTranslationForSimpleMode() {
        return _JDT.T.RenameAction_getTranslationForSimpleMode();
    }

    @Customizer(link = "#getTranslationForSimpleMode")
    public boolean isSimpleMode() {
        return simpleMode;
    }

    public void setSimpleMode(boolean simpleMode) {
        this.simpleMode = simpleMode;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (isSimpleMode() || ((getSelection().getRawSelection().size() == 1 && !(e.getSource() instanceof JMenuItem)))) {
            // do not edit in table if the user clicked on the menu item, but do it if the user uses a shortcut (source is the table in this
            // case)
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
        } else if (hasSelection()) {

            RenameDialog d = new RenameDialog(getSelection());

            try {
                Dialog.getInstance().showDialog(d);
            } catch (DialogClosedException e1) {
                e1.printStackTrace();
            } catch (DialogCanceledException e1) {
                e1.printStackTrace();
            }

        }

    }
}

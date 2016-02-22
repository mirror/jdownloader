package org.jdownloader.gui.views.components.packagetable.context;

import java.awt.event.ActionEvent;

import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.packagecontroller.PackageController;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;
import org.jdownloader.gui.views.components.packagetable.context.rename.RenameDialog;
import org.jdownloader.gui.views.downloads.columns.FileColumn;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.translate._JDT;

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
        final SelectionInfo selection = getSelection();
        if (isSimpleMode() || (selection.getRawSelection().size() == 1 && (!(e.getSource() instanceof JMenuItem) || selection.isPackageContext()))) {
            // do not edit in table if the user clicked on the menu item, but do it if the user uses a shortcut (source is the table in this
            // case)
            try {
                final PackageController controller = selection.getController();
                final PackageControllerTable table;
                if (DownloadController.getInstance() == controller) {
                    table = DownloadsTable.getInstance();
                } else if (LinkCollector.getInstance() == controller) {
                    table = LinkGrabberTable.getInstance();
                } else {
                    return;
                }
                final ExtColumn col = table.getModel().getColumnByClass(FileColumn.class);
                col.startEditing(selection.getRawContext());
            } catch (Exception ee) {
                // many casts here.... let's catch everything - just to be sure
            }
        } else if (hasSelection(selection)) {
            final RenameDialog d = new RenameDialog(selection);
            try {
                Dialog.getInstance().showDialog(d);
            } catch (DialogNoAnswerException e1) {
            }
        }

    }
}

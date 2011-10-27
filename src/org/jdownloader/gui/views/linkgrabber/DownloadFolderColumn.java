package org.jdownloader.gui.views.linkgrabber;

import java.awt.event.MouseEvent;

import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;

import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.Dialog.FileChooserSelectionMode;
import org.appwork.utils.swing.dialog.Dialog.FileChooserType;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.GeneralSettings;

public class DownloadFolderColumn extends ExtTextColumn<AbstractNode> {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public DownloadFolderColumn() {
        super(_GUI._.LinkGrabberTableModel_initColumns_folder());
    }

    @Override
    public boolean isEditable(AbstractNode obj) {

        return true;
    }

    @Override
    protected void onSingleClick(MouseEvent e, AbstractNode obj) {
        super.onSingleClick(e, obj);
    }

    @Override
    protected void onDoubleClick(MouseEvent e, AbstractNode obj) {
        // setFolder
        try {
            Dialog.getInstance().showFileChooser("test", "Choose file", FileChooserSelectionMode.FILES_AND_DIRECTORIES, null, false, FileChooserType.OPEN_DIALOG, null);
        } catch (DialogCanceledException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (DialogClosedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    @Override
    public boolean isSortable(AbstractNode obj) {
        return true;
    }

    @Override
    protected void setStringValue(String value, AbstractNode object) {
        // set Folder
    }

    @Override
    public String getStringValue(AbstractNode value) {
        if (value instanceof CrawledPackage) {
            return GeneralSettings.DOWNLOAD_FOLDER.getValue();
        } else {
            return null;
        }
    }

}

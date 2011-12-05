package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.filechooser.FileFilter;

import org.appwork.storage.JSonStorage;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.Dialog.FileChooserSelectionMode;
import org.appwork.utils.swing.dialog.Dialog.FileChooserType;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.filter.LinkgrabberFilterRule;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.translate._JDT;

public class ExportAction extends AppAction {
    /**
     * 
     */
    private static final long                serialVersionUID = 1L;
    private ArrayList<LinkgrabberFilterRule> rules;

    private LinkgrabberFilter                linkgrabberFilter;

    public ExportAction(LinkgrabberFilter linkgrabberFilter) {
        setName(_GUI._.LinkgrabberFilter_LinkgrabberFilter_export());
        setIconKey("export");
        setTooltipText(_JDT._.ExportAction_ExportAction_tt());
        this.linkgrabberFilter = linkgrabberFilter;

    }

    public boolean isEnabled() {
        return rules == null || rules.size() > 0;
    }

    public ExportAction(ArrayList<LinkgrabberFilterRule> selection) {
        setName(_GUI._.LinkgrabberFilter_LinkgrabberFilter_export());
        setIconKey("export");
        setTooltipText(_JDT._.ExportAction_ExportAction_tt());
        rules = selection;
    }

    public void actionPerformed(ActionEvent e) {
        try {

            ArrayList<LinkgrabberFilterRule> exportList = rules;

            if (exportList == null) {
                if (linkgrabberFilter == null) return;
                exportList = linkgrabberFilter.getView().getExtTableModel().getTableData();
            }
            if (exportList.size() == 0) return;
            final String ext = exportList.get(0).isAccept() ? ImportAction.VIEW : ImportAction.EXT;
            File[] filterFiles = Dialog.getInstance().showFileChooser(ext, _GUI._.LinkgrabberFilter_export_dialog_title(), FileChooserSelectionMode.FILES_ONLY, new FileFilter() {

                @Override
                public String getDescription() {

                    return "*" + ext;

                }

                @Override
                public boolean accept(File f) {
                    return f.isDirectory() || StringUtils.endsWithCaseInsensitive(f.getName(), ext);

                }
            }, true, FileChooserType.SAVE_DIALOG, null);

            String str = JSonStorage.toString(exportList);
            File saveto = filterFiles[0];
            if (!saveto.getName().endsWith(ext)) {
                saveto = new File(saveto.getAbsolutePath() + ext);
            }
            try {
                IO.writeStringToFile(saveto, str);
            } catch (IOException e1) {
                Dialog.getInstance().showExceptionDialog(e1.getMessage(), e1.getMessage(), e1);
            }

        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        }
    }
}
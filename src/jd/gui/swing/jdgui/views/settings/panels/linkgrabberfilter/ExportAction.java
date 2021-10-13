package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.filechooser.FileFilter;

import org.appwork.storage.JSonStorage;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.appwork.utils.swing.dialog.ExtFileChooserDialog;
import org.appwork.utils.swing.dialog.FileChooserSelectionMode;
import org.appwork.utils.swing.dialog.FileChooserType;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.filter.LinkgrabberFilterRule;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.logging.LogController;
import org.jdownloader.translate._JDT;

public class ExportAction extends AppAction {
    /**
     *
     */
    private static final long                 serialVersionUID = 1L;
    private final List<LinkgrabberFilterRule> rules;
    private final LinkgrabberFilter           linkgrabberFilter;

    public boolean isEnabled() {
        return rules == null || rules.size() > 0;
    }

    public ExportAction(LinkgrabberFilter linkgrabberFilter, java.util.List<LinkgrabberFilterRule> selection) {
        setName(_GUI.T.LinkgrabberFilter_LinkgrabberFilter_export());
        setIconKey(IconKey.ICON_EXPORT);
        setTooltipText(_JDT.T.ExportAction_ExportAction_tt());
        rules = selection;
        this.linkgrabberFilter = linkgrabberFilter;
    }

    public void actionPerformed(ActionEvent e) {
        try {
            List<LinkgrabberFilterRule> export = rules;
            if (export == null) {
                export = linkgrabberFilter.getTable().getModel().getTableData();
            }
            if (export == null || export.size() == 0) {
                return;
            }
            final String ext = export.get(0).isAccept() ? ImportAction.VIEW : ImportAction.EXT;
            final ExtFileChooserDialog d = new ExtFileChooserDialog(0, _GUI.T.LinkgrabberFilter_export_dialog_title(), null, null);
            d.setFileSelectionMode(FileChooserSelectionMode.FILES_ONLY);
            d.setFileFilter(new FileFilter() {
                @Override
                public String getDescription() {
                    return "*" + ext;
                }

                @Override
                public boolean accept(File f) {
                    return f.isDirectory() || StringUtils.endsWithCaseInsensitive(f.getName(), ext);
                }
            });
            d.setType(FileChooserType.SAVE_DIALOG);
            d.setMultiSelection(false);
            Dialog.I().showDialog(d);
            File saveto = d.getSelectedFile();
            if (saveto != null) {
                if (!saveto.getName().endsWith(ext)) {
                    saveto = new File(saveto.getAbsolutePath() + ext);
                }
                try {
                    if (saveto.exists() && !saveto.delete()) {
                        throw new IOException("Could not delete/overwrite:" + saveto);
                    }
                    IO.writeStringToFile(saveto, JSonStorage.serializeToJson(export));
                } catch (IOException e1) {
                    LogController.CL().log(e1);
                    Dialog.getInstance().showExceptionDialog(_GUI.T.lit_error_occured(), e1.getMessage(), e1);
                }
            }
        } catch (DialogNoAnswerException e1) {
            LogController.CL().log(e1);
        }
    }
}
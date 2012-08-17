package jd.gui.swing.jdgui.views.settings.panels.packagizer;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.filechooser.FileFilter;

import org.appwork.storage.JSonStorage;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ExtFileChooserDialog;
import org.appwork.utils.swing.dialog.FileChooserSelectionMode;
import org.appwork.utils.swing.dialog.FileChooserType;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.controlling.packagizer.PackagizerRule;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.translate._JDT;

public class ExportAction extends AppAction {
    /**
     * 
     */
    private static final long         serialVersionUID = 1L;
    private java.util.List<PackagizerRule> rules;

    public ExportAction() {
        setName(_GUI._.LinkgrabberFilter_LinkgrabberFilter_export());
        setIconKey("export");
        setTooltipText(_JDT._.ExportAction_ExportAction_tt());

    }

    public boolean isEnabled() {
        return rules != null && rules.size() > 0;
    }

    public ExportAction(java.util.List<PackagizerRule> selection) {
        this();

        rules = selection;
    }

    public void actionPerformed(ActionEvent e) {
        try {
            final String ext;

            ext = ImportAction.EXT;

            ExtFileChooserDialog d = new ExtFileChooserDialog(0, _GUI._.LinkgrabberFilter_export_dialog_title(), null, null);
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

            if (rules == null) {
                rules = PackagizerController.getInstance().list();
            }
            String str = JSonStorage.toString(rules);
            File saveto = d.getSelectedFile();
            if (saveto == null) return;
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
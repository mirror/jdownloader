package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.filechooser.FileFilter;

import jd.gui.swing.jdgui.views.settings.panels.packagizer.ImportAction;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.Dialog.FileChooserSelectionMode;
import org.appwork.utils.swing.dialog.Dialog.FileChooserType;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.filter.LinkFilterController;
import org.jdownloader.controlling.filter.LinkFilterSettings;
import org.jdownloader.controlling.filter.LinkgrabberFilterRule;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.translate._JDT;

public class ExportAction extends AppAction {
    /**
     * 
     */
    private static final long                serialVersionUID = 1L;
    private ArrayList<LinkgrabberFilterRule> rules;

    public ExportAction() {
        setName(_GUI._.LinkgrabberFilter_LinkgrabberFilter_export());
        setIconKey("export");
        setTooltipText(_JDT._.ExportAction_ExportAction_tt());

    }

    public boolean isEnabled() {
        return rules != null && rules.size() > 0;
    }

    public ExportAction(ArrayList<LinkgrabberFilterRule> selection) {
        this();

        rules = selection;
    }

    public void actionPerformed(ActionEvent e) {
        try {
            final String extension;
            if (JsonConfig.create(LinkFilterSettings.class).isRuleconditionsRegexEnabled()) {
                extension = ImportAction.EXT_ADVANCED;
            } else {
                extension = ImportAction.EXT;
            }
            File[] filterFiles = Dialog.getInstance().showFileChooser(ImportAction.EXT, _GUI._.LinkgrabberFilter_export_dialog_title(), FileChooserSelectionMode.FILES_ONLY, new FileFilter() {

                @Override
                public String getDescription() {
                    if (JsonConfig.create(LinkFilterSettings.class).isRuleconditionsRegexEnabled()) {
                        return "*" + ImportAction.EXT_ADVANCED;
                    } else {
                        return "*" + ImportAction.EXT;
                    }

                }

                @Override
                public boolean accept(File f) {
                    return StringUtils.endsWithCaseInsensitive(f.getName(), extension);

                }
            }, true, FileChooserType.SAVE_DIALOG, null);

            if (rules == null) {
                rules = LinkFilterController.getInstance().list();
            }
            String str = JSonStorage.toString(rules);
            File saveto = filterFiles[0];
            if (!saveto.getName().endsWith(extension)) {
                saveto = new File(saveto.getAbsolutePath() + extension);
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
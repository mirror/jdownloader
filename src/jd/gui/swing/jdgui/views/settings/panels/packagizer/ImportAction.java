package jd.gui.swing.jdgui.views.settings.panels.packagizer;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.filechooser.FileFilter;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.Dialog.FileChooserSelectionMode;
import org.appwork.utils.swing.dialog.Dialog.FileChooserType;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.filter.LinkFilterSettings;
import org.jdownloader.controlling.filter.LinkgrabberFilterRuleWrapper;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.controlling.packagizer.PackagizerRule;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.translate._JDT;

public class ImportAction extends AppAction {
    public static final String    EXT_ADVANCED     = ".jdPkgAdvancedRule";
    public static final String    EXT              = ".jdPkgRule";
    /**
     * 
     */
    private static final long     serialVersionUID = 1L;
    private PackagizerFilterTable table;

    public ImportAction(PackagizerFilterTable table) {
        setIconKey("import");
        setName(_GUI._.LinkgrabberFilter_LinkgrabberFilter_import());
        this.table = table;
        setTooltipText(_JDT._.ImportAction_tt());
    }

    public void actionPerformed(ActionEvent e) {
        try {
            File[] filterFiles = Dialog.getInstance().showFileChooser(ImportAction.EXT, _GUI._.LinkgrabberFilter_import_dialog_title(), FileChooserSelectionMode.FILES_ONLY, new FileFilter() {

                @Override
                public String getDescription() {
                    if (JsonConfig.create(LinkFilterSettings.class).isRuleconditionsRegexEnabled()) {
                        return "*" + EXT + ",*" + EXT_ADVANCED;
                    } else {
                        return "*" + EXT;
                    }
                }

                @Override
                public boolean accept(File f) {
                    if (JsonConfig.create(LinkFilterSettings.class).isRuleconditionsRegexEnabled()) {
                        return StringUtils.endsWithCaseInsensitive(f.getName(), EXT) || StringUtils.endsWithCaseInsensitive(f.getName(), EXT_ADVANCED);
                    } else {
                        return StringUtils.endsWithCaseInsensitive(f.getName(), EXT);
                    }
                }
            }, true, FileChooserType.OPEN_DIALOG, null);
            ArrayList<PackagizerRule> all = new ArrayList<PackagizerRule>();
            for (File f : filterFiles) {
                ArrayList<PackagizerRule> contents = null;
                try {
                    contents = JSonStorage.restoreFromString(IO.readFileToString(f), new TypeRef<ArrayList<PackagizerRule>>() {
                    }, null);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                if (contents == null || contents.size() == 0) {
                    Dialog.getInstance().showErrorDialog(_GUI._.LinkgrabberFilter_LinkgrabberFilter_import_invalid(f.getName()));
                } else {
                    if (JsonConfig.create(LinkFilterSettings.class).isRuleconditionsRegexEnabled() && f.getName().endsWith(EXT)) {
                        // convert
                        for (PackagizerRule r : contents) {
                            r.getFilenameFilter().setRegex(LinkgrabberFilterRuleWrapper.createPattern(r.getFilenameFilter().getRegex()).toString());
                            r.getHosterURLFilter().setRegex(LinkgrabberFilterRuleWrapper.createPattern(r.getHosterURLFilter().getRegex()).toString());
                            r.getSourceURLFilter().setRegex(LinkgrabberFilterRuleWrapper.createPattern(r.getSourceURLFilter().getRegex()).toString());
                            String customs = r.getFiletypeFilter().getCustoms();
                            customs = customs.replace(".", "|");
                            customs = customs.replaceAll("\\*+", ".*");
                            r.getFiletypeFilter().setCustoms(customs);
                            //
                            //
                        }

                    }
                    all.addAll(contents);
                }

            }
            PackagizerController.getInstance().addAll(all);

        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        }
    }
}
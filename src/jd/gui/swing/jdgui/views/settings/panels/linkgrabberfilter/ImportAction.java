package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.filechooser.FileFilter;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.appwork.utils.swing.dialog.ExtFileChooserDialog;
import org.appwork.utils.swing.dialog.FileChooserSelectionMode;
import org.appwork.utils.swing.dialog.FileChooserType;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.filter.LinkFilterController;
import org.jdownloader.controlling.filter.LinkgrabberFilterRule;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.logging.LogController;
import org.jdownloader.translate._JDT;

public class ImportAction extends AppAction {
    public static final String      EXT              = ".filter";
    public static final String      VIEW             = ".views";
    /**
     *
     */
    private static final long       serialVersionUID = 1L;
    private final LinkgrabberFilter table;

    public ImportAction(LinkgrabberFilter linkgrabberFilter) {
        setIconKey(IconKey.ICON_IMPORT);
        setName(_GUI.T.LinkgrabberFilter_LinkgrabberFilter_import());
        this.table = linkgrabberFilter;
        setTooltipText(_JDT.T.ImportAction_tt());
    }

    public void actionPerformed(ActionEvent e) {
        try {
            final String ext = table.getTable() instanceof ExceptionsTable ? ImportAction.VIEW : ImportAction.EXT;
            final ExtFileChooserDialog d = new ExtFileChooserDialog(0, _GUI.T.LinkgrabberFilter_import_dialog_title(), null, null);
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
            d.setType(FileChooserType.OPEN_DIALOG);
            d.setMultiSelection(true);
            Dialog.I().showDialog(d);
            final File[] files = d.getSelection();
            if (files == null || files.length == 0) {
                return;
            }
            final List<LinkgrabberFilterRule> all = new ArrayList<LinkgrabberFilterRule>();
            for (File file : files) {
                final List<LinkgrabberFilterRule> contents;
                try {
                    contents = JSonStorage.restoreFromString(IO.readFileToString(file), new TypeRef<ArrayList<LinkgrabberFilterRule>>() {
                    });
                } catch (Throwable e1) {
                    LogController.CL().log(e1);
                    Dialog.getInstance().showExceptionDialog(_GUI.T.lit_error_occured(), file.getAbsolutePath() + "-" + e1.getMessage(), e1);
                    continue;
                }
                if (contents == null || contents.size() == 0) {
                    Dialog.getInstance().showErrorDialog(_GUI.T.LinkgrabberFilter_LinkgrabberFilter_import_invalid(file.getName()));
                } else {
                    all.addAll(contents);
                }
            }
            LinkFilterController.getInstance().addAll(all);
        } catch (DialogNoAnswerException e1) {
            LogController.CL().log(e1);
        }
    }
}
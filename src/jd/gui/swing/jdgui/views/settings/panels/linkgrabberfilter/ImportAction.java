package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.filechooser.FileFilter;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ExtFileChooserDialog;
import org.appwork.utils.swing.dialog.FileChooserSelectionMode;
import org.appwork.utils.swing.dialog.FileChooserType;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.filter.LinkFilterController;
import org.jdownloader.controlling.filter.LinkgrabberFilterRule;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.translate._JDT;

public class ImportAction extends AppAction {

    public static final String EXT              = ".filter";
    public static final String VIEW             = ".views";
    /**
     * 
     */
    private static final long  serialVersionUID = 1L;
    private LinkgrabberFilter  table;

    public ImportAction(LinkgrabberFilter linkgrabberFilter) {
        setIconKey("import");
        setName(_GUI._.LinkgrabberFilter_LinkgrabberFilter_import());
        this.table = linkgrabberFilter;
        setTooltipText(_JDT._.ImportAction_tt());
    }

    public void actionPerformed(ActionEvent e) {
        try {

            final String ext = table.getView() instanceof ExceptionsTable ? ImportAction.VIEW : ImportAction.EXT;

            ExtFileChooserDialog d = new ExtFileChooserDialog(0, _GUI._.LinkgrabberFilter_import_dialog_title(), null, null);
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

            File[] filterFiles = d.getSelection();
            if (filterFiles == null) return;
            ArrayList<LinkgrabberFilterRule> all = new ArrayList<LinkgrabberFilterRule>();
            for (File f : filterFiles) {
                ArrayList<LinkgrabberFilterRule> contents = null;
                try {
                    contents = JSonStorage.restoreFromString(IO.readFileToString(f), new TypeRef<ArrayList<LinkgrabberFilterRule>>() {
                    }, null);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                if (contents == null || contents.size() == 0) {
                    Dialog.getInstance().showErrorDialog(_GUI._.LinkgrabberFilter_LinkgrabberFilter_import_invalid(f.getName()));
                } else {

                    all.addAll(contents);
                }

            }
            LinkFilterController.getInstance().addAll(all);

        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        }
    }
}
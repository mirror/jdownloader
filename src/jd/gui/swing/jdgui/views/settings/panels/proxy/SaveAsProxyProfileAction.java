package jd.gui.swing.jdgui.views.settings.panels.proxy;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.filechooser.FileFilter;

import jd.controlling.proxy.ProxyController;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ExtFileChooserDialog;
import org.appwork.utils.swing.dialog.FileChooserSelectionMode;
import org.appwork.utils.swing.dialog.FileChooserType;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class SaveAsProxyProfileAction extends AppAction {

    public static final String JDPROXIES = ".jdproxies";
    private ProxyTable         table;

    public SaveAsProxyProfileAction(ProxyTable table) {
        setName(_GUI._.SaveAsProxyProfileAction_SaveAsProxyProfileAction_());
        setIconKey("export");
        this.table = table;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        ExtFileChooserDialog d = new ExtFileChooserDialog(0, _GUI._.SaveAsProxyProfileAction_actionPerformed_choose_file(), null, null);
        d.setFileFilter(new FileFilter() {

            @Override
            public String getDescription() {

                return "*" + JDPROXIES;
            }

            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().endsWith(JDPROXIES);
            }
        });

        d.setFileSelectionMode(FileChooserSelectionMode.FILES_AND_DIRECTORIES);
        d.setMultiSelection(false);

        d.setStorageID(JDPROXIES);
        d.setType(FileChooserType.SAVE_DIALOG);
        try {
            Dialog.getInstance().showDialog(d);

            File saveTo = d.getSelectedFile();
            if (!saveTo.getName().endsWith(JDPROXIES)) {
                saveTo = new File(saveTo.getAbsolutePath() + JDPROXIES);
            }
            ProxyController.getInstance().exportTo(saveTo);

        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        } catch (Exception e1) {
            Dialog.getInstance().showExceptionDialog(_GUI._.lit_error_occured(), e1.getMessage(), e1);
        }
    }

}

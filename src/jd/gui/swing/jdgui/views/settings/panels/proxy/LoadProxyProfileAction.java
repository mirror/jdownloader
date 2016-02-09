package jd.gui.swing.jdgui.views.settings.panels.proxy;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.filechooser.FileFilter;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ExtFileChooserDialog;
import org.appwork.utils.swing.dialog.FileChooserSelectionMode;
import org.appwork.utils.swing.dialog.FileChooserType;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;

import jd.controlling.proxy.ProxyController;

public class LoadProxyProfileAction extends AppAction {

    public LoadProxyProfileAction(ProxyTable table) {
        setName(_GUI.T.LoadProxyProfileAction_LoadProxyProfileAction_());
        setIconKey(IconKey.ICON_IMPORT);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            ExtFileChooserDialog d = new ExtFileChooserDialog(0, _GUI.T.LoadProxyProfileAction_actionPerformed_(), null, null);
            d.setFileFilter(new FileFilter() {

                @Override
                public String getDescription() {

                    return "*" + SaveAsProxyProfileAction.JDPROXIES;
                }

                @Override
                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().endsWith(SaveAsProxyProfileAction.JDPROXIES);
                }
            });

            d.setFileSelectionMode(FileChooserSelectionMode.FILES_AND_DIRECTORIES);
            d.setMultiSelection(false);

            d.setStorageID(SaveAsProxyProfileAction.JDPROXIES);
            d.setType(FileChooserType.OPEN_DIALOG);

            Dialog.getInstance().showDialog(d);

            File selected = d.getSelectedFile();
            if (selected.exists() && selected.getName().endsWith(SaveAsProxyProfileAction.JDPROXIES)) {
                ProxyController.getInstance().importFrom(selected);
            }
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            Dialog.getInstance().showExceptionDialog(_GUI.T.lit_error_occured(), e1.getMessage(), e1);
        }
    }

}

package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.event.ActionEvent;
import java.io.File;

import jd.controlling.JDController;
import jd.nutils.io.JDFileFilter;
import jd.utils.JDUtilities;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.Dialog.FileChooserSelectionMode;
import org.appwork.utils.swing.dialog.Dialog.FileChooserType;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class AddContainerAction extends AppAction {
    {
        putValue(SMALL_ICON, NewTheme.I().getIcon("addContainer", 16));
        putValue(NAME, _GUI._.AddContainerAction());

    }

    public void actionPerformed(ActionEvent e) {
        // File[] ret = UserIO.getInstance().requestFileChooser("_LOADSAVEDLC",
        // _GUI._.gui_filechooser_loaddlc(), UserIO.FILES_ONLY, new
        // JDFileFilter(null, JDUtilities.getContainerExtensions(null), true),
        // true);
        File[] ret;
        try {
            ret = Dialog.getInstance().showFileChooser("loaddlc", _GUI._.AddContainerAction_actionPerformed_(), FileChooserSelectionMode.FILES_ONLY, new JDFileFilter(_GUI._.AddContainerAction_actionPerformed_extensions(JDUtilities.getContainerExtensions(null)), JDUtilities.getContainerExtensions(null), true), false, FileChooserType.OPEN_DIALOG, null);

            if (ret == null) return;
            for (File r : ret) {
                JDController.loadContainerFile(r);
            }
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        }
    }

}

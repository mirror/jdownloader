package org.jdownloader.controlling.ffmpeg;

import java.io.File;

import org.appwork.storage.config.ValidationException;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ExtFileChooserDialog;
import org.appwork.utils.swing.dialog.FileChooserSelectionMode;
import org.appwork.utils.swing.dialog.FileChooserType;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.advanced.AdvandedValueEditor;

public class FFmpegBinaryValueEditor extends AdvandedValueEditor<String> {
    @Override
    public String edit(String path) throws ValidationException {
        final ExtFileChooserDialog d = new ExtFileChooserDialog(0, _GUI.T.LoadProxyProfileAction_actionPerformed_(), null, null);
        d.setFileSelectionMode(FileChooserSelectionMode.FILES_ONLY);
        if (path != null && new File(path).isFile()) {
            d.setPreSelection(new File(path));
        }
        d.setMultiSelection(false);
        d.setType(FileChooserType.OPEN_DIALOG);
        try {
            Dialog.getInstance().showDialog(d);
            final File ret = d.getSelectedFile();
            if (ret != null && ret.isFile()) {
                return ret.getAbsolutePath();
            } else {
                throw new ValidationException("invalid:" + ret);
            }
        } catch (DialogClosedException e) {
            throw new ValidationException(e);
        } catch (DialogCanceledException e) {
            throw new ValidationException(e);
        }
    }
}
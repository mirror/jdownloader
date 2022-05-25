package org.jdownloader.controlling.ffmpeg;

import java.io.File;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.appwork.utils.swing.dialog.ExtFileChooserDialog;
import org.appwork.utils.swing.dialog.FileChooserSelectionMode;
import org.appwork.utils.swing.dialog.FileChooserType;
import org.jdownloader.settings.advanced.AdvandedValueEditor;

public class FFmpegBinaryValueEditor extends AdvandedValueEditor<String> {
    @Override
    public String edit(KeyHandler<String> keyHandler, String path) throws ValidationException {
        final DescriptionForConfigEntry description = keyHandler.getAnnotation(DescriptionForConfigEntry.class);
        String title = description != null ? description.value() : null;
        if (StringUtils.isEmpty(title)) {
            title = keyHandler.getKey();
        }
        final ExtFileChooserDialog d = new ExtFileChooserDialog(0, title, null, null);
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
        } catch (DialogNoAnswerException e) {
            throw new ValidationException(e);
        }
    }
}
package org.jdownloader.extensions.shutdown;

import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.images.NewTheme;

public class WarningDialog extends ConfirmDialog implements WarningDialogInterface {

    private ShutdownExtension extension;

    public WarningDialog(ShutdownExtension shutDown, String title, String message) {
        super(Dialog.STYLE_HTML | UIOManager.LOGIC_COUNTDOWN, title, message, NewTheme.I().getIcon("warning", 32), null, null);
        this.extension = shutDown;
        this.setTimeout(shutDown.getSettings().getCountdownTime() * 1000);

    }

    protected void packed() {
        getDialog().setAlwaysOnTop(true);
    }
}

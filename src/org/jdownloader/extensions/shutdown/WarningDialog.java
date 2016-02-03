package org.jdownloader.extensions.shutdown;

import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;

public class WarningDialog extends ConfirmDialog implements WarningDialogInterface {

    private ShutdownExtension extension;

    public WarningDialog(ShutdownExtension shutDown, String title, String message) {
        super(Dialog.STYLE_HTML | UIOManager.LOGIC_COUNTDOWN | UIOManager.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL, title, message, new AbstractIcon(IconKey.ICON_WARNING, 32), null, null);
        this.extension = shutDown;
        this.setTimeout(shutDown.getSettings().getCountdownTime() * 1000);

    }

    protected void packed() {
        getDialog().setAlwaysOnTop(true);
    }
}

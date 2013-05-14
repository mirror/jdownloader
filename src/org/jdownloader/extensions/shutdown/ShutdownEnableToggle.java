package org.jdownloader.extensions.shutdown;

import java.awt.event.ActionEvent;

import org.appwork.uio.MessageDialogImpl;
import org.appwork.uio.MessageDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.extensions.AbstractExtensionQuickToggleAction;
import org.jdownloader.gui.shortcuts.ShortcutController;
import org.jdownloader.gui.views.SelectionInfo;

public class ShutdownEnableToggle extends AbstractExtensionQuickToggleAction<ShutdownExtension> {

    public ShutdownEnableToggle(SelectionInfo<?, ?> selection) {
        super(CFG_SHUTDOWN.SHUTDOWN_ACTIVE);
        setName(org.jdownloader.extensions.shutdown.translate.T._.lit_shutdownn());
        setIconKey("logout");
        setAccelerator(ShortcutController._.getShutdownExtensionToggleShutdownAction());
        setTooltipText(org.jdownloader.extensions.shutdown.translate.T._.action_tooltip());
        this.setEnabled(true);
        setSelected(false);

    }

    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);

        if (isSelected()) {

            UIOManager.I().show(MessageDialogInterface.class, new MessageDialogImpl(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, org.jdownloader.extensions.shutdown.translate.T._.addons_jdshutdown_statusmessage_enabled()));

        } else {
            UIOManager.I().show(MessageDialogInterface.class, new MessageDialogImpl(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, org.jdownloader.extensions.shutdown.translate.T._.addons_jdshutdown_statusmessage_disabled()));

        }

    }

};

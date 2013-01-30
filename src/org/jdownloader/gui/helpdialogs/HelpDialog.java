package org.jdownloader.gui.helpdialogs;

import java.awt.Point;
import java.awt.event.WindowEvent;

import javax.swing.ImageIcon;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.BinaryLogic;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.locator.PointOnScreenLocator;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;
import org.jdownloader.updatev2.RestartController;

public class HelpDialog {

    public static void show(final Point point, final String dontShowAgainKey, int flags, String title, String msg, ImageIcon icon) {
        show(null, null, point, dontShowAgainKey, flags, title, msg, icon);
    }

    public static void show(final Boolean expandToBottom, final Boolean expandToRight, final Point point, final String dontShowAgainKey, int flags, String title, String msg, ImageIcon icon) {
        final boolean test = RestartController.getInstance().getParameterParser(null).hasCommandSwitch("translatortest");
        if (!JsonConfig.create(GraphicalUserInterfaceSettings.class).isBalloonNotificationEnabled()) return;

        try {

            ConfirmDialog d = new ConfirmDialog(flags | Dialog.BUTTONS_HIDE_CANCEL | Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, title, _GUI._.literall_usage_tipp() + "\r\n\r\n..." + msg, icon, null, null) {
                {
                    if (point != null) setLocator(new PointOnScreenLocator(point));
                }

                @Override
                protected String getDontShowAgainKey() {
                    if (test) return "bla_" + System.currentTimeMillis();
                    if (dontShowAgainKey == null) return super.getDontShowAgainKey();
                    return dontShowAgainKey;
                }

                public void windowClosing(final WindowEvent arg0) {
                    setReturnmask(false);
                    this.dispose();
                }

            };

            if (BinaryLogic.containsAll(flags, Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN)) {
                d.setDoNotShowAgainSelected(true);
            }
            Dialog.getInstance().showDialog(d);
        } catch (Throwable e) {
            Log.exception(e);
        }

    }

    public static void show(int flags, String title, String msg, ImageIcon icon) {
        show(null, title, flags, title, msg, icon);
    }

}

package org.jdownloader.gui.helpdialogs;

import java.awt.Point;
import java.awt.event.WindowEvent;

import javax.swing.ImageIcon;

import org.appwork.utils.BinaryLogic;
import org.appwork.utils.logging.Log;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.OffScreenException;
import org.appwork.utils.swing.dialog.SimpleTextBallon;
import org.jdownloader.gui.translate._GUI;

public class HelpDialog {

    public static void show(final Point point, final String dontShowAgainKey, int flags, String title, String msg, ImageIcon icon) {
        show(null, null, point, dontShowAgainKey, flags, title, msg, icon);
    }

    public static void show(final Boolean expandToBottom, final Boolean expandToRight, final Point point, final String dontShowAgainKey, int flags, String title, String msg, ImageIcon icon) {
        if (CrossSystem.isWindows()) {
            try {

                SimpleTextBallon d = new SimpleTextBallon(flags | Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, title, msg, icon) {

                    @Override
                    protected boolean doExpandToBottom(boolean b) {
                        if (expandToBottom != null) return expandToBottom;
                        return super.doExpandToBottom(b);
                    }

                    @Override
                    public int[] getContentInsets() {
                        return new int[] { 14, 14, 14, 14 };
                    }

                    @Override
                    protected boolean doExpandToRight(boolean b) {
                        if (expandToRight != null) return expandToRight;
                        return super.doExpandToRight(b);
                    }

                    @Override
                    protected String getDontShowAgainKey() {
                        if (dontShowAgainKey == null) return super.getDontShowAgainKey();
                        Log.L.info(dontShowAgainKey);
                        return dontShowAgainKey;
                    }

                };
                if (point != null) d.setDesiredLocation(point);
                Dialog.getInstance().showDialog(d);
            } catch (OffScreenException e1) {
                e1.printStackTrace();
            } catch (DialogClosedException e1) {
                e1.printStackTrace();
            } catch (DialogCanceledException e1) {
                e1.printStackTrace();
            }
        } else {
            try {

                ConfirmDialog d = new ConfirmDialog(flags | Dialog.BUTTONS_HIDE_CANCEL | Dialog.BUTTONS_HIDE_OK | Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, title, _GUI._.literall_usage_tipp() + "\r\n\r\n..." + msg, icon, null, null) {
                    @Override
                    protected String getDontShowAgainKey() {
                        if (dontShowAgainKey == null) return super.getDontShowAgainKey();
                        return dontShowAgainKey;
                    }

                    protected Point getDesiredLocation() {
                        if (point != null) return point;
                        return super.getDesiredLocation();
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
    }

    public static void show(int flags, String title, String msg, ImageIcon icon) {
        show(null, title, flags, title, msg, icon);
    }

}

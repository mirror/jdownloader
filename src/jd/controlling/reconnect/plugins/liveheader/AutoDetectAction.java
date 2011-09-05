package jd.controlling.reconnect.plugins.liveheader;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import org.appwork.utils.event.ProcessCallBack;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.jdownloader.extensions.antireconnect.translate.T;
import org.jdownloader.images.NewTheme;

public class AutoDetectAction extends AbstractAction {
    public AutoDetectAction() {
        super();
        putValue(NAME, T._.AutoDetectAction_AutoDetectAction_());
        putValue(SMALL_ICON, NewTheme.I().getIcon("wizard", 18));
    }

    public void actionPerformed(ActionEvent e) {
        ProgressDialog.ProgressGetter pg = new ProgressDialog.ProgressGetter() {

            private String status   = "";
            private int    progress = -1;

            public void run() throws Exception {
                new LiveHeaderDetectionWizard().runOnlineScan(new ProcessCallBack() {

                    public void setStatusString(Object caller, String string) {
                        status = string;
                    }

                    public void setProgress(Object caller, int percent) {
                        progress = percent;
                    }

                    public void showDialog(Object caller, String title, String message, ImageIcon icon) {
                        try {
                            Dialog.getInstance().showConfirmDialog(Dialog.BUTTONS_HIDE_CANCEL, title, message, icon, null, null);
                        } catch (DialogClosedException e) {
                            e.printStackTrace();
                        } catch (DialogCanceledException e) {
                            e.printStackTrace();
                        }
                    }

                });
            }

            public String getString() {
                return status;
            }

            public int getProgress() {
                return progress;
            }
        };

        ProgressDialog d = new ProgressDialog(pg, Dialog.BUTTONS_HIDE_OK, T._.AutoDetectAction_actionPerformed_d_title(), T._.AutoDetectAction_actionPerformed_d_msg(), NewTheme.I().getIcon("wizard", 32), null, null);
        try {
            Dialog.getInstance().showDialog(d);
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        } finally {
            System.out.println("CLOSED");
        }
    }

}

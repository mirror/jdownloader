package jd.controlling.reconnect.plugins.liveheader;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.appwork.utils.event.ProcessCallBack;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.jdownloader.extensions.antireconnect.translate.T;
import org.jdownloader.images.NewTheme;

public class RouterSendAction extends AbstractAction {

    public RouterSendAction(LiveHeaderReconnect liveHeaderReconnect) {
    }

    public void actionPerformed(ActionEvent e) {

        ProgressDialog.ProgressGetter pg = new ProgressDialog.ProgressGetter() {

            private String status   = "";
            private int    progress = -1;

            public void run() throws Exception {
                final RouterSender rs = new RouterSender();
                rs.setRequested(true);
                rs.run(new ProcessCallBack() {

                    public void setStatusString(String string) {
                        status = string;
                    }

                    public void setProgress(int percent) {
                        progress = percent;
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

        ProgressDialog d = new ProgressDialog(pg, Dialog.BUTTONS_HIDE_OK, T._.RouterSendAction_actionPerformed_(), T._.RouterSendAction_actionPerformed_msg(), NewTheme.I().getIcon("reconnect", 32), null, null);
        try {
            Dialog.getInstance().showDialog(d);
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }
    }

}

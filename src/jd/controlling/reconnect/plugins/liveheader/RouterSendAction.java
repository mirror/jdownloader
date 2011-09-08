package jd.controlling.reconnect.plugins.liveheader;

import java.awt.event.ActionEvent;

import org.appwork.swing.action.BasicAction;
import org.appwork.swing.components.tooltips.BasicTooltipFactory;
import org.appwork.utils.event.ProcessCallBackAdapter;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.jdownloader.extensions.antireconnect.translate.T;
import org.jdownloader.images.NewTheme;

public class RouterSendAction extends BasicAction {

    public RouterSendAction(LiveHeaderReconnect liveHeaderReconnect) {
        super(T._.RouterSendAction_RouterSendAction_());
        putValue(SMALL_ICON, NewTheme.I().getIcon("upload", 18));
        setTooltipFactory(new BasicTooltipFactory(getName(), T._.RouterSendAction_RouterSendAction_tt(), NewTheme.I().getIcon("upload", 32)));

    }

    public void actionPerformed(ActionEvent e) {
        // while(JsonConfig.create(ReconnectConfig.class).getSuccessCounter()

        ProgressGetter pg = new ProgressDialog.ProgressGetter() {

            private String text;
            private int    progress;

            public void run() throws Exception {
                new LiveHeaderDetectionWizard().sendRouter(new ProcessCallBackAdapter() {
                    @Override
                    public void setProgress(final Object caller, final int percent) {
                        progress = percent;
                    }

                    @Override
                    public void setStatus(final Object caller, final Object statusObject) {

                    }

                    @Override
                    public void setStatusString(final Object caller, final String string) {
                        text = string;
                    }
                });
            }

            public String getString() {
                return text;
            }

            public int getProgress() {
                return progress;
            }
        };
        try {
            Dialog.I().showDialog(new ProgressDialog(pg, 0, T._.RouterSendAction_actionPerformed_title(), T._.RouterSendAction_actionPerformed_msg(), NewTheme.I().getIcon("upload", 32), null, null));
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }
    }

}

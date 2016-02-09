package jd.controlling.reconnect.pluginsinc.liveheader;

import java.awt.event.ActionEvent;

import org.appwork.swing.action.BasicAction;
import org.appwork.swing.components.tooltips.BasicTooltipFactory;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;

import jd.controlling.reconnect.ProcessCallBackAdapter;
import jd.controlling.reconnect.pluginsinc.liveheader.translate.T;

public class RouterSendAction extends BasicAction {

    public RouterSendAction(LiveHeaderReconnect liveHeaderReconnect) {
        super(T.T.RouterSendAction_RouterSendAction_());
        putValue(SMALL_ICON, new AbstractIcon(IconKey.ICON_UPLOAD, 18));
        setTooltipFactory(new BasicTooltipFactory(getName(), T.T.RouterSendAction_RouterSendAction_tt(), new AbstractIcon(IconKey.ICON_UPLOAD, 32)));
        setEnabled(true);
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

            @Override
            public String getLabelString() {
                return null;
            }
        };
        try {
            Dialog.I().showDialog(new ProgressDialog(pg, 0, T.T.RouterSendAction_actionPerformed_title(), T.T.RouterSendAction_actionPerformed_msg2(), new AbstractIcon(IconKey.ICON_UPLOAD, 32), null, null).setPreferredWidth(500));
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }
    }
}

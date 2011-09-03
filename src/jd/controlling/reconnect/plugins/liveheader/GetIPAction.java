package jd.controlling.reconnect.plugins.liveheader;

import java.awt.event.ActionEvent;
import java.net.InetAddress;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import jd.controlling.ProgressController;
import jd.controlling.reconnect.RouterUtils;
import jd.controlling.reconnect.plugins.liveheader.translate.T;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.jdownloader.images.NewTheme;

public class GetIPAction extends AbstractAction {

    private LiveHeaderReconnect plugin;

    public GetIPAction(LiveHeaderReconnect liveHeaderReconnect) {
        plugin = liveHeaderReconnect;
        putValue(NAME, T._.GetIPAction_GetIPAction_());
        putValue(SMALL_ICON, NewTheme.I().getIcon("defaultProxy", 18));
    }

    public void actionPerformed(ActionEvent e) {

        ProgressDialog.ProgressGetter pg = new ProgressDialog.ProgressGetter() {

            private int progress = -1;

            public void run() throws Exception {
                final InetAddress ia = RouterUtils.getAddress(true);
                if (ia != null) {

                    JsonConfig.create(LiveHeaderReconnectSettings.class).setRouterIP(ia.getHostName());
                }

                if (ia != null) {
                    Dialog.getInstance().showMessageDialog(T._.gui_config_routeripfinder_ready(ia.getHostName()));

                } else {
                    Dialog.getInstance().showErrorDialog(T._.gui_config_routeripfinder_notfound());

                }
                progress = 100;
            }

            public String getString() {
                return T._.GetIPAction_getString_progress();
            }

            public int getProgress() {
                return progress;
            }
        };

        ProgressDialog d = new ProgressDialog(pg, Dialog.BUTTONS_HIDE_OK, T._.GetIPAction_actionPerformed_d_title(), T._.GetIPAction_actionPerformed_d_msg(), NewTheme.I().getIcon("defaultProxy", 32), null, null);
        try {
            Dialog.getInstance().showDialog(d);
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }

        final ProgressController progress = new ProgressController(100, T._.gui_config_routeripfinder_featchIP(), (ImageIcon) null);

    }

}

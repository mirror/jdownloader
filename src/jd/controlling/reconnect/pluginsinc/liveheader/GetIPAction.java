package jd.controlling.reconnect.pluginsinc.liveheader;

import java.awt.event.ActionEvent;
import java.net.InetAddress;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.action.BasicAction;
import org.appwork.swing.components.tooltips.BasicTooltipFactory;
import org.appwork.swing.components.tooltips.TooltipFactory;
import org.appwork.uio.UIOManager;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressInterface;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;

import jd.controlling.reconnect.RouterUtils;
import jd.controlling.reconnect.pluginsinc.liveheader.translate.T;

public class GetIPAction extends BasicAction {

    private LiveHeaderReconnect plugin;

    public GetIPAction(LiveHeaderReconnect liveHeaderReconnect) {
        plugin = liveHeaderReconnect;
        putValue(NAME, T.T.GetIPAction_GetIPAction_());
        putValue(SMALL_ICON, new AbstractIcon(IconKey.ICON_DEFAULTPROXY, 18));

    }

    public TooltipFactory getTooltipFactory() {

        String ip = JsonConfig.create(LiveHeaderReconnectSettings.class).getRouterIP();
        String txt = StringUtils.isEmpty(ip) ? T.T.GetIPAction_GetIPAction_tt() : T.T.GetIPAction_getTooltipText_tt_2(ip);
        return new BasicTooltipFactory(getName(), txt, new AbstractIcon(IconKey.ICON_DEFAULTPROXY, 32));
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
                    Dialog.getInstance().showMessageDialog(T.T.gui_config_routeripfinder_ready(ia.getHostName()));

                } else {
                    Dialog.getInstance().showErrorDialog(T.T.gui_config_routeripfinder_notfound());

                }
                progress = 100;
            }

            public String getString() {
                return T.T.GetIPAction_getString_progress();
            }

            public int getProgress() {
                return progress;
            }

            @Override
            public String getLabelString() {
                return null;
            }
        };

        ProgressDialog d = new ProgressDialog(pg, UIOManager.BUTTONS_HIDE_OK, T.T.GetIPAction_actionPerformed_d_title(), T.T.GetIPAction_actionPerformed_d_msg(), new AbstractIcon(IconKey.ICON_DEFAULTPROXY, 32), null, null);
        try {
            UIOManager.I().show(ProgressInterface.class, d).throwCloseExceptions();
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }

    }

}

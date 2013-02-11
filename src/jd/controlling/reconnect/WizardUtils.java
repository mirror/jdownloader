package jd.controlling.reconnect;

import java.awt.event.ActionEvent;
import java.net.InetAddress;

import javax.swing.AbstractAction;

import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.userio.NewUIO;
import org.jdownloader.images.NewTheme;

public class WizardUtils {

    /**
     * checks if the user has a modem connection, and asks the user if he want
     * to do a reconnect wizard run either. returns true if not.
     * 
     * @return
     */
    public static boolean modemCheck() {
        try {
            if (RouterUtils.isWindowsModemConnection()) {
                final boolean[] modemChoose = new boolean[] { false };
                final ConfirmDialog d = new ConfirmDialog(0, _GUI._.literally_warning(), _GUI._.AutoSetupAction_actionPerformed_modem(), NewTheme.I().getIcon("modem", 32), _GUI._.AutoSetupAction_actionPerformed_dont_know(), _GUI._.AutoSetupAction_actionPerformed_router());
                d.setLeftActions(new AbstractAction() {
                    {
                        putValue(NAME, _GUI._.AutoSetupAction_actionPerformed_choose_modem());
                    }

                    public void actionPerformed(ActionEvent e) {
                        modemChoose[0] = true;
                        d.dispose();
                    }
                });
                try {
                    Dialog.getInstance().showDialog(d);

                    if (modemChoose[0]) {
                        Dialog.getInstance().showErrorDialog(_GUI._.AutoSetupAction_actionPerformed_noautoformodem());
                        CrossSystem.openURLOrShowMessage("http://jdownloader.org/knowledge/wiki/reconnect/modem");
                        return true;
                    }
                    CrossSystem.openURLOrShowMessage("http://jdownloader.org/knowledge/wiki/reconnect/modem");
                    // don't know
                } catch (DialogCanceledException e1) {
                    // router

                }

            }
        } catch (Throwable e1) {

        }
        return false;
    }

    public static boolean vpnCheck() {

        try {

            InetAddress routerAdress = RouterUtils.getAddress(false);
            InetAddress gateway = RouterUtils.getWindowsGateway();

            if (!routerAdress.equals(gateway)) {
                // ouch. if gateway is not routerIp, we might be in a VPN
                try {
                    NewUIO.I().showConfirmDialog(0, _GUI._.ReconnectPluginController_autoFind_gatewaywarning_t(), _GUI._.ReconnectPluginController_autoFind_gatewaywarning(), NewTheme.I().getIcon("warning", 32), _GUI._.literally_continue(), null);
                } catch (DialogNoAnswerException e) {
                    return true;
                }
            }
        } catch (Throwable t) {

        }
        return false;
    }

}

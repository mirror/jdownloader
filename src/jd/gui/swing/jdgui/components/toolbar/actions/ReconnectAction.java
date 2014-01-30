package jd.gui.swing.jdgui.components.toolbar.actions;

import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.reconnect.ReconnectException;
import jd.controlling.reconnect.Reconnecter.ReconnectResult;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.WarnLevel;
import jd.gui.swing.jdgui.views.settings.panels.reconnect.ReconnectDialog;

import org.appwork.uio.CloseReason;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.gui.toolbar.action.AbstractToolBarAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class ReconnectAction extends AbstractToolBarAction {

    public ReconnectAction() {

        setIconKey("reconnect");
        setAccelerator(KeyEvent.VK_R);

    }

    public void actionPerformed(ActionEvent e) {
        new Thread() {
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                ConfirmDialog d = new ConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL, _GUI._.lit_are_you_sure(), _GUI._.gui_reconnect_confirm(), NewTheme.I().getIcon("reconnect", 32), _GUI._.lit_yes(), _GUI._.lit_no()) {

                    @Override
                    public ModalityType getModalityType() {
                        return ModalityType.MODELESS;
                        //
                    }

                };
                if (!JDGui.bugme(WarnLevel.NORMAL) || UIOManager.I().show(ConfirmDialogInterface.class, d).getCloseReason() == CloseReason.OK) {

                    try {
                        Dialog.getInstance().showDialog(new ReconnectDialog() {
                            protected boolean startReconnectAndWait(LogSource logger) throws ReconnectException, InterruptedException {
                                return DownloadWatchDog.getInstance().requestReconnect(true) == ReconnectResult.SUCCESSFUL;
                            }
                        });
                    } catch (DialogClosedException e1) {
                        e1.printStackTrace();
                    } catch (DialogCanceledException e1) {
                        e1.printStackTrace();
                    }

                    // new Thread(new Runnable() {
                    // public void run() {
                    // Reconnecter.getInstance().forceReconnect();
                    // }
                    // }).start();
                } else {
                    System.out.println("Canceled");
                }
            }
        }.start();

    }

    @Override
    protected String createTooltip() {
        return _GUI._.action_reconnect_invoke_tooltip();
    }

}

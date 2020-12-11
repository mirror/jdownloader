package jd.gui.swing.jdgui.components.toolbar.actions;

import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.reconnect.ReconnectException;
import jd.controlling.reconnect.Reconnecter.ReconnectResult;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.WarnLevel;
import jd.gui.swing.jdgui.views.settings.panels.reconnect.ReconnectDialog;
import jd.plugins.download.DownloadInterface;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.uio.CloseReason;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.toolbar.action.AbstractToolBarAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.translate._JDT;

public class ReconnectAction extends AbstractToolBarAction {
    private final GenericConfigEventListener<String> listener;

    public ReconnectAction() {
        setIconKey(IconKey.ICON_RECONNECT);
        setAccelerator(KeyEvent.VK_R);
        listener = new GenericConfigEventListener<String>() {
            @Override
            public void onConfigValueModified(KeyHandler<String> keyHandler, String newValue) {
                updateAction(newValue);
            }

            @Override
            public void onConfigValidatorError(KeyHandler<String> keyHandler, String invalidValue, ValidationException validateException) {
            }
        };
        org.jdownloader.settings.staticreferences.CFG_RECONNECT.ACTIVE_PLUGIN_ID.getEventSender().addListener(listener);
        updateAction(org.jdownloader.settings.staticreferences.CFG_RECONNECT.ACTIVE_PLUGIN_ID.getValue());
    }

    private void updateAction(final String activePlugin) {
        if ("DummyRouterPlugin".equalsIgnoreCase(activePlugin)) {
            new EDTRunner() {
                @Override
                protected void runInEDT() {
                    ReconnectAction.this.setEnabled(false);
                }
            };
        } else {
            new EDTRunner() {
                @Override
                protected void runInEDT() {
                    ReconnectAction.this.setEnabled(true);
                }
            };
        }
    }

    public void actionPerformed(ActionEvent e) {
        new Thread() {
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                String dialogMessage = null;
                for (final SingleDownloadController con : DownloadWatchDog.getInstance().getRunningDownloadLinks()) {
                    if (con.isAlive()) {
                        dialogMessage = _JDT.T.DownloadWatchDog_onShutdownRequest_();
                        final DownloadInterface dl = con.getDownloadInstance();
                        if (dl != null && !con.getDownloadLink().isResumeable()) {
                            dialogMessage = _JDT.T.DownloadWatchDog_onShutdownRequest_nonresumable();
                            break;
                        }
                    }
                }
                if (dialogMessage == null) {
                    dialogMessage = _GUI.T.gui_reconnect_confirm();
                } else {
                    dialogMessage = "\r\n" + _GUI.T.gui_reconnect_confirm();
                }
                final ConfirmDialog d = new ConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL, _GUI.T.lit_are_you_sure(), dialogMessage, new AbstractIcon(IconKey.ICON_RECONNECT, 32), _GUI.T.lit_yes(), _GUI.T.lit_no()) {
                    @Override
                    public ModalityType getModalityType() {
                        return ModalityType.MODELESS;
                    }
                };
                if (!JDGui.bugme(WarnLevel.NORMAL) || UIOManager.I().show(ConfirmDialogInterface.class, d).getCloseReason() == CloseReason.OK) {
                    try {
                        Dialog.getInstance().showDialog(new ReconnectDialog() {
                            @Override
                            public ModalityType getModalityType() {
                                return ModalityType.MODELESS;
                            }

                            protected boolean startReconnectAndWait(LogSource logger) throws ReconnectException, InterruptedException {
                                return DownloadWatchDog.getInstance().requestReconnect(true) == ReconnectResult.SUCCESSFUL;
                            }
                        });
                    } catch (DialogClosedException e1) {
                        e1.printStackTrace();
                    } catch (DialogCanceledException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }.start();
    }

    @Override
    protected String createTooltip() {
        return _GUI.T.action_reconnect_invoke_tooltip();
    }
}

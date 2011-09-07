package jd.controlling.reconnect.plugins.liveheader;

import java.awt.event.ActionEvent;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import jd.controlling.reconnect.ReconnectResult;
import jd.controlling.reconnect.RouterUtils;
import jd.controlling.reconnect.plugins.liveheader.translate.T;

import org.appwork.utils.event.ProcessCallBack;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class AutoDetectAction extends AbstractAction {
    private boolean modemChoose;

    public AutoDetectAction() {
        super();
        putValue(NAME, T._.AutoDetectAction_AutoDetectAction_());
        putValue(SMALL_ICON, NewTheme.I().getIcon("wizard", 18));
    }

    public void actionPerformed(ActionEvent e) {
        try {
            if (RouterUtils.isWindowsModemConnection()) {
                modemChoose = false;
                final ConfirmDialog d = new ConfirmDialog(0, _GUI._.literally_warning(), _GUI._.AutoSetupAction_actionPerformed_modem(), NewTheme.I().getIcon("modem", 32), _GUI._.AutoSetupAction_actionPerformed_dont_know(), _GUI._.AutoSetupAction_actionPerformed_router());
                d.setLeftActions(new AbstractAction() {
                    {
                        putValue(NAME, _GUI._.AutoSetupAction_actionPerformed_choose_modem());
                    }

                    public void actionPerformed(ActionEvent e) {
                        modemChoose = true;
                        d.dispose();
                    }
                });
                try {
                    Dialog.getInstance().showDialog(d);

                    if (modemChoose) {
                        Dialog.getInstance().showErrorDialog(_GUI._.AutoSetupAction_actionPerformed_noautoformodem());
                        CrossSystem.openURLOrShowMessage("http://jdownloader.org/knowledge/wiki/reconnect/modem");
                        return;
                    }
                    CrossSystem.openURLOrShowMessage("http://jdownloader.org/knowledge/wiki/reconnect/modem");
                    // don't know
                } catch (DialogCanceledException e1) {
                    // router

                }

            }
        } catch (Throwable e1) {

        }
        ReconnectFindDialog d = new ReconnectFindDialog() {

            @Override
            public void run() throws InterruptedException {

                LiveHeaderDetectionWizard wizard = new LiveHeaderDetectionWizard();
                ArrayList<ReconnectResult> scripts;
                try {
                    scripts = wizard.runOnlineScan(new ProcessCallBack() {

                        public void setStatusString(Object caller, String string) {
                            setBarText(string);
                        }

                        public void setProgress(Object caller, int percent) {
                            setBarProgress(percent);
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

                        public void setStatus(Object caller, Object statusObject) {
                            if (caller instanceof LiveHeaderDetectionWizard && statusObject instanceof ArrayList) {
                                @SuppressWarnings("unchecked")
                                ArrayList<LiveHeaderReconnectResult> foundScripts = (ArrayList<LiveHeaderReconnectResult>) statusObject;
                                setInterruptEnabled(foundScripts);
                            }
                        }

                    });

                    if (scripts != null && scripts.size() > 0) {
                        int i = 1;
                        for (ReconnectResult found : scripts) {
                            setBarText(T._.AutoDetectAction_run_optimize(found.getInvoker().getName()));
                            found.optimize();
                            setBarProgress(i++ / scripts.size());
                        }

                        Collections.sort(scripts, new Comparator<ReconnectResult>() {

                            public int compare(ReconnectResult o1, ReconnectResult o2) {
                                return new Long(o2.getAverageSuccessDuration()).compareTo(new Long(o1.getAverageSuccessDuration()));
                            }
                        });
                        System.out.println("Scripts " + scripts);
                        scripts.get(0).getInvoker().getPlugin().setSetup(scripts.get(0));

                    } else {
                        Dialog.getInstance().showErrorDialog(T._.AutoDetectAction_run_failed());
                    }
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    try {
                        Dialog.getInstance().showConfirmDialog(Dialog.BUTTONS_HIDE_CANCEL, _GUI._.literall_error(), T._.LiveHeaderDetectionWizard_runOnlineScan_notalive(), NewTheme.I().getIcon("error", 32), null, null);
                    } catch (DialogClosedException e1) {
                        e.printStackTrace();
                    } catch (DialogCanceledException e1) {
                        e.printStackTrace();
                    }

                }

            }

        };
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

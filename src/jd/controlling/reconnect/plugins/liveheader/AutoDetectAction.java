package jd.controlling.reconnect.plugins.liveheader;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.AbstractAction;

import jd.controlling.reconnect.ReconnectConfig;
import jd.controlling.reconnect.ReconnectResult;
import jd.controlling.reconnect.WizardUtils;
import jd.controlling.reconnect.plugins.liveheader.translate.T;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.event.ProcessCallBackAdapter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
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

        ReconnectFindDialog d = new ReconnectFindDialog() {

            @Override
            public void run() throws InterruptedException {

                LiveHeaderDetectionWizard wizard = new LiveHeaderDetectionWizard();

                if (WizardUtils.modemCheck()) return;

                try {
                    final ArrayList<ReconnectResult> scripts = wizard.runOnlineScan(new ProcessCallBackAdapter() {

                        public void setStatusString(Object caller, String string) {
                            setBarText(string);
                        }

                        public void setProgress(Object caller, int percent) {
                            setBarProgress(percent);
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
                        optimize(scripts);
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
                } catch (InterruptedException e) {
                    Log.L.finer("Interrupted");
                } catch (Throwable e) {
                    // might get a RemoteCallCommunicationException
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

            public void optimize(final ArrayList<ReconnectResult> scripts) throws InterruptedException {
                if (JsonConfig.create(ReconnectConfig.class).getOptimizationRounds() > 1) {

                    long bestTime = Long.MAX_VALUE;
                    long optiduration = 0;
                    for (ReconnectResult found : scripts) {

                        bestTime = Math.min(bestTime, found.getSuccessDuration());
                        optiduration += found.getSuccessDuration() * (JsonConfig.create(ReconnectConfig.class).getOptimizationRounds() - 1) * 1.5;
                    }
                    try {

                        Dialog.getInstance().showConfirmDialog(0, _GUI._.AutoDetectAction_actionPerformed_dooptimization_title(), _GUI._.AutoDetectAction_actionPerformed_dooptimization_msg(scripts.size(), TimeFormatter.formatMilliSeconds(optiduration, 0), TimeFormatter.formatMilliSeconds(bestTime, 0)), NewTheme.I().getIcon("ok", 32), _GUI._.AutoDetectAction_run_optimization(), _GUI._.AutoDetectAction_skip_optimization());
                        setBarProgress(0);
                        for (int ii = 0; ii < scripts.size(); ii++) {
                            ReconnectResult found = scripts.get(ii);
                            setBarText(_GUI._.AutoDetectAction_run_optimize(found.getInvoker().getName()));
                            final int step = ii;
                            found.optimize(new ProcessCallBackAdapter() {

                                public void setProgress(Object caller, int percent) {
                                    setBarProgress((step) * (100 / (scripts.size())) + percent / (scripts.size()));
                                }

                                public void setStatusString(Object caller, String string) {
                                    setBarText(_GUI._.AutoDetectAction_run_optimize(string));
                                }

                            });

                        }
                    } catch (DialogNoAnswerException e) {

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

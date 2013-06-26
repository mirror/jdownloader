package jd.controlling.reconnect.pluginsinc.upnp;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import jd.controlling.reconnect.ReconnectConfig;
import jd.controlling.reconnect.ReconnectResult;
import jd.controlling.reconnect.RouterUtils;
import jd.controlling.reconnect.pluginsinc.liveheader.ReconnectFindDialog;
import jd.controlling.reconnect.pluginsinc.upnp.translate.T;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.action.BasicAction;
import org.appwork.swing.components.tooltips.BasicTooltipFactory;
import org.appwork.uio.UIOManager;
import org.appwork.utils.event.ProcessCallBack;
import org.appwork.utils.event.ProcessCallBackAdapter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;

public class AutoDetectUpnpAction extends BasicAction {
    private UPNPRouterPlugin plugin;
    private boolean          modemChoose;

    public AutoDetectUpnpAction(UPNPRouterPlugin upnpRouterPlugin) {
        super();
        plugin = upnpRouterPlugin;
        putValue(NAME, T._.auto());
        putValue(SMALL_ICON, NewTheme.I().getIcon("wizard", 18));
        setTooltipFactory(new BasicTooltipFactory(getName(), T._.AutoDetectUpnpAction_AutoDetectUpnpAction_(), NewTheme.I().getIcon("upnp", 32)));
    }

    public void actionPerformed(ActionEvent e) {
        LogSource logger = LogController.getInstance().getLogger("UPNPReconnect");
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
        logger.info("RUN");
        ReconnectFindDialog d = new ReconnectFindDialog() {

            @Override
            public void run() throws InterruptedException {

                final java.util.List<ReconnectResult> scripts = plugin.runDetectionWizard(new ProcessCallBack() {

                    public void showDialog(Object caller, String title, String message, ImageIcon icon) {
                        try {
                            Dialog.getInstance().showConfirmDialog(UIOManager.BUTTONS_HIDE_CANCEL, title, message, icon, null, null);
                        } catch (DialogClosedException e) {
                            e.printStackTrace();
                        } catch (DialogCanceledException e) {
                            e.printStackTrace();
                        }
                    }

                    public void setStatusString(Object caller, String string) {
                        setBarText(string);
                    }

                    public void setProgress(Object caller, int percent) {
                        setBarProgress(percent);
                    }

                    public void setStatus(Object caller, Object statusObject) {
                    }
                });

                if (scripts != null && scripts.size() > 0) {

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

package jd.controlling.reconnect.pluginsinc.upnp;

import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.AbstractAction;

import jd.controlling.reconnect.ProcessCallBack;
import jd.controlling.reconnect.ProcessCallBackAdapter;
import jd.controlling.reconnect.ReconnectConfig;
import jd.controlling.reconnect.ReconnectResult;
import jd.controlling.reconnect.RouterUtils;
import jd.controlling.reconnect.pluginsinc.liveheader.ReconnectFindDialog;
import jd.controlling.reconnect.pluginsinc.upnp.translate.T;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.action.BasicAction;
import org.appwork.swing.components.tooltips.BasicTooltipFactory;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.logging.LogController;

public class AutoDetectUpnpAction extends BasicAction {
    private UPNPRouterPlugin plugin;
    private boolean          modemChoose;

    public AutoDetectUpnpAction(UPNPRouterPlugin upnpRouterPlugin) {
        super();
        plugin = upnpRouterPlugin;
        putValue(NAME, T.T.auto());
        putValue(SMALL_ICON, new AbstractIcon("wizard", 18));
        setTooltipFactory(new BasicTooltipFactory(getName(), T.T.AutoDetectUpnpAction_AutoDetectUpnpAction_(), new AbstractIcon("upnp", 32)));
    }

    public void actionPerformed(ActionEvent e) {
        LogSource logger = LogController.getInstance().getLogger("UPNPReconnect");
        final ConfirmDialog confirm = new ConfirmDialog(0, _GUI.T.AutoSetupAction_actionPerformed_warn_title(), _GUI.T.AutoSetupAction_actionPerformed_warn_message(), new AbstractIcon(IconKey.ICON_WARNING, 32), _GUI.T.AutoSetupAction_actionPerformed_warn_message_continue(), null) {
            @Override
            protected int getPreferredWidth() {
                return 750;
            }

            @Override
            public boolean isRemoteAPIEnabled() {
                return false;
            }
        };
        try {
            UIOManager.I().show(ConfirmDialogInterface.class, confirm).throwCloseExceptions();
        } catch (Throwable e2) {
            logger.log(e2);
            return;
        }
        try {
            if (RouterUtils.isWindowsModemConnection()) {
                modemChoose = false;
                final ConfirmDialog d = new ConfirmDialog(0, _GUI.T.literally_warning(), _GUI.T.AutoSetupAction_actionPerformed_modem(), new AbstractIcon("modem", 32), _GUI.T.AutoSetupAction_actionPerformed_dont_know(), _GUI.T.AutoSetupAction_actionPerformed_router());
                d.setLeftActions(new AbstractAction() {
                    {
                        putValue(NAME, _GUI.T.AutoSetupAction_actionPerformed_choose_modem());
                    }

                    public void actionPerformed(ActionEvent e) {
                        modemChoose = true;
                        d.dispose();
                    }
                });
                try {
                    Dialog.getInstance().showDialog(d);
                    if (modemChoose) {
                        Dialog.getInstance().showErrorDialog(_GUI.T.AutoSetupAction_actionPerformed_noautoformodem());
                        CrossSystem.openURL("http://jdownloader.org/knowledge/wiki/reconnect/modem");
                        return;
                    }
                    CrossSystem.openURL("http://jdownloader.org/knowledge/wiki/reconnect/modem");
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
                final AtomicBoolean methodConfirmEnabled = new AtomicBoolean(true);
                final java.util.List<ReconnectResult> scripts = plugin.runDetectionWizard(new ProcessCallBack() {
                    public void setStatusString(Object caller, String string) {
                        setBarText(string);
                    }

                    public void setProgress(Object caller, int percent) {
                        setBarProgress(percent);
                    }

                    public void setStatus(Object caller, Object statusObject) {
                    }

                    @Override
                    public void setMethodConfirmEnabled(boolean b) {
                        methodConfirmEnabled.set(b);
                    }

                    @Override
                    public boolean isMethodConfirmEnabled() {
                        return methodConfirmEnabled.get();
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
                            UIOManager.I().show(ConfirmDialogInterface.class, new ConfirmDialog(0, _GUI.T.AutoDetectAction_actionPerformed_dooptimization_title(), _GUI.T.AutoDetectAction_actionPerformed_dooptimization_msg(scripts.size(), TimeFormatter.formatMilliSeconds(optiduration, 0), TimeFormatter.formatMilliSeconds(bestTime, 0)), new AbstractIcon("ok", 32), _GUI.T.AutoDetectAction_run_optimization(), _GUI.T.AutoDetectAction_skip_optimization())).throwCloseExceptions();
                            setBarProgress(0);
                            for (int ii = 0; ii < scripts.size(); ii++) {
                                ReconnectResult found = scripts.get(ii);
                                setBarText(_GUI.T.AutoDetectAction_run_optimize(found.getInvoker().getName()));
                                final int step = ii;
                                found.optimize(new ProcessCallBackAdapter() {
                                    public void setProgress(Object caller, int percent) {
                                        setBarProgress((step) * (100 / (scripts.size())) + percent / (scripts.size()));
                                    }

                                    public void setStatusString(Object caller, String string) {
                                        setBarText(_GUI.T.AutoDetectAction_run_optimize(string));
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
                    UIOManager.I().showErrorMessage(T.T.AutoDetectAction_run_failed2());
                }
            }
        };
        try {
            UIOManager.I().show(null, d);
        } finally {
            System.out.println("CLOSED");
        }
    }
}

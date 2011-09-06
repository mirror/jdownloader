package jd.gui.swing.jdgui.views.settings.panels.reconnect;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import jd.controlling.reconnect.ReconnectConfig;
import jd.controlling.reconnect.ReconnectPluginController;
import jd.controlling.reconnect.ReconnectResult;
import jd.controlling.reconnect.RouterPlugin;
import jd.controlling.reconnect.RouterUtils;
import jd.controlling.reconnect.plugins.liveheader.ReconnectFindDialog;
import jd.controlling.reconnect.plugins.liveheader.translate.T;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.event.ProcessCallBack;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.translate._JDT;

public class AutoSetupAction extends AbstractAction {
    private boolean modemChoose;

    {
        putValue(NAME, _JDT._.reconnectmanager_wizard());
        putValue(SMALL_ICON, NewTheme.I().getIcon("wizard", 20));

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
        boolean pre = JsonConfig.create(ReconnectConfig.class).isIPCheckGloballyDisabled();
        try {
            if (pre) {

                Dialog.getInstance().showConfirmDialog(0, _GUI._.literally_warning(), T._.ipcheck());
                JsonConfig.create(ReconnectConfig.class).setIPCheckGloballyDisabled(false);

            }

            ReconnectFindDialog d = new ReconnectFindDialog() {

                @Override
                public void run() throws InterruptedException {

                    ArrayList<ReconnectResult> scripts;

                    scripts = ReconnectPluginController.getInstance().autoFind(new ProcessCallBack() {

                        public void showDialog(Object caller, String title, String message, ImageIcon icon) {
                            try {
                                Dialog.getInstance().showConfirmDialog(Dialog.BUTTONS_HIDE_CANCEL, title, message, icon, null, null);
                            } catch (DialogClosedException e) {
                                e.printStackTrace();
                            } catch (DialogCanceledException e) {
                                e.printStackTrace();
                            }
                        }

                        public void setStatusString(Object caller, String string) {
                            if (caller instanceof RouterPlugin) {
                                setSubStatusHeader(((RouterPlugin) caller).getName());
                            }
                            setBarText(string);
                        }

                        public void setProgress(Object caller, int percent) {
                            setBarProgress(percent);
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

                }

            };

            Dialog.getInstance().showDialog(d);

        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        } finally {

            JsonConfig.create(ReconnectConfig.class).setIPCheckGloballyDisabled(pre);
            if (pre) {
                Dialog.getInstance().showMessageDialog(T._.ipcheckreverted());
            }
        }
    }

}

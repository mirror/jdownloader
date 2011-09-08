package jd.gui.swing.jdgui.views.settings.panels.reconnect;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.reconnect.ReconnectConfig;
import jd.controlling.reconnect.ReconnectPluginController;
import jd.controlling.reconnect.ReconnectResult;
import jd.controlling.reconnect.RouterPlugin;
import jd.controlling.reconnect.WizardUtils;
import jd.controlling.reconnect.ipcheck.IPController;
import jd.controlling.reconnect.plugins.liveheader.LiveHeaderReconnectResult;
import jd.controlling.reconnect.plugins.liveheader.ReconnectFindDialog;
import jd.controlling.reconnect.plugins.liveheader.translate.T;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.action.BasicAction;
import org.appwork.swing.components.tooltips.BasicTooltipFactory;
import org.appwork.utils.event.ProcessCallBackAdapter;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.translate._JDT;

public class AutoSetupAction extends BasicAction {

    public AutoSetupAction() {
        putValue(NAME, _JDT._.reconnectmanager_wizard());
        putValue(SMALL_ICON, NewTheme.I().getIcon("wizard", 20));

        this.setTooltipFactory(new BasicTooltipFactory(getName(), _GUI._.AutoSetupAction_tt(), NewTheme.I().getIcon("wizard", 32)));

    }

    public void actionPerformed(ActionEvent e) {

        boolean pre = JsonConfig.create(ReconnectConfig.class).isIPCheckGloballyDisabled();
        try {
            if (pre) {

                Dialog.getInstance().showConfirmDialog(0, _GUI._.literally_warning(), T._.ipcheck());
                JsonConfig.create(ReconnectConfig.class).setIPCheckGloballyDisabled(false);

            }

            ReconnectFindDialog d = new ReconnectFindDialog() {

                @Override
                public void run() throws InterruptedException {
                    setBarText(_GUI._.LiveaheaderDetection_wait_for_online());
                    IPController.getInstance().waitUntilWeAreOnline();
                    if (WizardUtils.modemCheck()) return;

                    final ArrayList<ReconnectResult> scripts = ReconnectPluginController.getInstance().autoFind(new ProcessCallBackAdapter() {

                        public void setStatusString(Object caller, String string) {
                            if (caller instanceof RouterPlugin) {
                                setSubStatusHeader(((RouterPlugin) caller).getName());
                            }
                            setBarText(string);
                        }

                        public void setProgress(Object caller, int percent) {
                            setBarProgress(percent);
                        }

                        public void setStatus(Object caller, Object statusObject) {
                            if (statusObject instanceof ArrayList) {
                                try {
                                    @SuppressWarnings("unchecked")
                                    ArrayList<LiveHeaderReconnectResult> foundScripts = (ArrayList<LiveHeaderReconnectResult>) statusObject;
                                    if (foundScripts.size() > 0) {
                                        setInterruptEnabled(foundScripts);
                                    }
                                } catch (Throwable e) {

                                }
                            }
                        }
                    });

                    if (scripts != null && scripts.size() > 0) {

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

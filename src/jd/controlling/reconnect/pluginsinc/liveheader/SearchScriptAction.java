package jd.controlling.reconnect.pluginsinc.liveheader;

import java.awt.event.ActionEvent;

import jd.controlling.reconnect.pluginsinc.liveheader.recoll.RecollController;
import jd.controlling.reconnect.pluginsinc.liveheader.remotecall.RouterData;
import jd.controlling.reconnect.pluginsinc.liveheader.translate.T;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.action.BasicAction;
import org.appwork.swing.components.tooltips.BasicTooltipFactory;
import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

public class SearchScriptAction extends BasicAction {

    private LiveHeaderReconnect liveHeaderReconnect;
    protected RouterData        myConnectionInfo;

    public SearchScriptAction(LiveHeaderReconnect liveHeaderReconnect) {
        this.liveHeaderReconnect = liveHeaderReconnect;
        putValue(NAME, T._.SearchScriptAction());
        putValue(SMALL_ICON, new AbstractIcon(IconKey.ICON_SEARCH, 18));

        setTooltipFactory(new BasicTooltipFactory(getName(), T._.SearchScriptAction_tt(), new AbstractIcon("edit", 32)));
        setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {

        ProgressGetter pg = new ProgressGetter() {

            @Override
            public void run() throws Exception {
                if (myConnectionInfo == null) {

                    final RouterData rd = new LiveHeaderDetectionWizard() {
                        protected void scanRemoteInfo() {
                        }
                    }.collectRouterDataInfo();
                    rd.setIsp(RecollController.getInstance().getIsp());
                    myConnectionInfo = rd;
                }

                new Thread() {
                    {
                        setDaemon(true);
                    }

                    public void run() {
                        SearchScriptDialog d;
                        try {
                            UIOManager.I().show(null, d = new SearchScriptDialog(myConnectionInfo)).throwCloseExceptions();

                            LiveHeaderReconnectSettings config = JsonConfig.create(LiveHeaderReconnectSettings.class);
                            config.setRouterData(d.getRouterData());

                            // changed script.reset router sender state
                            if (d.getRouterData().getScript() != null && d.getRouterData().getScript().equals(config.getScript())) {
                                config.setAlreadySendToCollectServer3(false);
                            }

                            config.setScript(d.getRouterData().getScript());
                        } catch (DialogClosedException e) {
                            e.printStackTrace();
                        } catch (DialogCanceledException e) {
                            e.printStackTrace();
                        }

                    };
                }.start();

            }

            @Override
            public String getString() {
                return null;
            }

            @Override
            public int getProgress() {
                return -1;
            }

            @Override
            public String getLabelString() {
                return null;
            }
        };

        ProgressDialog d = new ProgressDialog(pg, UIOManager.BUTTONS_HIDE_OK, T._.searching(), _GUI._.lit_please_wait(), new AbstractIcon(IconKey.ICON_WAIT, 32), null, null);
        UIOManager.I().show(null, d);

    }

}

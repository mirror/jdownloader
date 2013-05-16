package jd.gui.swing.jdgui.components.toolbar.actions;

import java.awt.event.ActionEvent;

import jd.controlling.IOEQ;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.event.DownloadWatchdogListener;

import org.appwork.uio.UIOManager;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.gui.toolbar.action.ToolBarAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.images.NewTheme;

public class StopDownloadsAction extends ToolBarAction implements DownloadWatchdogListener {

    public StopDownloadsAction(SelectionInfo<?, ?> selection) {
        setIconKey("media-playback-stop");
        DownloadWatchDog.getInstance().getEventSender().addListener(this, true);
        DownloadWatchDog.getInstance().notifyCurrentState(this);
    }

    public void actionPerformed(ActionEvent e) {
        IOEQ.add(new Runnable() {

            public void run() {
                if (DownloadWatchDog.getInstance().getStateMachine().hasPassed(DownloadWatchDog.STOPPING_STATE)) return;
                int count = DownloadWatchDog.getInstance().getNonResumableRunningCount();
                if (count > 0) {
                    if (!UIOManager.I().showConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.lit_are_you_sure(), _GUI._.StopDownloadsAction_run_msg_(SizeFormatter.formatBytes(DownloadWatchDog.getInstance().getNonResumableBytes()), count), NewTheme.I().getIcon("stop", 32), _GUI._.lit_yes(), _GUI._.lit_no())) {

                    return; }
                }

                DownloadWatchDog.getInstance().stopDownloads();
            }

        });
    }

    @Override
    public String createTooltip() {
        return _GUI._.action_stop_downloads_tooltip();
    }

    @Override
    public void onDownloadWatchdogStateIsStopping() {

    }

    @Override
    public void onDownloadWatchdogStateIsStopped() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                setEnabled(false);
            }
        };
    }

    @Override
    public void onDownloadWatchdogStateIsRunning() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                setEnabled(true);
            }
        };
    }

    @Override
    public void onDownloadWatchdogStateIsPause() {
    }

    @Override
    public void onDownloadWatchdogStateIsIdle() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                setEnabled(false);
            }
        };
    }

    @Override
    public void onDownloadWatchdogDataUpdate() {
    }

}

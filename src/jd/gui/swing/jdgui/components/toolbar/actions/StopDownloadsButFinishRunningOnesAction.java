package jd.gui.swing.jdgui.components.toolbar.actions;

import java.awt.event.ActionEvent;

import jd.controlling.downloadcontroller.DownloadSession;
import jd.controlling.downloadcontroller.DownloadSession.STOPMARK;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.DownloadWatchDogJob;
import jd.controlling.downloadcontroller.event.DownloadWatchdogListener;

import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.gui.toolbar.action.ToolBarAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class StopDownloadsButFinishRunningOnesAction extends ToolBarAction implements DownloadWatchdogListener {

    public StopDownloadsButFinishRunningOnesAction(SelectionInfo<?, ?> selection) {
        setIconKey("stop_conditional");
        setEnabled(false);
        DownloadWatchDog.getInstance().getEventSender().addListener(this, true);
        DownloadWatchDog.getInstance().notifyCurrentState(this);
        setHideIfDownloadsAreStopped(false);
    }

    public void actionPerformed(ActionEvent e) {
        DownloadWatchDog.getInstance().enqueueJob(new DownloadWatchDogJob() {

            @Override
            public void execute(DownloadSession currentSession) {
                currentSession.setStopMark(STOPMARK.RANDOM);
            }
        });
    }

    private boolean            hideIfDownloadsAreStopped     = false;
    public static final String HIDE_IF_DOWNLOADS_ARE_STOPPED = "HideIfDownloadsAreStopped";

    @Customizer(name = "Hide if downloads are not running")
    public boolean isHideIfDownloadsAreStopped() {
        return hideIfDownloadsAreStopped;
    }

    public void setHideIfDownloadsAreStopped(boolean showIfDownloadsAreRunning) {
        this.hideIfDownloadsAreStopped = showIfDownloadsAreRunning;
        if (isHideIfDownloadsAreStopped() && !DownloadWatchDog.getInstance().isRunning()) {
            setVisible(false);
        } else {
            setVisible(true);
        }
    }

    @Override
    public String createTooltip() {
        return _GUI._.StopDownloadsAction_createTooltip();
    }

    @Override
    public void onDownloadWatchdogStateIsStopping() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                setEnabled(false);

            }
        };

    }

    @Override
    public void onDownloadWatchdogStateIsStopped() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                setEnabled(false);
                if (isHideIfDownloadsAreStopped()) {
                    setVisible(false);
                }
            }
        };
    }

    @Override
    public void onDownloadWatchdogStateIsRunning() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                setEnabled(true);
                setVisible(true);
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
                if (isHideIfDownloadsAreStopped()) {
                    setVisible(false);
                }
            }
        };
    }

    @Override
    public void onDownloadWatchdogDataUpdate() {
    }

}

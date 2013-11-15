package org.jdownloader.gui.notify.downloads;

import java.util.List;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.downloadcontroller.event.DownloadWatchdogListener;

import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.notify.AbstractBubbleSupport;
import org.jdownloader.gui.notify.BasicNotify;
import org.jdownloader.gui.notify.BubbleNotify;
import org.jdownloader.gui.notify.Element;
import org.jdownloader.gui.notify.gui.CFG_BUBBLE;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class StartStopPauseBubbleSupport extends AbstractBubbleSupport implements DownloadWatchdogListener {

    public StartStopPauseBubbleSupport() {
        super(_GUI._.plugins_optional_JDLightTray_ballon_startpausestop2(), CFG_BUBBLE.BUBBLE_NOTIFY_START_PAUSE_STOP_ENABLED);
        DownloadWatchDog.getInstance().getEventSender().addListener(this, true);
    }

    @Override
    public List<Element> getElements() {
        return null;
    }

    @Override
    public void onDownloadWatchdogDataUpdate() {
    }

    @Override
    public void onDownloadWatchdogStateIsIdle() {
    }

    @Override
    public void onDownloadWatchdogStateIsPause() {
        if (!CFG_BUBBLE.BUBBLE_NOTIFY_START_PAUSE_STOP_ENABLED.isEnabled()) {

        return; }
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                BasicNotify no = new BasicNotify(_GUI._.download_paused(), _GUI._.download_paused_msg(), NewTheme.I().getIcon(IconKey.ICON_MEDIA_PLAYBACK_PAUSE, 24));
                show(no);
            }
        };
    }

    @Override
    public void onDownloadWatchdogStateIsRunning() {
        if (!CFG_BUBBLE.BUBBLE_NOTIFY_START_PAUSE_STOP_ENABLED.isEnabled()) {

        return; }
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                BasicNotify no = new BasicNotify(_GUI._.download_start(), _GUI._.download_start_msg(), NewTheme.I().getIcon(IconKey.ICON_MEDIA_PLAYBACK_START, 24));
                BubbleNotify.getInstance().show(no);
            }
        };
    }

    @Override
    public void onDownloadWatchdogStateIsStopped() {

        if (!CFG_BUBBLE.BUBBLE_NOTIFY_START_PAUSE_STOP_ENABLED.isEnabled()) {

        return; }
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                BasicNotify no = new BasicNotify(_GUI._.download_stopped(), _GUI._.download_stopped_msg(), NewTheme.I().getIcon(IconKey.ICON_MEDIA_PLAYBACK_STOP, 24));
                BubbleNotify.getInstance().show(no);
            }
        };
    }

    @Override
    public void onDownloadWatchdogStateIsStopping() {

    }

    @Override
    public void onDownloadControllerStart(SingleDownloadController downloadController) {
    }

    @Override
    public void onDownloadControllerStopped(SingleDownloadController downloadController) {
    }

}

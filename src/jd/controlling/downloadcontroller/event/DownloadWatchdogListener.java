package jd.controlling.downloadcontroller.event;

import java.util.EventListener;

public interface DownloadWatchdogListener extends EventListener {

    void onDownloadWatchdogDataUpdate();

    void onDownloadWatchdogStateIsIdle();

    void onDownloadWatchdogStateIsPause();

    void onDownloadWatchdogStateIsRunning();

    void onDownloadWatchdogStateIsStopped();

    void onDownloadWatchdogStateIsStopping();

}
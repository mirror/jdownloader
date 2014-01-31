package jd.controlling.downloadcontroller.event;

import java.util.EventListener;

import jd.controlling.downloadcontroller.DownloadLinkCandidate;
import jd.controlling.downloadcontroller.DownloadLinkCandidateResult;
import jd.controlling.downloadcontroller.SingleDownloadController;

public interface DownloadWatchdogListener extends EventListener {

    void onDownloadWatchdogDataUpdate();

    void onDownloadWatchdogStateIsIdle();

    void onDownloadWatchdogStateIsPause();

    void onDownloadWatchdogStateIsRunning();

    void onDownloadWatchdogStateIsStopped();

    void onDownloadWatchdogStateIsStopping();

    void onDownloadControllerStart(SingleDownloadController downloadController, DownloadLinkCandidate candidate);

    void onDownloadControllerStopped(SingleDownloadController downloadController, DownloadLinkCandidate candidate, DownloadLinkCandidateResult result);
}
package jd.controlling.downloadcontroller.event;

import jd.controlling.downloadcontroller.SingleDownloadController;

import org.appwork.utils.event.Eventsender;

public class DownloadWatchdogEventSender extends Eventsender<DownloadWatchdogListener, DownloadWatchdogEvent> {

    @Override
    protected void fireEvent(DownloadWatchdogListener listener, DownloadWatchdogEvent event) {
        switch (event.getType()) {
        case DATA_UPDATE:
            listener.onDownloadWatchdogDataUpdate();
            break;
        case STATE_IDLE:
            listener.onDownloadWatchdogStateIsIdle();
            break;
        case STATE_PAUSE:
            listener.onDownloadWatchdogStateIsPause();
            break;
        case STATE_RUNNING:
            listener.onDownloadWatchdogStateIsRunning();
            break;
        case STATE_STOPPED:
            listener.onDownloadWatchdogStateIsStopped();
            break;
        case STATE_STOPPING:
            listener.onDownloadWatchdogStateIsStopping();
            break;
        case LINK_STARTED:
            listener.onDownloadControllerStart((SingleDownloadController) event.getParameter());
            break;
        case LINK_STOPPED:
            listener.onDownloadControllerStopped((SingleDownloadController) event.getParameter());
            break;
        // fill
        default:
            System.out.println("Unhandled Event: " + event);
        }
    }
}
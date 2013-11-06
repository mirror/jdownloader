package org.jdownloader.controlling.download;

import org.appwork.utils.event.Eventsender;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.logging.LogController;

public class DownloadControllerEventSender extends Eventsender<DownloadControllerListener, DownloadControllerEvent> {

    private LogSource logger;

    public DownloadControllerEventSender() {
        logger = LogController.getInstance().getLogger(DownloadControllerEventSender.class.getName());
    }

    @Override
    protected void fireEvent(DownloadControllerListener listener, DownloadControllerEvent event) {
        try {
            event.sendToListener(listener);
        } catch (RuntimeException e) {
            logger.log(e);

        }
    }
}
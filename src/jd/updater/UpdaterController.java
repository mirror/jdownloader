package jd.updater;

import org.appwork.utils.event.DefaultEventSender;

public class UpdaterController {
    private static final UpdaterController INSTANCE = new UpdaterController();

    public static UpdaterController getInstance() {
        return UpdaterController.INSTANCE;
    }

    private DefaultEventSender<UpdaterEvent> eventSender;

    public DefaultEventSender<UpdaterEvent> getEventSender() {
        return eventSender;
    }

    private UpdaterController() {
        eventSender = new DefaultEventSender<UpdaterEvent>();
    }

    public void requestExit() {
        eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.EXIT_REQUEST, null));
    }
}

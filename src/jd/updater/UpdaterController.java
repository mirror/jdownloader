package jd.updater;

import org.appwork.storage.JSonStorage;
import org.appwork.utils.event.DefaultEventSender;

public class UpdaterController {
    private static final UpdaterController INSTANCE = new UpdaterController();

    public static UpdaterController getInstance() {
        return UpdaterController.INSTANCE;
    }

    private DefaultEventSender<UpdaterEvent> eventSender;
    private WebUpdaterOptions                options;

    public DefaultEventSender<UpdaterEvent> getEventSender() {
        return eventSender;
    }

    private UpdaterController() {
        eventSender = new DefaultEventSender<UpdaterEvent>();

    }

    public void requestExit() {
        eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.EXIT_REQUEST, null));
    }

    public void start(WebUpdaterOptions options) {
        this.options = options;
        setBranch(options.getBranch());
        this.waitDelay();
    }

    private void waitDelay() {
    }

    private void setBranch(String branch) {
        JSonStorage.getPlainStorage("WEBUPDATE").put("BRANCHINUSE", branch);
        JSonStorage.getPlainStorage("WEBUPDATE").save();
        System.out.println("Switched branch: " + branch);
    }

    public String getBranch() {
        return JSonStorage.getPlainStorage("WEBUPDATE").get("BRANCHINUSE", (String) null);

    }
}

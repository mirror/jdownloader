package jd.updater;

import org.appwork.utils.event.SimpleEvent;

public class UpdaterEvent extends SimpleEvent<UpdaterController, Object, UpdaterEvent.Types> {

    public UpdaterEvent(UpdaterController caller, Types type, Object[] parameters) {
        super(caller, type, parameters);
    }

    public enum Types {
        EXIT_REQUEST

    }

}

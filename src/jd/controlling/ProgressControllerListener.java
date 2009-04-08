package jd.controlling;

import java.util.EventListener;

public interface ProgressControllerListener extends EventListener {
    public void handle_ProgressControllerEvent(ProgressControllerEvent event);
}

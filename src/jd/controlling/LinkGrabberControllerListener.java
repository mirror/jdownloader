package jd.controlling;

import java.util.EventListener;

public interface LinkGrabberControllerListener extends EventListener {
    public void onLinkGrabberControllerEvent(LinkGrabberControllerEvent event);
}

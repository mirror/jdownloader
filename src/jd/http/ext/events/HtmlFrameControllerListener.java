package jd.http.ext.events;

import java.util.EventListener;

public interface HtmlFrameControllerListener extends EventListener {

    public void onFrameEvent(HtmlFrameControllerEvent event);

}

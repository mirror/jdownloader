package jd.http.ext.events;

import java.util.EventListener;

public interface ExtBrowserListener extends EventListener {

    public void onFrameEvent(ExtBrowserEvent event);

}

package jd.controlling;

import java.util.EventListener;

public interface DownloadControllerListener extends EventListener {
    public void onDownloadControllerEvent(DownloadControllerEvent event);
}

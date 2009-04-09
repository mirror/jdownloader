package jd.plugins;

import java.util.EventListener;

public interface DownloadLinkListener extends EventListener {
    public void handle_DownloadLinkEvent(DownloadLinkEvent event);

}

package jd.controlling.linkchecker;

import java.util.EventListener;

public interface LinkCheckerListener extends EventListener {

    public void onLinkCheckerEvent(LinkCheckerEvent event);
}

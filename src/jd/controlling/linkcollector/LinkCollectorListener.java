package jd.controlling.linkcollector;

import java.util.EventListener;

public interface LinkCollectorListener extends EventListener {
    public void onLinkCollectorEvent(LinkCollectorEvent event);
}

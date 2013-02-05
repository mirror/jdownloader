package jd.controlling.linkcollector;

import java.util.EventListener;

public interface LinkCollectorApiEventListener extends EventListener {
    public void onLinkCollectorApiEvent(LinkCollectorApiEvent event);
}

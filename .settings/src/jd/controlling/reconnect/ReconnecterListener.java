package jd.controlling.reconnect;

import java.util.EventListener;

public interface ReconnecterListener extends EventListener {

    void onAfterReconnect(ReconnecterEvent event);

    void onBeforeReconnect(ReconnecterEvent event);

    void onReconnectSettingsUpdated(ReconnecterEvent event);

}

package org.jdownloader.settings.advanced;

import org.appwork.utils.event.Eventsender;

public class AdvancedConfigEventSender extends Eventsender<AdvancedConfigEventListener, AdvancedConfigEvent> {

    @Override
    protected void fireEvent(AdvancedConfigEventListener listener, AdvancedConfigEvent event) {
        switch (event.getType()) {
        case UPDATED:
            listener.onAdvancedConfigUpdate();
        }
    }

}

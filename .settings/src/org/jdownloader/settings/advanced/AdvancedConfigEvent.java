package org.jdownloader.settings.advanced;

import org.appwork.utils.event.SimpleEvent;

public class AdvancedConfigEvent extends SimpleEvent<AdvancedConfigManager, Object, AdvancedConfigEvent.Types> {

    public AdvancedConfigEvent(AdvancedConfigManager caller, Types type, Object... parameters) {
        super(caller, type, parameters);
    }

    public static enum Types {
        UPDATED

    }
}

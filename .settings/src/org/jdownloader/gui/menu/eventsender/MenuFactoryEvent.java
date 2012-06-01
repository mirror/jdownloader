package org.jdownloader.gui.menu.eventsender;

import org.appwork.utils.event.SimpleEvent;
import org.jdownloader.gui.menu.MenuContext;

public class MenuFactoryEvent extends SimpleEvent<Object, Object, MenuFactoryEvent.Type> {

    public static enum Type {
        EXTEND

    }

    public MenuFactoryEvent(Type type, MenuContext<?> context) {
        super(null, type, context);
    }

    public MenuContext<?> getContext() {
        return (MenuContext<?>) getParameters()[0];
    }
}
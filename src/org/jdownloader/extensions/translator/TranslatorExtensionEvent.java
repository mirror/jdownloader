package org.jdownloader.extensions.translator;

import org.appwork.utils.event.SimpleEvent;

public class TranslatorExtensionEvent extends SimpleEvent<Object, Object, TranslatorExtensionEvent.Type> {

    public static enum Type {
        LOADED_TRANSLATION,
        REFRESH_DATA,
    }

    public TranslatorExtensionEvent(Object caller, Type type, Object... parameters) {
        super(caller, type, parameters);
    }
}
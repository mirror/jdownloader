package org.jdownloader.settings.advanced;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.handler.KeyHandler;

public abstract class AdvandedValueEditor<T extends Object> {
    public abstract T edit(KeyHandler<String> keyHandler, T object) throws ValidationException;
}
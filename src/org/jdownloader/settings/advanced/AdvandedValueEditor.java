package org.jdownloader.settings.advanced;

import org.appwork.storage.config.ValidationException;

public abstract class AdvandedValueEditor<T extends Object> {
    public abstract T edit(T object) throws ValidationException;
}
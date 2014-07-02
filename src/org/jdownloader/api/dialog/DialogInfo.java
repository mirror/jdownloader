package org.jdownloader.api.dialog;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storable;
import org.appwork.utils.reflection.Clazz;
import org.jdownloader.myjdownloader.client.bindings.DialogStorable;

public class DialogInfo extends DialogStorable implements Storable {
    public DialogInfo(/* Storable */) {
        super();
    }

    public void put(final String key, final Object value) {
        if (value == null) {
            return;
        }
        if (Clazz.isPrimitive(value.getClass()) || value instanceof String) {
            properties.put(key, value.toString());
        } else {
            properties.put(key, JSonStorage.toString(value));
        }
    }
}

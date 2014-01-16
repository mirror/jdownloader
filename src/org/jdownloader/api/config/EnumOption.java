package org.jdownloader.api.config;

import org.appwork.storage.Storable;
import org.jdownloader.myjdownloader.client.bindings.EnumOptionStorable;

public class EnumOption extends EnumOptionStorable implements Storable {

    public EnumOption(/* storable */) {
        super();
    }

    public EnumOption(String name, String label) {
        super();
        setName(name);
        setLabel(label);
    }
}
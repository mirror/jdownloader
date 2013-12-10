package org.jdownloader.settings.advanced;

import java.io.File;

import org.appwork.remoteapi.annotations.AllowNonStorableObjects;
import org.appwork.storage.Storable;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.StringUtils;
import org.jdownloader.myjdownloader.client.bindings.AdvancedConfigEntryDataStorable;

public class AdvancedConfigAPIEntry extends AdvancedConfigEntryDataStorable implements Storable {

    public AdvancedConfigAPIEntry(AdvancedConfigEntry entry, boolean returnDescription, boolean addValue, boolean defaultValues) {
        KeyHandler<?> kh = ((AdvancedConfigEntry) entry).getKeyHandler();
        if (returnDescription && StringUtils.isNotEmpty(entry.getDescription())) {
            setDocs(entry.getDescription());
        }

        setType(kh.getTypeString());
        String i = kh.getStorageHandler().getConfigInterface().getName();
        setInterfaceName(i);

        setKey(kh.getKey());
        File expectedPath = Application.getResource("cfg/" + i);
        String storage = null;
        if (!expectedPath.equals(kh.getStorageHandler().getPath())) {
            storage = Files.getRelativePath(Application.getTemp().getParentFile(), kh.getStorageHandler().getPath());
            if (StringUtils.isEmpty(storage)) storage = kh.getStorageHandler().getPath().getAbsolutePath();
        }
        if (storage != null) {
            setStorage(storage);

        }
        Object value = kh.getValue();
        if (addValue) {
            setValue(value);

        }
        if (defaultValues) {
            Object def = entry.getDefault();
            boolean different;
            if (value == null) {
                different = (def != null);
            } else {
                different = (!value.equals(def));
            }

            setDefaultValue(def);

        }

    }

    @AllowNonStorableObjects
    @Override
    public Object getDefaultValue() {
        return super.getDefaultValue();
    }

    @Override
    @AllowNonStorableObjects
    public Object getValue() {
        return super.getValue();
    }

    @SuppressWarnings("unused")
    protected AdvancedConfigAPIEntry(/* Storable */) {
        super();
    }

}

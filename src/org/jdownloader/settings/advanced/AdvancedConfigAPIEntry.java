package org.jdownloader.settings.advanced;

import java.io.File;
import java.util.HashMap;

import org.appwork.storage.Storable;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.StringUtils;

public class AdvancedConfigAPIEntry extends HashMap<String, Object> implements Storable {

    private static final String DESCRIPTION  = "docs";
    private static final String STORAGE      = "storage";
    private static final String VALUE        = "value";

    private static final String DEFAULTVALUE = "default";
    private static final String TYPE         = "type";
    private static final String INTERFACE    = "interface";
    private static final String KEY          = "key";

    public AdvancedConfigAPIEntry(AdvancedConfigEntry entry, boolean returnDescription, boolean addValue, boolean defaultValues) {
        KeyHandler<?> kh = ((AdvancedConfigEntry) entry).getKeyHandler();
        if (returnDescription && StringUtils.isNotEmpty(entry.getDescription())) put(DESCRIPTION, entry.getDescription());

        put(TYPE, kh.getTypeString());
        String i = kh.getStorageHandler().getConfigInterface().getName();
        put(INTERFACE, i);
        put(KEY, kh.getKey());
        File expectedPath = Application.getResource("cfg/" + i);
        String storage = null;
        if (!expectedPath.equals(kh.getStorageHandler().getPath())) {
            storage = Files.getRelativePath(Application.getResource("tmp").getParentFile(), kh.getStorageHandler().getPath());
            if (StringUtils.isEmpty(storage)) storage = kh.getStorageHandler().getPath().getAbsolutePath();
        }
        if (storage != null) {
            put(STORAGE, storage);
        }
        Object value = kh.getValue();
        if (addValue) {
            put(VALUE, value);
        }
        if (defaultValues) {
            Object def = entry.getDefault();
            boolean different;
            if (value == null) {
                different = (def != null);
            } else {
                different = (!value.equals(def));
            }

            if (!addValue || different) {
                put(DEFAULTVALUE, def);
            }
        }

    }

    @SuppressWarnings("unused")
    private AdvancedConfigAPIEntry(/* Storable */) {
    }

}

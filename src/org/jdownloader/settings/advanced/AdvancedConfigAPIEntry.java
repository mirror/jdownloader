package org.jdownloader.settings.advanced;

import org.appwork.exceptions.WTFException;
import org.appwork.remoteapi.annotations.AllowNonStorableObjects;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storable;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.StringUtils;
import org.jdownloader.myjdownloader.client.bindings.AdvancedConfigEntryDataStorable;

public class AdvancedConfigAPIEntry extends AdvancedConfigEntryDataStorable implements Storable {
    public AdvancedConfigAPIEntry(AdvancedConfigEntry entry, boolean returnDescription, boolean addValue, boolean defaultValues) {
        final KeyHandler<?> kh = entry.getKeyHandler();
        if (returnDescription && entry.hasDescription() && StringUtils.isNotEmpty(entry.getDescription())) {
            setDocs(entry.getDescription());
        }
        setType(kh.getTypeString());
        final String configInterfaceName = kh.getStorageHandler().getConfigInterface().getName();
        setInterfaceName(configInterfaceName);
        setKey(createKey(kh));
        final AbstractType abstractType;
        try {
            abstractType = AbstractType.valueOf(kh.getAbstractType().name());
            setAbstractType(abstractType);
        } catch (Exception e) {
            throw new WTFException(e);
        }
        final String storage = kh.getStorageHandler().getStorageID();
        if (storage != null) {
            setStorage(storage);
        }
        if (addValue) {
            final Object value = kh.getValue();
            if (value != null && AbstractType.OBJECT.equals(abstractType)) {
                // TODO: dirty workaround for Map in webinterface (eg waitformap)
                setValue(JSonStorage.toString(value));
            } else {
                setValue(value);
            }
        }
        if (defaultValues) {
            final Object defValue = entry.getDefault();
            if (defValue != null && AbstractType.OBJECT.equals(abstractType)) {
                // TODO: dirty workaround for Map in webinterface (eg waitformap)
                setDefaultValue(JSonStorage.toString(defValue));
            } else {
                setDefaultValue(defValue);
            }
        }
    }

    protected String createKey(KeyHandler<?> kh) {
        final String getterName = kh.getGetMethod().getName();
        if (getterName.startsWith("is")) {
            return getterName.substring(2);
        } else if (getterName.startsWith("get")) {
            return getterName.substring(3);
        } else {
            return getterName;
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
    public AdvancedConfigAPIEntry(/* Storable */) {
        super();
    }
}

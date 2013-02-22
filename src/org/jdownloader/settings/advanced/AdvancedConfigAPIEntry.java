package org.jdownloader.settings.advanced;

import org.appwork.storage.Storable;
import org.appwork.storage.config.handler.KeyHandler;

public class AdvancedConfigAPIEntry implements Storable {

    private int storageID;

    public AdvancedConfigAPIEntry(AdvancedConfigEntry entry) {
        KeyHandler<?> kh = ((AdvancedConfigInterfaceEntry) entry).getKeyHandler();
        this.description = entry.getDescription();
        this.dataType = kh.getRawClass().getName();
        this.interfacename = kh.getStorageHandler().getConfigInterface().getName();
        this.setStorageID(kh.getStorageHandler().getId());
        this.key = kh.getKey();
    }

    @SuppressWarnings("unused")
    private AdvancedConfigAPIEntry(/* Storable */) {
    }

    private String key;
    private String interfacename;
    private String description;
    private String dataType;

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getInterfacename() {
        return interfacename;
    }

    public void setInterfacename(String interfacename) {
        this.interfacename = interfacename;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the storageID
     */
    public int getStorageID() {
        return storageID;
    }

    /**
     * @param storageID
     *            the storageID to set
     */
    public void setStorageID(int storageID) {
        this.storageID = storageID;
    }
}

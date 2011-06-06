package org.jdownloader.settings;

import java.util.ArrayList;
import java.util.HashMap;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AllowStorage;
import org.appwork.storage.config.annotations.CryptedStorage;
import org.appwork.storage.config.annotations.DefaultBooleanValue;

public interface AccountSettings extends ConfigInterface {
    // @AboutConfig
    @AllowStorage({ Object.class })
    @CryptedStorage(key = { 1, 6, 4, 5, 2, 7, 4, 3, 12, 61, 14, 75, -2, -7, -44, 33 })
    HashMap<String, ArrayList<AccountData>> getAccounts();

    @AllowStorage({ Object.class })
    @CryptedStorage(key = { 1, 6, 4, 5, 2, 7, 4, 3, 12, 61, 14, 75, -2, -7, -44, 33 })
    void setAccounts(HashMap<String, ArrayList<AccountData>> data);

    @AboutConfig
    @DefaultBooleanValue(false)
    boolean isUseAccountWithMostTrafficLeft();

    void setUseAccountWithMostTrafficLeft(boolean b);

}

package org.jdownloader.settings;

import java.util.ArrayList;
import java.util.HashMap;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.AllowStorage;
import org.appwork.storage.config.annotations.CryptedStorage;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.Description;
import org.appwork.storage.config.annotations.SpinnerValidator;

public interface AccountSettings extends ConfigInterface {
    // @AboutConfig
    @AllowStorage({ Object.class })
    @CryptedStorage(key = { 1, 6, 4, 5, 2, 7, 4, 3, 12, 61, 14, 75, -2, -7, -44, 33 })
    HashMap<String, ArrayList<AccountData>> getAccounts();

    @AllowStorage({ Object.class })
    void setAccounts(HashMap<String, ArrayList<AccountData>> data);

    @AboutConfig
    @DefaultIntValue(5)
    @SpinnerValidator(min = 1, max = Integer.MAX_VALUE)
    @Description("Default temporary disabled timeout (in minutes) on unknown errors while account checking!")
    int getTempDisableOnErrorTimeout();

    void setTempDisableOnErrorTimeout(int j);

}

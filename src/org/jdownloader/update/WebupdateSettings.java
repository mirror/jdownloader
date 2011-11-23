package org.jdownloader.update;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultLongValue;
import org.appwork.storage.config.annotations.Description;

public interface WebupdateSettings extends ConfigInterface {

    void setBranch(String branch);

    String getBranch();

    void setBranchInUse(String branch);

    String getBranchInUse();

    @DefaultLongValue(30 * 60 * 1000)
    @AboutConfig
    @Description("[MS] How often shall JD do a silent Updatecheck.")
    long getUpdateInterval();

    void setUpdateInterval(long intervalMS);
}

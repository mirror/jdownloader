package org.jdownloader.controlling.hosterrule;

import java.util.List;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultJsonObject;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;

public interface HosterRuleConfig extends ConfigInterface {
    @AboutConfig
    @DefaultJsonObject("[]")
    @DescriptionForConfigEntry("Account Manager -> Usage Rules")
    public List<AccountRuleStorable> getRules();

    public void setRules(List<AccountRuleStorable> rules);
}

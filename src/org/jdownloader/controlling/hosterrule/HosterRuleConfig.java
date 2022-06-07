package org.jdownloader.controlling.hosterrule;

import java.util.List;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultJsonObject;

public interface HosterRuleConfig extends ConfigInterface {
    @AboutConfig
    @DefaultJsonObject("[]")
    public List<AccountRuleStorable> getRules();

    public void setRules(List<AccountRuleStorable> rules);
}

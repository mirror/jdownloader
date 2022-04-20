package org.jdownloader.controlling.hosterrule;

import java.util.List;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;

public interface HosterRuleConfig extends ConfigInterface {
    @AboutConfig
    public List<AccountRuleStorable> getRules();

    public void setRules(List<AccountRuleStorable> rules);
}

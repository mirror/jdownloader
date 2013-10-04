package org.jdownloader.controlling.hosterrule;

import java.util.EventListener;

public interface HosterRuleControllerListener extends EventListener {

    void onRuleAdded(AccountUsageRule parameter);

    void onRuleDataUpdate();

    void onRuleRemoved(AccountUsageRule parameter);

}
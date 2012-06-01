package org.jdownloader.extensions.captchapush;

import jd.plugins.ExtensionConfigInterface;

import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DefaultStringValue;

public interface CaptchaPushConfig extends ExtensionConfigInterface {

    @DefaultStringValue("update1.jdownloader.org")
    String getBrokerHost();

    @DefaultIntValue(31263)
    int getBrokerPort();

    @DefaultStringValue("Test123")
    String getBrokerTopic();

    @DefaultIntValue(120)
    int getTimeout();

    boolean isSelected();

    void setBrokerHost(String brokerHost);

    void setBrokerPort(int brokerPort);

    void setBrokerTopic(String brokerTopic);

    void setTimeout(int timeout);

    void setSelected(boolean selected);

}
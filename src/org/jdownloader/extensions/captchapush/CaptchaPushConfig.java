package org.jdownloader.extensions.captchapush;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.DefaultIntValue;

public interface CaptchaPushConfig extends ConfigInterface {

    String getBrokerHost();

    @DefaultIntValue(19732)
    int getBrokerPort();

    String getBrokerTopic();

    @DefaultIntValue(120)
    int getTimeout();

    void setBrokerHost(String brokerHost);

    void setBrokerPort(int brokerPort);

    void setBrokerTopic(String brokerTopic);

    void setTimeout(int timeout);

}
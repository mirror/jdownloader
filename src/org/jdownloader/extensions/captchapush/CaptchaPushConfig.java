package org.jdownloader.extensions.captchapush;

import org.appwork.storage.config.ConfigInterface;

public interface CaptchaPushConfig extends ConfigInterface {

    String getBrokerHost();

    int getBrokerPort();

    String getBrokerTopic();

    int getTimeout();

    void setBrokerHost(String brokerHost);

    void setBrokerPort(int brokerPort);

    void setBrokerTopic(String brokerTopic);

    void setTimeout(int timeout);

}
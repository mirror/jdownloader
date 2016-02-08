package jd.plugins.components;

import org.appwork.storage.config.ConfigInterface;

public interface UsenetConfigInterface extends ConfigInterface {

    UsenetServer getUsenetServer();

    void setUsenetServer(UsenetServer server);
}

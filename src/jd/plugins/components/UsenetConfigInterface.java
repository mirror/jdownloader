package jd.plugins.components;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;

public interface UsenetConfigInterface extends ConfigInterface {

    @AboutConfig
    UsenetServer getUsenetServer();

    void setUsenetServer(UsenetServer server);
}

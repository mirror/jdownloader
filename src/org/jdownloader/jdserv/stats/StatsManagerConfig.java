package org.jdownloader.jdserv.stats;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultFactory;
import org.appwork.storage.config.defaults.AbstractDefaultFactory;
import org.appwork.utils.Hash;

public interface StatsManagerConfig extends ConfigInterface {
    class AnonymIDCreater extends AbstractDefaultFactory<String> {

        @Override
        public String getDefaultValue() {
            return Hash.getSHA256(System.currentTimeMillis() + "_" + System.nanoTime() + "_" + Math.random());
        }

    }

    @DefaultFactory(AnonymIDCreater.class)
    String getAnonymID();

    public void setAnonymID();

    @DefaultBooleanValue(false)
    boolean isFreshInstall();

    void setFreshInstall(boolean value);

}

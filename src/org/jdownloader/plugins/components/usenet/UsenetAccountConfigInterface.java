package org.jdownloader.plugins.components.usenet;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.AbstractCustomValueGetter;
import org.appwork.storage.config.annotations.CustomValueGetter;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.KeyHandler;
import org.jdownloader.plugins.config.AccountConfigInterface;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.translate._JDT;

public interface UsenetAccountConfigInterface extends AccountConfigInterface {
    static class Translation {
        public String getPort_description() {
            return "";
        }

        public String getHost_description() {
            return "";
        }

        public String getSSLEnabled_description() {
            return "";
        }

        public String getConnections_description() {
            return "";
        }

        public String getPort_label() {
            return _JDT.T.lit_port();
        }

        public String getHost_label() {
            return _JDT.T.lit_host();
        }

        public String getSSLEnabled_label() {
            return _JDT.T.lit_ssl_enabled();
        }

        public String getConnections_label() {
            return _JDT.T.lit_connections();
        }
    }

    public static final Translation TRANSLATION = new Translation();

    @AboutConfig
    @Order(10)
    @DescriptionForConfigEntry("Usenet Server address NOTE: host port and ssl must be a valid combination")
    String getHost();

    void setHost(String server);

    public static class MyPortGetter extends AbstractCustomValueGetter<Integer> {
        @Override
        public Integer getValue(KeyHandler<Integer> keyHandler, Integer value) {
            Integer port = value;
            if (port == null || port.intValue() <= 0) {
                boolean ssl = keyHandler.getStorageHandler().getKeyHandler("SSLEnabled", BooleanKeyHandler.class).isEnabled();
                return ssl ? 563 : 119;
            } else {
                return port;
            }
        }
    }

    @DescriptionForConfigEntry("Usenet Server port NOTE: host port and ssl must be a valid combination")
    @AboutConfig
    @Order(20)
    @CustomValueGetter(MyPortGetter.class)
    void setPort(int port);

    int getPort();

    @DescriptionForConfigEntry("Usenet SSL enabled NOTE: host port and ssl must be a valid combination")
    @DefaultBooleanValue(false)
    @AboutConfig
    @Order(30)
    void setSSLEnabled(boolean b);

    boolean isSSLEnabled();

    @AboutConfig
    @DefaultIntValue(1)
    @Order(40)
    void setConnections(int con);

    int getConnections();

}

package org.jdownloader.extensions.shutdown;

import org.jdownloader.extensions.shutdown.translate.T;

public enum Mode {
    SHUTDOWN(T.T.gui_config_jdshutdown_shutdown()),
    LOGOFF(T.T.gui_config_jdshutdown_logoff()),
    STANDBY(T.T.gui_config_jdshutdown_standby()),
    HIBERNATE(T.T.gui_config_jdshutdown_hibernate()),
    CLOSE(T.T.gui_config_jdshutdown_close());
    private final String translation;

    public final String getTranslation() {
        return translation;
    }

    private Mode(String translation) {
        this.translation = translation;
    }
}

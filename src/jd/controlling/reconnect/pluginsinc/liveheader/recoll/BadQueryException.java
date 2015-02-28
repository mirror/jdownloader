package jd.controlling.reconnect.pluginsinc.liveheader.recoll;

import java.io.IOException;

public class BadQueryException extends IOException {

    public BadQueryException(String ret) {
        super(ret);
    }

}

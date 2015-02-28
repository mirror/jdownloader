package jd.controlling.reconnect.pluginsinc.liveheader.recoll;

import java.io.IOException;

public class RetryIOException extends IOException {

    public RetryIOException(String ret) {
        super(ret);
    }

}

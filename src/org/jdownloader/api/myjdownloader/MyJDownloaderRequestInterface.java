package org.jdownloader.api.myjdownloader;

import java.io.IOException;

public interface MyJDownloaderRequestInterface {
    public MyJDownloaderHttpConnection getConnection();

    public String getRequestConnectToken();

    public long getRid() throws IOException;

    public String getSignature();

    public String getJqueryCallback();
}

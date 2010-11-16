package jd.plugins.optional.remoteserv.remotecall;

import java.io.IOException;
import java.net.URL;

public interface HttpClient {
    public String post(final URL url, final String data) throws IOException;
}

package jd.http;

import java.util.logging.Logger;

public interface BrowserSettings {

    public HTTPProxy getCurrentProxy();

    public void setCurrentProxy(HTTPProxy proxy);

    public void setVerbose(boolean b);

    public boolean isVerbose();

    public void setDebug(boolean b);

    public boolean isDebug();

    public void setLogger(Logger logger);

    public Logger getLogger();

}

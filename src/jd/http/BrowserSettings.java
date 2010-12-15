package jd.http;

import java.util.logging.Logger;

public interface BrowserSettings {

    public JDProxy getCurrentProxy();

    public void setCurrentProxy(JDProxy proxy);

    public void setVerbose(boolean b);

    public boolean isVerbose();

    public void setDebug(boolean b);

    public boolean isDebug();

    public void setLogger(Logger logger);

    public Logger getLogger();

}

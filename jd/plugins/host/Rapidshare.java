package jd.plugins.host;

import java.net.URLConnection;
import java.util.regex.Pattern;

import jd.plugins.PluginForHost;

public class Rapidshare extends PluginForHost{
    private String  host    = "rapidshare.com";
    private String  version = "1.0.0.0";
    private Pattern patternSupported = Pattern.compile("http://rapidshare\\.com/files[^\\s\"]*");
    
    @Override public String getCoder()            { return "astaldo";        }
    @Override public String getHost()             { return host;             }
    @Override public String getPluginName()       { return host;             }
    @Override public Pattern getSupportedLinks()  { return patternSupported; }
    @Override public String getVersion()          { return version;          }
    @Override public boolean isClipboardEnabled() { return true;             }
    @Override
    public URLConnection getURLConnection() {
        return null;
    }

}

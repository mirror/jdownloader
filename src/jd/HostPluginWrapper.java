package jd;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.plugins.Plugin;
import jd.plugins.PluginForHost;

public class HostPluginWrapper extends PluginWrapper {

    private Plugin loadedPLugin;

    public HostPluginWrapper(String host, boolean loadOnInit, String className, String patternSupported) {
        super(host, loadOnInit, className, patternSupported);
        getPlugin();
    }

    public PluginForHost getPlugin() {
        return (PluginForHost) super.getPlugin();
    }

  

    public String getAGBLink() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isAGBChecked() {
        // TODO Auto-generated method stub
        return true;
    }

    public void setAGBChecked(Boolean value) {
        // TODO Auto-generated method stub

    }

   

}

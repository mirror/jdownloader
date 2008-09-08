package jd;

import java.lang.reflect.Constructor;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

public class PluginWrapper {

    private Pattern pattern;
    private String host;
    private String className;
    protected Logger logger= JDUtilities.getLogger();
    private Plugin loadedPLugin;

  

    public PluginWrapper(String host, boolean loadOnInit, String className, String patternSupported) {
        this.pattern = Pattern.compile(patternSupported, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        this.host = host;
        this.className = className;
        
    }

    public Pattern getPattern() {
        return pattern;
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }
    public Plugin getPlugin() {
        if(loadedPLugin!=null)return loadedPLugin;
        JDClassLoader jdClassLoader = JDUtilities.getJDClassLoader();
        try {

            Class plgClass = jdClassLoader.loadClass("jd.plugins.host." + getClassName());
            if (plgClass == null) {
                logger.info("PLUGIN NOT FOUND!");
                return null;
            }
            Class[] classes = new Class[] {};
            Constructor con = plgClass.getConstructor(classes);

            this.loadedPLugin = (Plugin) con.newInstance(new Object[] {});
           return loadedPLugin;
        } catch (Throwable e) {
            logger.info("Plugin Exception!");
            e.printStackTrace();
        }
        return null;
    }
    public String getPluginName() {
        // TODO Auto-generated method stub
        return getHost();
    }

    public Object getVersion() {
        // TODO Auto-generated method stub
        return "n.A.";
    }

    public Object getCoder() {
        // TODO Auto-generated method stub
        return "JD-Team";
    }
    public boolean canHandle(String data) {
        if (data == null) { return false; }
        Pattern pattern = this.getPattern();
        if (pattern != null) {
            Matcher matcher = pattern.matcher(data);
            if (matcher.find()) { return true; }
        }
        return false;
    }

    public boolean isAcceptOnlyURIs() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isLoaded() {
        // TODO Auto-generated method stub
        return this.loadedPLugin!=null;
    }
}

package jd;

import java.lang.reflect.Constructor;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

public class PluginWrapper {

    public static final int LOAD_ON_INIT = 0<<1;
    private Pattern pattern;
    private String host;
    private String className;
    protected Logger logger= JDUtilities.getLogger();
    private Plugin loadedPLugin;
    private boolean acceptOnlyURIs;
    private String pluginName;

  

    public PluginWrapper(String name,String host, String className, String pattern,int flags) {
        this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        this.host = host;
        this.className = className;
        this.pluginName=name;
        if((flags&PluginWrapper.LOAD_ON_INIT)>0){
            getPlugin();
        }
        
    }

    public Pattern getPattern() {
     return this.isLoaded()?getPlugin().getSupportedLinks():pattern;
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public String getHost() {
     return this.isLoaded()?getPlugin().getHost():host;
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
            logger.finer("laoded PLugin "+"jd.plugins.host." + getClassName());
           return loadedPLugin;
        } catch (Throwable e) {
            logger.info("Plugin Exception!");
            e.printStackTrace();
        }
        return null;
    }
    public String getPluginName() {
        // TODO Auto-generated method stub
        return this.isLoaded()?getPlugin().getPluginName():this.pluginName;
    }

    public Object getVersion() {
        // TODO Auto-generated method stub
        return this.isLoaded()?getPlugin().getVersion():"n.A.";
    }

    public Object getCoder() {
        // TODO Auto-generated method stub
        return this.isLoaded()?getPlugin().getCoder():"JD-Team";
    }
    public boolean canHandle(String data) {
       if(this.isLoaded())getPlugin().canHandle(data);
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
        return acceptOnlyURIs;
    }

    public boolean isLoaded() {
        // TODO Auto-generated method stub
        return this.loadedPLugin!=null;
    }

    public void setAcceptOnlyURIs(boolean acceptOnlyURIs) {
        this.acceptOnlyURIs = acceptOnlyURIs;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }
}

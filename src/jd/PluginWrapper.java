package jd;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.config.SubConfiguration;
import jd.plugins.Plugin;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class PluginWrapper implements Comparable {

    public static final int LOAD_ON_INIT = 1 << 1;
    private Pattern pattern;
    private String host;
    private String className;
    protected Logger logger = JDUtilities.getLogger();
    private Plugin loadedPLugin = null;
    private boolean acceptOnlyURIs;
    private String pluginName;
    private static URLClassLoader CL;

    public PluginWrapper(String name, String host, String className, String pattern, int flags) {

        if (pattern != null) {
            this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        }
        this.host = host;
        this.className = className;
        this.pluginName = name;
        if ((flags & PluginWrapper.LOAD_ON_INIT) > 0) this.getPlugin();

    }

    public Pattern getPattern() {
        return this.isLoaded() ? getPlugin().getSupportedLinks() : pattern;
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public String getHost() {
        return this.isLoaded() ? getPlugin().getHost() : host;
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

    @SuppressWarnings("unchecked")
    public Plugin getPlugin() {
        if (loadedPLugin != null) return loadedPLugin;

        try {

            if (CL == null) CL = new URLClassLoader(new URL[] { JDUtilities.getResourceFile("plugins").toURI().toURL() }, Thread.currentThread().getContextClassLoader());

            System.out.println(JDUtilities.getResourceFile("").toURI().toURL() + " - " + getClassName());
            logger.finer("load plugin: "+getClassName());
            Class plgClass = CL.loadClass(getClassName());

            if (plgClass == null) {
                logger.info("PLUGIN NOT FOUND!");
                return null;
            }
            Class[] classes = new Class[] { String.class };
            Constructor con = plgClass.getConstructor(classes);
            classes = null;
            this.loadedPLugin = (Plugin) con.newInstance(new Object[] { host });
            loadedPLugin.setHost(host);
            loadedPLugin.setSupportedPattern(pattern);
          
            return loadedPLugin;
        } catch (Throwable e) {
            logger.info("Plugin Exception!");
            e.printStackTrace();
        }
        return null;
    }

    public String getPluginName() {
        return this.isLoaded() ? getPlugin().getPluginName() : this.pluginName;
    }

    public Object getVersion() {
        return this.isLoaded() ? getPlugin().getVersion() : JDLocale.L("plugin.system.notloaded", "idle");
    }

    public Object getCoder() {
        return this.isLoaded() ? getPlugin().getCoder() : "JD-Team";
    }

    public boolean canHandle(String data) {
        if (this.isLoaded()){
           return getPlugin().canHandle(data);
        }
        if (data == null) { return false; }
        Pattern pattern = this.getPattern();
        if (pattern != null) {
            Matcher matcher = pattern.matcher(data);
            if (matcher.find()) { return true; }
        }
        return false;
    }

    public boolean isAcceptOnlyURIs() {
        return acceptOnlyURIs;
    }

    public boolean isLoaded() {
        return this.loadedPLugin != null;
    }

    public void setAcceptOnlyURIs(boolean acceptOnlyURIs) {
        this.acceptOnlyURIs = acceptOnlyURIs;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    public SubConfiguration getPluginConfig() {
        String name = getPluginName();
        return JDUtilities.getSubConfig(name);
    }

    public Plugin getNewPluginInstance() {
        Plugin plg = getPlugin();
        try {
            return plg.getClass().getConstructor(new Class[] { String.class }).newInstance(new Object[] { host });
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;

    }

    public int compareTo(Object o) {
        return getHost().compareTo(((PluginWrapper) o).getHost());
    }
}

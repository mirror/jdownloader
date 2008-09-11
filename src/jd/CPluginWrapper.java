package jd;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.plugins.PluginsC;
import jd.utils.JDUtilities;

public class CPluginWrapper extends PluginWrapper {
    private static final ArrayList<CPluginWrapper> C_WRAPPER = new ArrayList<CPluginWrapper>();

    public static ArrayList<CPluginWrapper> getCWrapper() {
        return C_WRAPPER;
    }

    public CPluginWrapper(String name, String host, String className, String patternSupported, int flags) {
        super(name, host, "jd.plugins.a." + className, patternSupported, flags);
        if(loadPlugin()!=null)C_WRAPPER.add(this);
    }


    public CPluginWrapper(String host, String className, String patternSupported) {
        this(host, host, className, patternSupported, 0);
    }

    public PluginsC getPlugin() {
        return (PluginsC)loadedPlugin;
    }
    public PluginsC loadPlugin() {
        JDClassLoader jdClassLoader = JDUtilities.getJDClassLoader();

        logger.finer("Try to initialize " + this.getClassName());
        try {

            Class plgClass = jdClassLoader.loadClass(this.getClassName());
            if (plgClass == null) {
                logger.info("PLUGIN NOT FOUND!");
                return null;
            }
            Class[] classes = new Class[] { PluginWrapper.class };
            Constructor con = plgClass.getConstructor(classes);

            this.loadedPlugin = (PluginsC) con.newInstance(new Object[] { this });
            logger.finer("Successfully loaded " + this.getClassName());
return (PluginsC)loadedPlugin;
        } catch (Throwable e) {
            logger.info("Plugin Exception!");
            e.printStackTrace();
        }

        return null;
    }

    public boolean canHandle(String data) {
        return getPlugin().canHandle(data);
    }

}

package jd;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;

import jd.plugins.PluginOptional;
import jd.utils.JDUtilities;

public class OptionalPluginWrapper extends PluginWrapper {
    private static final ArrayList<OptionalPluginWrapper> OPTIONAL_WRAPPER = new ArrayList<OptionalPluginWrapper>();

    public static ArrayList<OptionalPluginWrapper> getOptionalWrapper() {
        return OPTIONAL_WRAPPER;
    }

    private double version;

    public OptionalPluginWrapper(String string, double d) {
        super(string, string, "jd.plugins.optional." + string, null, 0);
        this.version = d;

        if (loadPlugin() != null) OPTIONAL_WRAPPER.add(this);

    }

    public PluginOptional getPlugin() {
        return (PluginOptional) loadedPlugin;
    }

    @SuppressWarnings("unchecked")
    public PluginOptional loadPlugin() {
        JDClassLoader jdClassLoader = JDUtilities.getJDClassLoader();
        Double version = JDUtilities.getJavaVersion();

        if (version < this.version) {
            logger.finer("Plugin " + this.getClassName() + " requires Java Version " + this.version + " your Version is: " + version);
            return null;
        }
        logger.finer("Try to initialize " + this.getClassName());
        try {

            Class plgClass = jdClassLoader.loadClass(this.getClassName());
            if (plgClass == null) {
                logger.info("PLUGIN NOT FOUND!");
                return null;
            }
            Class[] classes = new Class[] { PluginWrapper.class };
            Constructor con = plgClass.getConstructor(classes);

            try {

                Method f = plgClass.getMethod("getAddonInterfaceVersion", new Class[] {});

                int id = (Integer) f.invoke(null, new Object[] {});

                if (id != PluginOptional.ADDON_INTERFACE_VERSION) {
                    logger.severe("Addon " + this.getClassName() + " is outdated and incompatible. Please update(Packagemanager) :Addon:" + id + " : Interface: " + PluginOptional.ADDON_INTERFACE_VERSION);
                } else {
                    this.loadedPlugin = (PluginOptional) con.newInstance(new Object[] { this });
                    logger.finer("Successfully loaded " + this.getClassName());
                    return (PluginOptional) loadedPlugin;
                }
            } catch (Exception e) {
                logger.severe("Addon " + this.getClassName() + " is outdated and incompatible. Please update(Packagemanager) :" + e.getLocalizedMessage());
            }

        } catch (Throwable e) {
            logger.info("Plugin Exception!");
            e.printStackTrace();
        }
        return null;

    }

    public String getConfigParamKey() {
        return "OPTIONAL_PLUGIN_" + loadedPlugin.getHost();
    }

}

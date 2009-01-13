//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;

import jd.plugins.PluginOptional;
import jd.utils.JDUtilities;

public class OptionalPluginWrapper extends PluginWrapper {
    private static final ArrayList<OptionalPluginWrapper> OPTIONAL_WRAPPER = new ArrayList<OptionalPluginWrapper>();
    public static final int FLAG_ALWAYS_ENABLED = 1<<0;

    public static ArrayList<OptionalPluginWrapper> getOptionalWrapper() {
        return OPTIONAL_WRAPPER;
    }

    private double version;
    private int flag;

    public OptionalPluginWrapper(String string, double d) {
        super(string, "jd.plugins.optional." + string, null, 0);
     
        this.version = d;

        if (loadPlugin() != null){
            //aktiviert neue addons automatisch.
            if (JDUtilities.getConfiguration().getBooleanProperty(getConfigParamKey(), false) != JDUtilities.getConfiguration().getBooleanProperty(getConfigParamKey(), true)) {
                JDUtilities.getConfiguration().setProperty(getConfigParamKey(), true);       
            }
            
            OPTIONAL_WRAPPER.add(this);
        }
    }

    public OptionalPluginWrapper(String string, double d, int flag) {
        this(string,d);
        this.flag=flag;
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    @Override
    public PluginOptional getPlugin() {
        return (PluginOptional) loadedPlugin;
    }

    public PluginOptional loadPlugin() {
        JDClassLoader jdClassLoader = JDUtilities.getJDClassLoader();
        Double version = JDUtilities.getJavaVersion();

        if (version < this.version) {
            logger.finer("Plugin " + this.getClassName() + " requires Java Version " + this.version + " your Version is: " + version);
            return null;
        }
        logger.finer("Try to initialize " + this.getClassName());
        try {

            Class<?> plgClass = jdClassLoader.loadClass(this.getClassName());
            if (plgClass == null) {
                logger.info("PLUGIN NOT FOUND!");
                return null;
            }
            Class<?>[] classes = new Class[] { PluginWrapper.class };
            Constructor<?> con = plgClass.getConstructor(classes);

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

    @Override
    public int compareTo(PluginWrapper plg) {
        return getPlugin().getHost().toLowerCase().compareTo(plg.getPlugin().getHost().toLowerCase());
    }

    public boolean isEnabled() {
        return JDUtilities.getConfiguration().getBooleanProperty(this.getConfigParamKey(), false)||(this.flag&FLAG_ALWAYS_ENABLED)>0;
    }
}

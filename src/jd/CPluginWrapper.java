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
import java.util.ArrayList;

import jd.controlling.JDLogger;
import jd.nutils.JDFlags;
import jd.plugins.PluginsC;
import jd.utils.JDUtilities;

public class CPluginWrapper extends PluginWrapper {
    private static final ArrayList<CPluginWrapper> C_WRAPPER = new ArrayList<CPluginWrapper>();

    public static ArrayList<CPluginWrapper> getCWrapper() {
        return C_WRAPPER;
    }

    public CPluginWrapper(String name, String host, String classNamePrefix, String className, String patternSupported, int flags) {
        super(host, classNamePrefix, className, patternSupported, flags);
        if (loadPlugin() != null) {
            for (CPluginWrapper plugin : C_WRAPPER) {
                if (plugin.getID().equalsIgnoreCase(this.getID()) && plugin.getPattern().equals(this.getPattern())) {
                    if (JDFlags.hasNoFlags(flags, ALLOW_DUPLICATE)) {
                        logger.severe("Cannot add CPlugin!CPluginID " + getID() + " already exists!");
                        return;
                    }
                }
            }
            C_WRAPPER.add(this);
        }
    }

    public CPluginWrapper(String host, String className, String patternSupported) {
        this(host, host, "jd.plugins.a.", className, patternSupported, 0);
    }

    public PluginsC getPlugin() {
        return (PluginsC) loadedPlugin;
    }

    public PluginsC loadPlugin() {
        final JDClassLoader jdClassLoader = JDUtilities.getJDClassLoader();

        logger.finer("Try to initialize " + this.getClassName());
        try {

            final Class<?> plgClass = jdClassLoader.loadClass(this.getClassName());
            if (plgClass == null) {
                logger.info("PLUGIN NOT FOUND!");
                return null;
            }
            final Class<?>[] classes = new Class[] { PluginWrapper.class };
            final Constructor<?> con = plgClass.getConstructor(classes);

            this.loadedPlugin = (PluginsC) con.newInstance(new Object[] { this });
            logger.finer("Successfully loaded " + this.getClassName());
            return (PluginsC) loadedPlugin;
        } catch (Exception e) {
            logger.info("Plugin Exception!");
            JDLogger.exception(e);
        }

        return null;
    }

    // @Override
    public boolean canHandle(String data) {
        return getPlugin().canHandle(data);
    }

    /*
     * (non-Javadoc)
     * 
     * @see jd.PluginWrapper#getVersion()
     */
    @Override
    public String getVersion() {
        return this.isLoaded() ? this.getPlugin().getVersion() : "idle";
    }

}

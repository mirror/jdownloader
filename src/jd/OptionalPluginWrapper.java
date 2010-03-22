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
import jd.nutils.Formatter;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class OptionalPluginWrapper extends PluginWrapper {

    private static final ArrayList<OptionalPluginWrapper> OPTIONAL_WRAPPER = new ArrayList<OptionalPluginWrapper>();

    public static ArrayList<OptionalPluginWrapper> getOptionalWrapper() {
        return OPTIONAL_WRAPPER;
    }

    private double version;
    private String id;
    private String name;
    private String revision;
    private OptionalPlugin annotation;

    public OptionalPluginWrapper(Class<?> c, OptionalPlugin help) {

        super(c.getName(), null, c.getName(), null, 0);
        this.id = help.id();
        revision = Formatter.getRevision(help.rev());
        this.version = help.minJVM();
        this.name = JDL.L(c.getName(), c.getSimpleName());
        this.annotation = help;

        try {

            logger.finer("OPTIONAL loaded " + help);
            for (OptionalPluginWrapper plugin : OPTIONAL_WRAPPER) {
                if (plugin.getID().equalsIgnoreCase(this.getID())) {
                    logger.severe("Cannot add OptionalPlugin!OptionalPluginID " + getID() + " already exists!");
                    return;
                }
            }
            OPTIONAL_WRAPPER.add(this);

            if (this.isEnabled()) {
                this.getPlugin();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public OptionalPlugin getAnnotation() {
        return annotation;
    }

    public double getJavaVersion() {
        return version;
    }

    @Override
    public String getHost() {
        return name;
    }

    /**
     * returns the addon's version (revision)
     */
    @Override
    public String getVersion() {
        // TODO
        return revision;
    }

    @Override
    public PluginOptional getPlugin() {
        if (!OPTIONAL_WRAPPER.contains(this)) return null;
        if (loadedPlugin == null) loadPlugin();
        return (PluginOptional) loadedPlugin;
    }

    private PluginOptional loadPlugin() {
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

                this.loadedPlugin = (PluginOptional) con.newInstance(new Object[] { this });
                logger.finer("Successfully loaded " + this.getClassName());
                return (PluginOptional) loadedPlugin;

            } catch (Exception e) {
                JDLogger.exception(e);
                logger.severe("Addon " + this.getClassName() + " is outdated and incompatible. Please update(Packagemanager) :" + e.getLocalizedMessage());
            }

        } catch (Exception e) {
            logger.info("Plugin Exception!");
            JDLogger.exception(e);
        }
        return null;

    }

    @Override
    public String getID() {
        return id;
    }

    public String getConfigParamKey() {
        return "OPTIONAL_PLUGIN2_" + id;
    }

    @Override
    public int compareTo(PluginWrapper plg) {
        return getHost().toLowerCase().compareTo(plg.getHost().toLowerCase());
    }

    public boolean isEnabled() {
        return JDUtilities.getConfiguration().getBooleanProperty(getConfigParamKey(), annotation.defaultEnabled());
    }
}

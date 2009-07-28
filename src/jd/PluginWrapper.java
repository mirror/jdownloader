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
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.DownloadController;
import jd.controlling.JDLogger;
import jd.controlling.ProgressController;
import jd.nutils.JDFlags;
import jd.plugins.Plugin;
import jd.update.FileUpdate;
import jd.update.WebUpdater;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class PluginWrapper implements Comparable<PluginWrapper> {

    public static final int LOAD_ON_INIT = 1 << 1;
    public static final int ACCEPTONLYSURLSFALSE = 1 << 2;
    public static final int ALWAYS_ENABLED = 1 << 3;
    /**
     * Load only if debug flag is set
     */
    public static final int DEBUG_ONLY = 1 << 4;

    private Pattern pattern;
    private String host;
    private String className;
    protected Logger logger = jd.controlling.JDLogger.getLogger();
    protected Plugin loadedPlugin = null;
    private boolean acceptOnlyURIs = true;
    private boolean alwaysenabled = false;
    private int flags;
    private static URLClassLoader CL;
    private static final HashMap<String, PluginWrapper> WRAPPER = new HashMap<String, PluginWrapper>();

    public PluginWrapper(String host, String classNamePrefix, String className, String pattern, int flags) {
        String classn = (classNamePrefix == null ? "" : classNamePrefix) + className;
        if (pattern != null) {
            this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        }
        this.host = host.toLowerCase();
        this.className = classn;
        this.flags = flags;
        if (JDFlags.hasSomeFlags(flags, LOAD_ON_INIT)) this.getPlugin();
        if (JDFlags.hasSomeFlags(flags, ALWAYS_ENABLED)) this.alwaysenabled = true;
        if (JDFlags.hasSomeFlags(flags, ACCEPTONLYSURLSFALSE)) this.acceptOnlyURIs = false;
        if (JDFlags.hasNoFlags(flags, DEBUG_ONLY) || JDInitFlags.SWITCH_DEBUG) {

            WRAPPER.put(classn, this);
        }
    }

    public int getFlags() {
        return flags;
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
        this.host = host.toLowerCase();
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public synchronized Plugin getPlugin() {
        if (loadedPlugin != null) return loadedPlugin;
        boolean manualupdate = SubConfiguration.getConfig("WEBUPDATE").getBooleanProperty(Configuration.PARAM_WEBUPDATE_DISABLE, false);
        try {

            if (CL == null) CL = new URLClassLoader(new URL[] {
                    JDUtilities.getJDHomeDirectoryFromEnvironment().toURI().toURL(), JDUtilities.getResourceFile("java").toURI().toURL()
            }, Thread.currentThread().getContextClassLoader());
            if (JDUtilities.getRunType() == JDUtilities.RUNTYPE_LOCAL_JARED && WebUpdater.getPluginList() != null) {

                ArrayList<FileUpdate> filelist = new ArrayList<FileUpdate>();
                for (Entry<String, FileUpdate> entry : WebUpdater.PLUGIN_LIST.entrySet()) {
                    if (entry.getKey().startsWith("/" + getClassName().replace(".", "/"))) {
                        filelist.add(entry.getValue());
                    }
                }
                // HashMap<String, Vector<String>> list = WebUpdater.PLUGIN_LIST
                // != null ? WebUpdater.PLUGIN_LIST : new HashMap<String,
                // Vector<String>>();
                ProgressController progress = new ProgressController(JDL.LF("wrapper.webupdate.updateFile", "Update plugin %s", getClassName()), filelist.size() + 1);
                progress.increase(1);
                for (FileUpdate entry : filelist) {
                    String plg = entry.getLocalPath();

                    if (!entry.equals()) {
                        if (!manualupdate) {
                            if (!new WebUpdater().updateUpdatefile(entry)) {
                                logger.severe("Could not update plugin: " + plg);
                            } else {
                                logger.info("Updated plugin: " + plg);
                            }
                        } else {
                            logger.info("New plugin: " + plg + " available, but update-on-the-fly is disabled!");
                        }
                    }
                    progress.increase(1);
                }
                progress.finalize();
            }
            logger.finer("load plugin: " + getClassName());

            Class<?> plgClass;

            try {
                plgClass = CL.loadClass(getClassName());
            } catch (ClassNotFoundException e) {
                plgClass = JDUtilities.getJDClassLoader().loadClass(getClassName());
            }

            if (plgClass == null) {
                logger.info("PLUGIN NOT FOUND!");
                return null;
            }
            Class<?>[] classes = new Class[] {
                PluginWrapper.class
            };
            Constructor<?> con = plgClass.getConstructor(classes);
            classes = null;
            this.loadedPlugin = (Plugin) con.newInstance(new Object[] {
                this
            });

            return loadedPlugin;
        } catch (Exception e) {
            logger.info("Plugin Exception!");
            JDLogger.exception(e);
        }
        return null;
    }

    public String getVersion() {
        return loadedPlugin != null ? loadedPlugin.getVersion() : JDL.L("plugin.system.notloaded", "idle");
    }

    public String getCoder() {
        return loadedPlugin != null ? loadedPlugin.getCoder() : JDL.L("plugin.system.notloaded", "idle");
    }

    public boolean usePlugin() {
        return this.alwaysenabled || getPluginConfig().getBooleanProperty("USE_PLUGIN", true);
    }

    public void setUsePlugin(boolean bool) {
        if (this.alwaysenabled) return;
        getPluginConfig().setProperty("USE_PLUGIN", bool);
        getPluginConfig().save();
        if (JDUtilities.getController() != null) DownloadController.getInstance().fireGlobalUpdate();
    }

    public boolean canHandle(String data) {
        if (this.isLoaded()) { return getPlugin().canHandle(data); }
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
        return this.loadedPlugin != null;
    }

    public void setAcceptOnlyURIs(boolean acceptOnlyURIs) {
        this.acceptOnlyURIs = acceptOnlyURIs;
    }

    public SubConfiguration getPluginConfig() {
        return SubConfiguration.getConfig(getHost());
    }

    public Plugin getNewPluginInstance() {
        try {
            return getPlugin().getClass().getConstructor(new Class[] {
                PluginWrapper.class
            }).newInstance(new Object[] {
                this
            });
        } catch (Exception e) {
            JDLogger.exception(e);
        }
        return null;
    }

    public boolean hasConfig() {
        return isLoaded() && !getPlugin().getConfig().getEntries().isEmpty();
    }

    public int compareTo(PluginWrapper plg) {
        return getHost().toLowerCase().compareTo(plg.getHost().toLowerCase());
    }

    public String getConfigName() {
        return getHost();
    }

    public static PluginWrapper getWrapper(String clazz) {
        return WRAPPER.get(clazz);
    }

    public static Plugin getNewInstance(String className) {
        if (!WRAPPER.containsKey(className)) {
            try {
                throw new Exception("plugin " + className + " could not be found");
            } catch (Exception e) {
                JDLogger.exception(e);
                return null;
            }
        }
        return WRAPPER.get(className).getNewPluginInstance();
    }

}

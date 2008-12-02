//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.nutils.JDHash;
import jd.plugins.Plugin;
import jd.update.WebUpdater;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class PluginWrapper implements Comparable<PluginWrapper> {

    public static final int LOAD_ON_INIT = 1 << 1;
    public static final int ACCEPTONLYSURLSFALSE = 1 << 2;
    private Pattern pattern;
    private String host;
    private String className;
    protected Logger logger = JDUtilities.getLogger();
    protected Plugin loadedPlugin = null;
    private boolean acceptOnlyURIs = true;
    private static URLClassLoader CL;
    private static final HashMap<String, PluginWrapper> WRAPPER = new HashMap<String, PluginWrapper>();

    public PluginWrapper(String host, String className, String pattern, int flags) {

        if (pattern != null) {
            this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        }
        this.host = host.toLowerCase();
        this.className = className;
        if ((flags & PluginWrapper.LOAD_ON_INIT) != 0) this.getPlugin();
        if ((flags & ACCEPTONLYSURLSFALSE) != 0) this.acceptOnlyURIs = false;
        WRAPPER.put(className, this);

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

    public Plugin getPlugin() {
        if (loadedPlugin != null) return loadedPlugin;

        try {

            if (CL == null) CL = new URLClassLoader(new URL[] { JDUtilities.getResourceFile("").toURI().toURL(), JDUtilities.getResourceFile("java").toURI().toURL() }, Thread.currentThread().getContextClassLoader());
            if (JDUtilities.getRunType() == JDUtilities.RUNTYPE_LOCAL_JARED && WebUpdater.PLUGIN_LIST != null) {

                ArrayList<Vector<String>> filelist = new ArrayList<Vector<String>>();
                for (Iterator<Entry<String, Vector<String>>> it = WebUpdater.PLUGIN_LIST.entrySet().iterator(); it.hasNext();) {
                    Entry<String, Vector<String>> entry = it.next();
                    if (entry.getKey().startsWith(getClassName().replace(".", "/"))) {
                        filelist.add(entry.getValue());
                    }

                }
                // HashMap<String, Vector<String>> list = WebUpdater.PLUGIN_LIST
                // != null ? WebUpdater.PLUGIN_LIST : new HashMap<String,
                // Vector<String>>();
                ProgressController progress = new ProgressController(JDLocale.LF("wrapper.webupdate.updateFile", "Update plugin %s", getClassName()), filelist.size() + 1);
                progress.increase(1);
                for (Vector<String> entry : filelist) {
                    String plg = entry.get(0).split("\\?")[0];
                    File path = JDUtilities.getResourceFile(plg);
                    String hash = JDHash.getMD5(path);
                    if (hash == null || !hash.equalsIgnoreCase(entry.get(1))) {
                        new WebUpdater().updateFile(entry);
                        logger.info("UPdated plugin: " + plg);

                    }
                    progress.increase(1);
                }
                progress.finalize();
            }
            logger.finer("load plugin: " + getClassName());
            Class<?> plgClass = CL.loadClass(getClassName());

            if (plgClass == null) {
                logger.info("PLUGIN NOT FOUND!");
                return null;
            }
            Class<?>[] classes = new Class[] { PluginWrapper.class };
            Constructor<?> con = plgClass.getConstructor(classes);
            classes = null;
            this.loadedPlugin = (Plugin) con.newInstance(new Object[] { this });

            return loadedPlugin;
        } catch (Throwable e) {
            logger.info("Plugin Exception!");
            e.printStackTrace();
        }
        return null;
    }

    public Object getVersion() {
        return this.isLoaded() ? getPlugin().getVersion() : JDLocale.L("plugin.system.notloaded", "idle");
    }

    public Object getCoder() {
        return this.isLoaded() ? getPlugin().getCoder() : JDLocale.L("plugin.system.notloaded", "idle");
    }

    public boolean usePlugin() {
        return getPluginConfig().getBooleanProperty("USE_PLUGIN", true);
    }

    public void setUsePlugin(boolean bool) {
        getPluginConfig().setProperty("USE_PLUGIN", bool);
        getPluginConfig().save();
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
        return JDUtilities.getSubConfig(getHost());
    }

    public Plugin getNewPluginInstance() {
        try {
            return getPlugin().getClass().getConstructor(new Class[] { PluginWrapper.class }).newInstance(new Object[] { this });
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;

    }

    public boolean hasConfig() {
        return isLoaded() && getPlugin().getConfig().getEntries().size() != 0;
    }

    public int compareTo(PluginWrapper plg) {
        return getHost().toLowerCase().compareTo(plg.getHost().toLowerCase());
    }

    public String getConfigName() {
        return getHost();
    }

    public static Plugin getNewInstance(String className) {
        if (!WRAPPER.containsKey(className)) {
            try {
                throw new Exception("plugin " + className + " could not be found");
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return WRAPPER.get(className).getNewPluginInstance();
    }
}

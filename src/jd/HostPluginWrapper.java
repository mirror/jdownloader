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

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import javax.swing.ImageIcon;

import jd.config.container.JDLabelContainer;
import jd.controlling.JDLogger;
import jd.nutils.JDFlags;
import jd.plugins.PluginForHost;

public class HostPluginWrapper extends PluginWrapper implements JDLabelContainer {
    private static final ArrayList<HostPluginWrapper> HOST_WRAPPER = new ArrayList<HostPluginWrapper>();

    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    public static final ReadLock readLock = lock.readLock();
    public static final WriteLock writeLock = lock.writeLock();

    static {
        try {
            writeLock.lock();
            JDInit.loadPluginForHost();
        } catch (Throwable e) {
            JDLogger.exception(e);
        } finally {
            writeLock.unlock();
        }
    }

    public static ArrayList<HostPluginWrapper> getHostWrapper() {
        return HOST_WRAPPER;
    }

    public static boolean hasPlugin(final String data) {
        for (HostPluginWrapper w : getHostWrapper()) {
            if (w.canHandle(data)) return true;
        }
        return false;
    }

    private static final String AGB_CHECKED = "AGB_CHECKED";

    public HostPluginWrapper(final String host, final String classNamePrefix, final String className, final String patternSupported, final int flags, final String revision) {
        super(host, classNamePrefix, className, patternSupported, flags, revision);
        try {
            writeLock.lock();
            for (HostPluginWrapper plugin : HOST_WRAPPER) {
                if (plugin.getID().equalsIgnoreCase(this.getID()) && plugin.getPattern().equals(this.getPattern())) {
                    if (JDFlags.hasNoFlags(flags, ALLOW_DUPLICATE)) {
                        logger.severe("Cannot add HostPlugin!HostPluginID " + getID() + " already exists!");
                        return;
                    }
                }
            }
            HOST_WRAPPER.add(this);
        } finally {
            writeLock.unlock();
        }
    }

    public HostPluginWrapper(final String host, final String simpleName, final String pattern, final int flags, final String revision) {
        this(host, "jd.plugins.hoster.", simpleName, pattern, flags, revision);
    }

    @Override
    public PluginForHost getPlugin() {
        return (PluginForHost) super.getPlugin();
    }

    public boolean isAGBChecked() {
        return super.getPluginConfig().getBooleanProperty(AGB_CHECKED, false);
    }

    public void setAGBChecked(final Boolean value) {
        super.getPluginConfig().setProperty(AGB_CHECKED, value);
        super.getPluginConfig().save();
    }

    public boolean isPremiumEnabled() {
        return this.isLoaded() && this.getPlugin().isPremiumEnabled();
    }

    @Override
    public int compareTo(final PluginWrapper pw) {
        if (!(pw instanceof HostPluginWrapper)) return super.compareTo(pw);

        final HostPluginWrapper plg = (HostPluginWrapper) pw;
        if (this.isLoaded() && plg.isLoaded()) {
            if (this.isPremiumEnabled() && plg.isPremiumEnabled()) return this.getHost().compareToIgnoreCase(plg.getHost());
            if (this.isPremiumEnabled() && !plg.isPremiumEnabled()) return -1;
            if (!this.isPremiumEnabled() && plg.isPremiumEnabled()) return 1;
        }
        if (this.isLoaded() && !plg.isLoaded()) {
            if (this.isPremiumEnabled()) return -1;
        }
        if (!this.isLoaded() && !!plg.isLoaded()) {
            if (plg.isPremiumEnabled()) return 1;
        }
        return this.getHost().compareToIgnoreCase(plg.getHost());
    }

    @Override
    public String toString() {
        return getHost();
    }

    /**
     * FIXME: Getting the icon loads the plugin... Maybe move the HosterIcon
     * functions to this wrapper?
     */
    public ImageIcon getIcon() {
        return getPlugin().getHosterIcon();
    }

    public String getLabel() {
        return toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof HostPluginWrapper)) return false;
        return this.getID().equalsIgnoreCase(((HostPluginWrapper) obj).getID());
    }

}

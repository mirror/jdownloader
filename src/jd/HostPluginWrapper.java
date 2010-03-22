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

import javax.swing.ImageIcon;

import jd.config.container.JDLabelContainer;
import jd.controlling.JDLogger;
import jd.nutils.Formatter;
import jd.nutils.JDFlags;
import jd.plugins.PluginForHost;

public class HostPluginWrapper extends PluginWrapper implements JDLabelContainer {
    private static final ArrayList<HostPluginWrapper> HOST_WRAPPER = new ArrayList<HostPluginWrapper>();
    public static final Object LOCK = new Object();

    static {
        try {
            JDInit.loadPluginForHost();
        } catch (Throwable e) {
            JDLogger.exception(e);
        }
    }

    public static ArrayList<HostPluginWrapper> getHostWrapper() {
        return HOST_WRAPPER;
    }

    private static final String AGB_CHECKED = "AGB_CHECKED";
    // private String revision = "idle";
    private final String revision;

    public HostPluginWrapper(final String host, final String classNamePrefix, final String className, final String patternSupported, final int flags, final String revision) {
        super(host, classNamePrefix, className, patternSupported, flags);
        this.revision = Formatter.getRevision(revision);
        synchronized (LOCK) {
            for (HostPluginWrapper plugin : HOST_WRAPPER) {
                if (plugin.getID().equalsIgnoreCase(this.getID()) && plugin.getPattern().equals(this.getPattern())) {
                    if (JDFlags.hasNoFlags(flags, ALLOW_DUPLICATE)) {
                        logger.severe("Cannot add HostPlugin!HostPluginID " + getID() + " already exists!");
                        return;
                    }
                }
            }
            HOST_WRAPPER.add(this);
        }
    }

    public HostPluginWrapper(final String host, final String simpleName, final String pattern, final int flags, final String revision) {
        this(host, "jd.plugins.hoster.", simpleName, pattern, flags, revision);
    }

    @Override
    public String getVersion() {
        return revision;
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

    public static boolean hasPlugin(final String data) {
        for (HostPluginWrapper w : getHostWrapper()) {
            if (w.canHandle(data)) return true;
        }
        return false;
    }

    public ImageIcon getIcon() {
        return getPlugin().getHosterIcon();
    }

    public String getLabel() {
        return toString();
    }

}

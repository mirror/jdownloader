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

package org.jdownloader;

import java.util.ArrayList;

import org.jdownloader.controlling.AccountController;
import org.jdownloader.plugins.PluginForHost;


public class HostPluginWrapper extends PluginWrapper {
    private static final ArrayList<HostPluginWrapper> HOST_WRAPPER = new ArrayList<HostPluginWrapper>();

    public static ArrayList<HostPluginWrapper> getHostWrapper() {
        return HOST_WRAPPER;
    }

    private static final String AGB_CHECKED = "AGB_CHECKED";

    public HostPluginWrapper(String host, String classNamePrefix, String className, String patternSupported, int flags) {
        super(host, classNamePrefix, className, patternSupported, flags);
        HOST_WRAPPER.add(this);
    }

    public HostPluginWrapper(String host, String className, String patternSupported) {
        this(host, "jd.plugins.host.", className, patternSupported, 0);
    }

    public HostPluginWrapper(String host, String className, String patternSupported, int flags) {
        this(host, "jd.plugins.host.", className, patternSupported, flags);
    }

    @Override
    public PluginForHost getPlugin() {
        return (PluginForHost) super.getPlugin();
    }

    public boolean isAGBChecked() {
        return super.getPluginConfig().getBooleanProperty(AGB_CHECKED, false);
    }

    public void setAGBChecked(Boolean value) {
        super.getPluginConfig().setProperty(AGB_CHECKED, value);
        super.getPluginConfig().save();
    }

    public boolean isPremiumEnabled() {
        return this.isLoaded() && this.getPlugin().isPremiumEnabled();
    }

    @Override
    public int compareTo(PluginWrapper pw) {
        if (!(pw instanceof HostPluginWrapper)) return super.compareTo(pw);

        HostPluginWrapper plg = (HostPluginWrapper) pw;
        if (this.isLoaded() && plg.isLoaded()) {
            if (this.isPremiumEnabled() && plg.isPremiumEnabled()) {
                boolean a = AccountController.getInstance().hasAccounts(this.getHost());
                boolean b = AccountController.getInstance().hasAccounts(plg.getHost());
                if ((a && b) || (!a && !b)) {
                    return this.getHost().compareToIgnoreCase(plg.getHost());
                } else if (a && !b) {
                    return -1;
                } else {
                    return 1;
                }
            } else if (!this.isPremiumEnabled() && !plg.isPremiumEnabled()) {
                return this.getHost().compareToIgnoreCase(plg.getHost());
            } else if (this.isPremiumEnabled() && !plg.isPremiumEnabled()) {
                return -1;
            } else {
                return 1;
            }
        } else if (!this.isLoaded() && !plg.isLoaded()) {
            return this.getHost().compareToIgnoreCase(plg.getHost());
        } else if (this.isLoaded() && !plg.isLoaded()) {
            return -1;
        } else {
            return 1;
        }
    }

 

}

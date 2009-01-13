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

import jd.plugins.PluginForHost;

public class HostPluginWrapper extends PluginWrapper {
    private static final ArrayList<HostPluginWrapper> HOST_WRAPPER = new ArrayList<HostPluginWrapper>();

    public static ArrayList<HostPluginWrapper> getHostWrapper() {
        return HOST_WRAPPER;
    }

    private static final String AGB_CHECKED = "AGB_CHECKED";

    public HostPluginWrapper(String host, String className, String patternSupported, int flags) {
        super(host, "jd.plugins.host." + className, patternSupported, flags);
        HOST_WRAPPER.add(this);
    }

    public HostPluginWrapper(String host, String className, String patternSupported) {
        this(host, className, patternSupported, 0);
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

}

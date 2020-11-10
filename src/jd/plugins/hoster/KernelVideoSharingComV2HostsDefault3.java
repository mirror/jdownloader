//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class KernelVideoSharingComV2HostsDefault3 extends KernelVideoSharingComV2 {
    public KernelVideoSharingComV2HostsDefault3(final PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "shameless.com" });
        ret.add(new String[] { "xbabe.com" });
        ret.add(new String[] { "bravotube.net" });
        ret.add(new String[] { "hellporno.com" });
        ret.add(new String[] { "alphaporno.com" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return KernelVideoSharingComV2.buildAnnotationUrlsDefaultVideosPatternWithoutFileID(getPluginDomains());
    }

    @Override
    protected boolean hasFUIDInsideURL(final String url) {
        return false;
    }

    @Override
    protected String generateContentURL(final String fuid, String title) {
        if (StringUtils.isEmpty(title)) {
            return null;
        }
        title = title.trim().toLowerCase();
        /* We cannot create a valid url-title if it contains other chars than we expect. */
        if (!title.matches("[a-z0-9\\- ]+")) {
            return null;
        }
        return "https://www." + this.getHost() + "/videos/" + title.replace(" ", "-") + "/";
    }
}
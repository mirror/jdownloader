//jDownloader - Downloadmanager
//Copyright (C) 2020  JD-Team support@jdownloader.org
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.HostPlugin;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class MetacafeCom extends KernelVideoSharingComV2 {
    public MetacafeCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    /** Add all KVS hosts to this list that fit the main template without the need of ANY changes to this class. */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "metacafe.com" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    private static final String TYPE_CUSTOM = "https?://(?:www\\.)?[^/]+/watch/(\\d+)/([a-z0-9\\-]+)/?";

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(watch/\\d+/[a-z0-9\\-]+/?|embed/\\d+/?)");
        }
        return ret.toArray(new String[0]);
    }

    protected String getFUIDFromURL(final String url) {
        if (url != null && url.matches(TYPE_CUSTOM)) {
            return new Regex(url, TYPE_CUSTOM).getMatch(0);
        } else {
            return super.getFUIDFromURL(url);
        }
    }

    @Override
    protected String getURLTitle(final String url) {
        if (url.matches(TYPE_CUSTOM)) {
            return new Regex(url, TYPE_CUSTOM).getMatch(1);
        } else {
            return null;
        }
    }

    @Override
    protected String getDllink(final Browser br) throws PluginException, IOException {
        String dllink = PluginJSonUtils.getJson(br, "video_url");
        if (dllink != null) {
            /* 2020-11-23: Cheap way of doing an unnecessary stringformat. */
            if (dllink.contains("%07d")) {
                dllink = dllink.replace("%07d", "0000000");
            }
            final String urlPart = new Regex(dllink, "/get_file/\\d*/[a-f0-9]{32}/(.+\\.mp4)").getMatch(0);
            if (urlPart != null) {
                dllink = "https://cdn.mcstatic.com/videos/" + urlPart;
            }
            return dllink;
        }
        return null;
    }
}
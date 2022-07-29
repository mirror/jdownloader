//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class VxxxCom extends KernelVideoSharingComV2 {
    public VxxxCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://vxxx.com/information/terms";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "vxxx.com" });
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
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:video|embed)-\\d+/?");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    protected String getFUIDFromURL(final String url) {
        if (url == null) {
            return null;
        } else {
            return new Regex(url, "(\\d+)/?$").getMatch(0);
        }
    }

    @Override
    protected boolean useAPI() {
        return true;
    }

    @Override
    protected String getAPIParam1(final String videoID) {
        return "0";
    }

    @Override
    protected String getAPICroppedVideoID(final String videoID) {
        if (videoID.length() > 3) {
            return videoID.substring(0, videoID.length() - 3) + "000";
        } else {
            /* Most likely invalid videoID. */
            return null;
        }
    }

    @Override
    String generateContentURL(final String host, final String fuid, final String urlSlug) {
        if (host == null || urlSlug == null) {
            return null;
        }
        return this.getProtocol() + host + "/video-" + fuid + "/";
    }
}
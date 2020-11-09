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

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class H2pornCom extends KernelVideoSharingComV2 {
    public H2pornCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    /** Add all KVS hosts to this list that fit the main template without the need of ANY changes to this class. */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "h2porn.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:videos/|player/iframe_embed\\.php\\?dir=)[a-z0-9\\-]+/?|https?://embed\\." + buildHostsPatternPart(domains) + "/[a-z0-9\\-]+");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    protected String getURLTitle(final String url) {
        if (url == null) {
            return null;
        }
        return new Regex(url, "([a-z0-9\\-]+)/?$").getMatch(0);
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setPluginPatternMatcher("http://h2porn.com/videos/" + getURLTitle(link.getPluginPatternMatcher()) + "/");
    }

    @Override
    protected boolean hasFUIDInsideURL(final String url) {
        return false;
    }
}
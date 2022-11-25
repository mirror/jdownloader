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
import jd.http.Browser;
import jd.parser.Regex;
import jd.parser.html.HTMLSearch;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class MambahuyambaCom extends KernelVideoSharingComV2 {
    public MambahuyambaCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    /** Add all KVS hosts to this list that fit the main template without the need of ANY changes to this class. */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "deep.mambahuyamba.com", "most.mambahuyamba.com", "huyamba.info", "pornogovno.me" });
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
            ret.add("https?://" + buildHostsPatternPart(domains) + "/(video/\\d+/|embed/\\d+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String rewriteHost(final String host) {
        /* 2022-11-25: Main domain has changed from most.mambahuyamba.com to deep.mambahuyamba.com. */
        return this.rewriteHost(getPluginDomains(), host);
    }

    @Override
    protected ArrayList<String> getDeadDomains() {
        final ArrayList<String> deadDomains = new ArrayList<String>();
        /* 2022-11-25 */
        deadDomains.add("most.mambahuyamba.com");
        return deadDomains;
    }

    @Override
    protected String regexNormalTitleWebsite(final Browser br) {
        String title = br.getRegex("class=\"videotitle\"><h1>([^<>\"]+)<").getMatch(0);
        if (title == null) {
            title = br.getRegex("<h1 itemprop=\"name\">([^<>\"]+)</h1>").getMatch(0);
        }
        if (title != null) {
            return title;
        } else {
            /* Fallback to upper handling */
            return super.regexNormalTitleWebsite(br);
        }
    }

    @Override
    protected String regexEmbedTitleWebsite(final Browser br) {
        String title = HTMLSearch.searchMetaTag(br, "og:title");
        if (title == null) {
            title = br.getRegex("<title>([^<]+)</title>").getMatch(0);
        }
        if (title != null) {
            return title;
        } else {
            /* Fallback to upper handling */
            return super.regexEmbedTitleWebsite(br);
        }
    }

    @Override
    protected boolean hasFUIDInsideURLAtTheEnd(final String url) {
        return true;
    }

    @Override
    protected String getFUIDFromURL(final String url) {
        final String fuid = new Regex(url, "https?://[^/]+/video/(\\d+)/?$").getMatch(0);
        if (fuid != null) {
            return fuid;
        } else {
            /* Fallback to upper handling */
            return super.getFUIDFromURL(url);
        }
    }

    @Override
    String generateContentURL(final String host, final String fuid, final String urlSlug) {
        return "https://" + this.getHost() + "/video/" + fuid + "/";
    }
}
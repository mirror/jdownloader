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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class TheyarehugeCom extends KernelVideoSharingComV2 {
    public TheyarehugeCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    /** Add all KVS hosts to this list that fit the main template without the need of ANY changes to this class. */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "theyarehuge.com" });
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
            /*
             * 2020-10-27: They got embed URLs but they do not work and it is impossible to get the original URL if you only have the embed
             * URL!
             */
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(v/([^/]+)/?|embed/\\d+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    protected String getFileTitle(final DownloadLink link) {
        String filename = br.getRegex("class=\"cs_headline\">([^<>\"]+)<").getMatch(0);
        String title_url = null;
        if (link.getPluginPatternMatcher().matches(type_embedded)) {
            /* Filenames for embed URLs */
            final String originalURL = br.getRegex("(https?://[^/]+/v/[^/]+)").getMatch(0);
            if (originalURL != null) {
                title_url = this.getURLTitleCorrected(originalURL);
            }
        } else {
            title_url = this.getURLTitleCorrected(br.getURL());
            if (title_url == null) {
                title_url = this.getURLTitleCorrected(link.getPluginPatternMatcher());
            }
        }
        /* Now decide which filename we want to use */
        if (StringUtils.isEmpty(filename)) {
            /* Fallback */
            filename = title_url;
        }
        return filename;
    }

    @Override
    protected String getURLTitle(final String url) {
        if (url == null) {
            return null;
        }
        return new Regex(url, this.getSupportedLinks()).getMatch(1);
    }

    @Override
    protected String getURLTitleCorrected(final String url) {
        String urltitle = getURLTitle(url);
        if (!StringUtils.isEmpty(urltitle)) {
            final String removeme = new Regex(urltitle, "(-?\\d{10,}-?)").getMatch(0);
            if (removeme != null) {
                urltitle = urltitle.replace(removeme, "");
            }
            /* Make the url-filenames look better by using spaces instead of '-'. */
            urltitle = urltitle.replace("-", " ");
            /* Remove eventually existing spaces at the end */
            urltitle = urltitle.trim();
        }
        return urltitle;
    }

    @Override
    protected String getDllink(final Browser br) throws PluginException, IOException {
        /* 2020-10-27: Official download available: Higher quality than streaming download! */
        String officialDownloadurl = br.getRegex("\"(https?://[^\"]+\\.mp4[^\"]*\\?download=true[^\"]*720p[^\"]*)\"").getMatch(0);
        if (officialDownloadurl != null) {
            if (Encoding.isHtmlEntityCoded(officialDownloadurl)) {
                officialDownloadurl = Encoding.htmlDecode(officialDownloadurl);
            }
            return officialDownloadurl;
        } else {
            /* Fallback and also required for embed URLs */
            String streamURL = super.getDllink(br);
            if (!streamURL.contains(".mp4")) {
                /* Fallback/Workaround */
                streamURL = br.getRegex("video_url\\s*:\\s*'(https?://[^<>\"\\']+)'").getMatch(0);
            }
            return streamURL;
        }
    }

    @Override
    protected boolean isOffline() {
        boolean offline = super.isOffline();
        final String urltitle = getURLTitle(this.getDownloadLink().getPluginPatternMatcher());
        /* 2020-10-30: Website redirects to random other video if originally requested content is offline. */
        if (!offline && urltitle != null) {
            offline = !br.getURL().contains(urltitle);
        }
        return offline;
    }
}
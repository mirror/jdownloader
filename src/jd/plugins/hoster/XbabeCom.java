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
import java.util.HashMap;
import java.util.List;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class XbabeCom extends KernelVideoSharingComV2 {
    public XbabeCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "xbabe.com" });
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
    protected String generateContentURL(final String host, final String fuid, final String urlTitle) {
        if (StringUtils.isEmpty(urlTitle)) {
            return null;
        }
        return "https://www." + this.getHost() + "/videos/" + urlTitle + "/";
    }

    @Override
    protected String getDllink(final DownloadLink link, final Browser br) throws PluginException, IOException {
        /* 2021-09-01: Workaround as upper handling picks up preview-clips instead of the full videos. */
        final HashMap<Integer, String> qualityMap = new HashMap<Integer, String>();
        final String[] htmls = br.getRegex("(<source[^>]*src=.*?[^>]*type=\"video/mp4\"[^>]*>)").getColumn(0);
        for (final String html : htmls) {
            final String url = new Regex(html, "src=\"(http[^\"]+)").getMatch(0);
            if (url == null) {
                continue;
            }
            int width = -1;
            String widthStr = new Regex(html, "title=\"(\\d+)p\"").getMatch(0);
            if (widthStr == null) {
                widthStr = new Regex(url, "(\\d+)p\\.mp4").getMatch(0);
            }
            if (widthStr == null && html.contains("data-fluid-hd")) {
                /* Fallback */
                width = 720;
            } else if (widthStr != null) {
                width = Integer.parseInt(widthStr);
            }
            qualityMap.put(width, url);
        }
        String dllink = this.handleQualitySelection(link, qualityMap);
        if (dllink != null) {
            return dllink;
        } else {
            /* Fallback to upper handling */
            return super.getDllink(link, br);
        }
    }
}
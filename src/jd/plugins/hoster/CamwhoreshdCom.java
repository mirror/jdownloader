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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class CamwhoreshdCom extends KernelVideoSharingComV2 {
    public CamwhoreshdCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        /*
         * 2020-10-27: cwtvembeds.com is part of camwhoreshd.com - it is very unlikely going to happen that a user inserts a single
         * camwhoreshd.com URL but just in case, we will support that too. Usually camwhoreshd.com will embed camwhoreshd.com (= their own)
         * videos (special case as cwtvembeds.com is their own website too.)!
         */
        ret.add(new String[] { "camwhoreshd.com", "cwtvembeds.com" });
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
        return KernelVideoSharingComV2.buildAnnotationUrlsDefaultVideosPattern(getPluginDomains());
    }

    @Override
    protected String getDllink(final DownloadLink link, final Browser br) throws PluginException, IOException {
        /* Special handling */
        final String embed = br.getRegex("(https?://(?:www\\.)?(cwtvembeds\\.com|camwhores\\.lol)/embed/\\d+)").getMatch(0);
        if (embed != null && !StringUtils.equals(br._getURL().getPath(), new URL(embed).getPath())) {
            br.setFollowRedirects(true);
            br.getPage(embed);
            return super.getDllink(link, br);
        } else {
            return super.getDllink(link, br);
        }
    }

    @Override
    protected String generateContentURL(final String host, final String fuid, final String urlTitle) {
        return generateContentURLDefaultVideosPattern(host, fuid, urlTitle);
    }
}
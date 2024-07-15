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

import org.jdownloader.plugins.components.config.KVSConfig;
import org.jdownloader.plugins.components.config.KVSConfigXfreehdCom;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class XfreehdCom extends KernelVideoSharingComV2 {
    public XfreehdCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.xfreehd.com/signup");
    }

    /** Add all KVS hosts to this list that fit the main template without the need of ANY changes to this class. */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "xfreehd.com" });
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
    protected String generateContentURL(final String host, final String fuid, final String urlTitle) {
        return generateContentURLDefaultVideosPattern(host, fuid, urlTitle);
    }

    @Override
    protected boolean isLoggedIN(final Browser br) {
        if (br.containsHTML("/logout")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected String getDllink(final DownloadLink link, final Browser br) throws PluginException, IOException {
        final int userSelectedQuality = this.getPreferredStreamQuality();
        final String urlsd = br.getRegex("src=\"(https?://[^\"]+)\" title=\"SD\" [^>]*/>").getMatch(0);
        final String urlhd = br.getRegex("src=\"(https?://[^\"]+)\" title=\"HD\" [^>]*/>").getMatch(0);
        if (userSelectedQuality != -1 && userSelectedQuality < 720 && urlsd != null) {
            /* Try to get SD quality */
            return urlsd;
        } else if (urlhd != null) {
            return urlhd;
        } else {
            /* Fallback */
            return super.getDllink(link, br);
        }
    }

    @Override
    protected boolean isOfflineWebsite(final Browser br) {
        if (br.containsHTML(">\\s*This Video Is No Longer Available")) {
            return true;
        } else {
            return super.isOfflineWebsite(br);
        }
    }

    @Override
    public Class<? extends KVSConfig> getConfigInterface() {
        return KVSConfigXfreehdCom.class;
    }
}
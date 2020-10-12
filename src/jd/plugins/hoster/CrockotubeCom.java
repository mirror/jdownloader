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
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class CrockotubeCom extends KernelVideoSharingComV2 {
    public CrockotubeCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "crockotube.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/watch/([A-Za-z0-9\\-]+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        String fid = getFID(link);
        if (fid == null) {
            fid = this.getURLFilename(link.getPluginPatternMatcher());
        }
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), "/watch/[A-Za-z0-9\\-]+-([A-Za-z0-9]+)$").getMatch(0);
    }

    @Override
    protected String getURLFilename(final String url_source) {
        if (url_source == null) {
            return null;
        }
        String filename_url = new Regex(url_source, "/watch/(.+)-[A-Za-z0-9]+$").getMatch(0);
        if (filename_url == null) {
            filename_url = new Regex(url_source, "/watch/(.+)").getMatch(0);
        }
        if (!StringUtils.isEmpty(filename_url)) {
            /* Make the url-filenames look better by using spaces instead of '-'. */
            filename_url = filename_url.replace("-", " ");
        }
        return filename_url;
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    @Override
    public int getMaxChunks(final Account account) {
        return -2;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }
}
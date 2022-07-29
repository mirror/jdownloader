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

import java.util.ArrayList;
import java.util.List;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class WebcamsbabeCom extends KernelVideoSharingComV2 {
    public WebcamsbabeCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "sexcams-24.com", "webcamsbabe.com" });
        /* Russian version of webcamsbabe.com. Same URL-pattern but different content/file-servers/contentIDs! */
        ret.add(new String[] { "private-records.com", "webcamvau.com", "privat-zapisi.biz" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(videos/\\d+-[a-z0-9\\-]+\\.html|embed/\\d+/?)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String rewriteHost(String host) {
        /* 2022-07-29: privat-zapisi.biz is now private-records.com */
        return this.rewriteHost(getPluginDomains(), host);
    }

    protected String getURLTitle(final String url) {
        if (url == null) {
            return null;
        } else {
            return new Regex(url, "/videos/\\d+-(.+)\\.html$").getMatch(0);
        }
    }

    @Override
    String generateContentURL(final String host, final String fuid, final String urlSlug) {
        return this.getProtocol() + host + "/videos/" + fuid + "-" + urlSlug + ".html";
    }
}
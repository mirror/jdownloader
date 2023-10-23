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

import org.appwork.utils.Regex;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class AdultdeepfakesCom extends KernelVideoSharingComV2 {
    public AdultdeepfakesCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    /** Add all KVS hosts to this list that fit the main template without the need of ANY changes to this class. */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "adultdeepfakes.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(videos/(\\d+)-([a-z0-9\\-]+)|embed/(\\d+))");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    protected String generateContentURL(final String host, final String fuid, final String urlSlug) {
        return "https://" + host + "/videos/" + fuid + "/" + urlSlug + "/";
    }

    @Override
    protected String getFUIDFromURL(final String url) {
        if (url == null) {
            return null;
        } else {
            final String fuidFromUrlTypeNormal = new Regex(url, this.getSupportedLinks()).getMatch(1);
            if (fuidFromUrlTypeNormal != null) {
                return fuidFromUrlTypeNormal;
            } else {
                return super.getFUIDFromURL(url);
            }
        }
    }

    @Override
    protected String getTitleURL(final Browser br, final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(2);
    }
}
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
package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;

import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class FrprnComCrawler extends PornEmbedParser {
    public FrprnComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "frprn.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:embed|videos)/(\\d+)/?");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    protected String getFileTitle(final CryptedLink param, final Browser br) {
        String title = regexTitle(br);
        if (title == null) {
            title = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        }
        return title;
    }

    public static String regexTitle(final Browser br) {
        return br.getRegex("<title>(.*?)</title>").getMatch(0);
    }

    @Override
    protected boolean isOffline(final Browser br) {
        if (br.getHttpConnection().getResponseCode() == 404) {
            return true;
        } else {
            return false;
        }
    }

    public static final String TYPE_EMBED = "(?:https?://[^/]+)?/embed/(\\d+)/?";

    @Override
    protected boolean isSelfhosted(final Browser br) {
        final String embedURL = br.getRegex(FrprnComCrawler.TYPE_EMBED).getMatch(-1);
        if (embedURL != null && embedURL.contains(br.getHost())) {
            return true;
        } else {
            return false;
        }
    }
}
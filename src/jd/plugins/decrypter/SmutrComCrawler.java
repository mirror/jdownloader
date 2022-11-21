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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class SmutrComCrawler extends PornEmbedParser {
    public SmutrComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "smutr.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/v/(\\d+)/?");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    protected String getFileTitle(final CryptedLink param, final Browser br) {
        return getFileTitleStatic(br);
    }

    public static String getFileTitleStatic(final Browser br) {
        String title = br.getRegex("<h1 class=\"title\">([^<>\"]+)</h1>").getMatch(0);
        if (title == null) {
            title = br.getRegex("(?i)<title>([^<>\"]+) Porn Video</title>").getMatch(0);
        }
        return title;
    }

    @Override
    protected boolean returnRedirectToUnsupportedLinkAsResult() {
        return true;
    }

    @Override
    protected boolean isOffline(final Browser br) {
        final String offlineTrait = "/404.php";
        if (br.getHttpConnection().getResponseCode() == 404) {
            return true;
        } else if (br.getURL().contains(offlineTrait) || br.getRedirectLocation() != null && br.getRedirectLocation().contains(offlineTrait)) {
            return true;
        } else {
            return false;
        }
    }
}
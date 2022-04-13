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

import org.appwork.utils.Regex;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class CamwhoresTv extends PornEmbedParser {
    public CamwhoresTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    /** Sync this between camwhores hoster + crawler plugins!! */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "camwhores.tv", "camwhores.video", "camwhores.biz", "camwhores.sc", "camwhores.io", "camwhores.adult", "camwhores.cc", "camwhores.org", "camwhores.lol", "camwhorestv.co", "camwhorestv.org" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/videos/(?:\\d+/[a-z0-9\\-]+/|private/[a-z0-9\\-]+/)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    protected boolean isOffline(final Browser br) {
        if (jd.plugins.hoster.CamwhoresTv.isOfflineStatic(br)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected boolean isSelfhosted(final Browser br) {
        final boolean isSelfhostedContent = br.containsHTML("/embed/\\d+");
        final boolean isPrivate = br.containsHTML("(?i)>\\s*This video is a private video uploaded");
        if (isSelfhostedContent || isPrivate) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected String getFileTitle(final CryptedLink param, final Browser br) {
        return new Regex(param.getCryptedUrl(), "/videos/(?:\\d+/|private/)([^/]+)/$").getMatch(0).replace("-", " ").trim();
    }
}

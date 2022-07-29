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
import java.util.regex.Pattern;

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
        ret.add(new String[] { "camwhores.tv", "camlovers.tv", "cam-recorder.net", "camwhores10.com", "camwhores10.tv", "camwhores1.com", "camwhores1.tv", "camwhores2.com", "camwhores2.tv", "camwhores3.com", "camwhores3.tv", "camwhores4.com", "camwhores4.tv", "camwhores5.com", "camwhores5.tv", "camwhores6.com", "camwhores6.tv", "camwhores7.com", "camwhores7.tv", "camwhores8.com", "camwhores8.tv", "camwhores9.com", "camwhores9.tv", "camwhores.adult", "camwhores.agency", "camwhores.art", "camwhoresbay.adult", "camwhoresbay.net", "camwhoresbay.org", "camwhoresbay.porn", "camwhoresbay.sex", "camwhoresbay.sexy", "camwhoresbay.tv", "camwhoresbay.webcam", "camwhoresbay.wtf", "camwhoresbay.xxx", "camwhores.best", "camwhores.biz", "camwhores.camera", "camwhores.cc", "camwhores.click", "camwhores.cloud", "camwhorescloud.net", "camwhorescloud.org", "camwhorescloud.porn", "camwhorescloud.tv",
                "camwhorescloud.xxx", "camwhores.com.co", "camwhores.company", "camwhores.cool", "camwhores.dance", "camwhores.digital", "camwhores.eu", "camwhores.exposed", "camwhores.fans", "camwhores.film", "camwhores.global", "camwhores.guru", "camwhores.id", "camwhores.in", "camwhores.io", "camwhores.lol", "camwhores.love", "camwhores.media", "camwhores.movie", "camwhores.one", "camwhores.online", "camwhores.porn", "camwhores.red", "camwhores.rip", "camwhores.ru.com", "camwhores.run", "camwhores.sex", "camwhores.sexy", "camwhores.shop", "camwhores.show", "camwhores.social", "camwhores.store", "camwhores.stream", "camwhores.studio", "camwhores.sucks", "camwhores.tips", "camwhores.today", "camwhores.top", "camwhores.tube", "camwhorestv.co", "camwhorestv.org", "camwhores.us.com", "camwhores.vc", "camwhores.video", "camwhores.vip", "camwhores.watch", "camwhores.webcam",
                "camwhores.work", "camwhores.works", "camwhores.ws", "camwhores.wtf", "camwhorez.com", "camwhorez.net", "camwhorez.porn", "camwhorez.sex", "camwhorez.tv", "camwhorez.video", "perfectpussy.tv", "purfectpussy.com", "purfectpussy.net", "purfectpussy.org", "purfectpussy.porn", "purfectpussy.sex", "purfectpussy.tv", "vidwhore.com", "camwhores.sc", "camwhores.org" });
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
    protected ArrayList<String> getDeadDomains() {
        return getDeadDomainsStatic();
    }

    public static ArrayList<String> getDeadDomainsStatic() {
        final ArrayList<String> deadDomains = new ArrayList<String>();
        /* 2022-07-29 */
        deadDomains.add("camwhores.org");
        return deadDomains;
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
        final boolean isSelfhostedContent = br.containsHTML(Pattern.quote(br.getHost()) + "/embed/\\d+");
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

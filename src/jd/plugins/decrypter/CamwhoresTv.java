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
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

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
    public List<DownloadLink> convert(Browser br, String title, String url, List<? extends LazyPlugin> lazyPlugins) throws Exception {
        final Iterator<? extends LazyPlugin> it = lazyPlugins.iterator();
        while (it.hasNext()) {
            final LazyPlugin next = it.next();
            if (getHost().equals(next.getDisplayName())) {
                it.remove();
            }
        }
        return super.convert(br, title, url, lazyPlugins);
    }

    /* DEV NOTES */
    /* Porn_plugin */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        this.br.setCookiesExclusive(true);
        String parameter = param.toString();
        getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        } else if (StringUtils.containsIgnoreCase(br.getRedirectLocation(), "cwcams.com/landing")) {
            return decryptedLinks;
        } else if (StringUtils.containsIgnoreCase(br.getRedirectLocation(), "de.stripchat.com")) {
            return decryptedLinks;
        }
        br.followRedirect();
        final String filename = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        final String fid = new Regex(param.getCryptedUrl(), "https?://[^/]+/videos/(\\d+)").getMatch(0);
        /* Avoid crawling embed URL of current item as this website doesn't allow embedding their own selfhosted content. */
        final boolean isSelfhostedContent = fid != null && br.containsHTML("/embed/" + fid);
        final boolean isPrivate = br.containsHTML(">\\s*This video is a private video uploaded");
        if (!isSelfhostedContent && !isPrivate) {
            decryptedLinks.addAll(findEmbedUrls(filename));
        }
        if (decryptedLinks.size() == 0) {
            String id = new Regex(parameter, "/videos/(\\d+)").getMatch(0);
            if (id == null) {
                logger.info("Failed to find videoid, probably private video");
                final String filename_url = new Regex(parameter, "([^/]+/)$").getMatch(0);
                /*
                 * Private videos do not contain videoID inside URL but we can usually find the original URL containing that ID inside html.
                 */
                id = br.getRegex("https?://[^/]+/videos/(\\d+)/" + Pattern.compile(filename_url) + "\"").getMatch(0);
                if (id != null) {
                    logger.info("Found videoid");
                    parameter = "https://www.camwhores.tv/videos/" + id + "/" + filename_url;
                } else {
                    logger.info("Found no videoid at all");
                }
            }
            /* Probably a selfhosted video. */
            final DownloadLink dl = createDownloadlink(parameter);
            decryptedLinks.add(dl);
        }
        return decryptedLinks;
    }
}

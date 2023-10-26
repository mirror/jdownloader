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
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class ManyvidsComCrawler extends PluginForDecrypt {
    public ManyvidsComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "manyvids.com" });
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
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/Video/(\\d+)(/([^/]+))?");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        String title = null;
        final boolean useWebsiteHandling = false;
        final Regex urlinfo = new Regex(param.getCryptedUrl(), this.getSupportedLinks());
        final String videoID = urlinfo.getMatch(0);
        String urlSlug = urlinfo.getMatch(2);
        if (useWebsiteHandling) {
            br.getPage(param.getCryptedUrl());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (this.isAbort()) {
                logger.info("Aborted by user");
                return ret;
            }
            /* Search video title */
            if (urlSlug == null) {
                urlSlug = br.getRegex(videoID + "/" + "([^\"/]+)/").getMatch(0);
            }
            if (urlSlug != null) {
                title = urlSlug.replace("-", " ").trim();
            } else {
                /* Fallback */
                title = videoID;
            }
        }
        /* Find video metadata + screenshots */
        logger.info("Crawling metadata and images");
        final Browser brc = br.cloneBrowser();
        brc.getPage("https://video-player-bff.estore.kiwi.manyvids.com/vercel/videos/" + videoID + "/public");
        final Map<String, Object> entries = restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.MAP);
        final int statusCode = ((Number) entries.get("statusCode")).intValue();
        if (statusCode != 200) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> data = (Map<String, Object>) entries.get("data");
        title = data.get("title").toString();
        final String filesizeStr = (String) data.get("size");
        final String description = (String) data.get("description");
        // final Boolean isFree = (Boolean) data.get("isFree");
        final String urlThumbnail = data.get("thumbnail").toString();
        ret.add(this.createDownloadlink(DirectHTTP.createURLForThisPlugin(urlThumbnail)));
        final String urlScreenshot = data.get("screenshot").toString();
        ret.add(this.createDownloadlink(DirectHTTP.createURLForThisPlugin(urlScreenshot)));
        if (this.isAbort()) {
            logger.info("Aborted by user");
            return ret;
        }
        /* Find stream-downloadurls */
        logger.info("Crawling stream downloadurls");
        brc.getPage("/vercel/videos/" + videoID + "/private");
        final Map<String, Object> entries2 = restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.MAP);
        final Map<String, Object> data2 = (Map<String, Object>) entries2.get("data");
        final Map<String, Object> teasermap = (Map<String, Object>) data2.get("teaser");
        final String videourl1 = (String) data2.get("filepath");
        final String videourl2 = (String) data2.get("transcodedFilepath");
        final String videourlTeaser = teasermap.get("filepath").toString();
        if (!StringUtils.isEmpty(videourl1)) {
            /* Original video -> We may know the filesize of this item. */
            final DownloadLink video1 = this.createDownloadlink(DirectHTTP.createURLForThisPlugin(videourl1));
            if (!StringUtils.isEmpty(filesizeStr)) {
                video1.setDownloadSize(SizeFormatter.getSize(filesizeStr));
            }
            ret.add(video1);
        }
        if (!StringUtils.isEmpty(videourl2)) {
            /* Transcoded video -> We do not know the filesize of this item. */
            final DownloadLink video2 = this.createDownloadlink(DirectHTTP.createURLForThisPlugin(videourl2));
            ret.add(video2);
        }
        ret.add(this.createDownloadlink(DirectHTTP.createURLForThisPlugin(videourlTeaser)));
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        if (!StringUtils.isEmpty(description)) {
            fp.setComment(description);
        }
        fp.setPackageKey("manyvids_com://video/" + videoID);
        /* Set additional properties */
        for (final DownloadLink result : ret) {
            result._setFilePackage(fp);
            result.setAvailable(true);
        }
        return ret;
    }
}

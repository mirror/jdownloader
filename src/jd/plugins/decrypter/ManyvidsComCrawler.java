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
import java.util.Map;
import java.util.Map.Entry;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;
import jd.plugins.hoster.ManyvidsCom;

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
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        if (account != null) {
            final ManyvidsCom hosterplugin = (ManyvidsCom) this.getNewPluginForHostInstance(this.getHost());
            hosterplugin.login(account, false);
        }
        final String manyvidsKeyPrefix = "manyvids_com://";
        String title = null;
        final boolean useWebsiteHandling = false;
        final Regex urlinfo = new Regex(param.getCryptedUrl(), this.getSupportedLinks());
        final String videoID = urlinfo.getMatch(0);
        String urlSlug = urlinfo.getMatch(2);
        if (useWebsiteHandling) {
            /* Deprecated code */
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
        brc.getPage("/vercel/videos/" + videoID + "/private");
        final Map<String, Object> entries2 = restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.MAP);
        final Map<String, Object> data2 = (Map<String, Object>) entries2.get("data");
        boolean crawlVideoStreams = true;
        if (Boolean.TRUE.equals(data2.get("isDownloadable"))) {
            /**
             * Crawl official video downloads </br>
             * Users can download videos if they bought them (or if they are premium/flatrate users??)
             */
            logger.info("Crawling official downloadlinks");
            final UrlQuery query = new UrlQuery();
            query.add("id", videoID);
            query.add("etag", "KIWI_video_player");
            br.postPage("https://www." + this.getHost() + "/download.php?" + query.toString(), query);
            final Map<String, Object> resp = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final Object errorO = resp.get("error");
            if (errorO != null) {
                /* This should never happen */
                logger.info("Failed to obtain official downloadlinks because: " + errorO);
            } else {
                /* Crawl downloadlinks */
                final ArrayList<DownloadLink> officialVideoDownloads = new ArrayList<DownloadLink>();
                final Iterator<Entry<String, Object>> iterator = resp.entrySet().iterator();
                while (iterator.hasNext()) {
                    final Entry<String, Object> entry = iterator.next();
                    final String videoQualityLabel = entry.getKey();
                    final Map<String, Object> qualitymap = (Map<String, Object>) entry.getValue();
                    final String file_url = qualitymap.get("file_url").toString();
                    final String officialVideoDownloadFilesizeStr = qualitymap.get("file_size").toString();
                    /* Don't do this, it will fuckup URL-encoding. */
                    // final DownloadLink video = this.createDownloadlink(DirectHTTP.createURLForThisPlugin(file_url));
                    final DownloadLink video = this.createDownloadlink(file_url);
                    video.setName(qualitymap.get("file_title") + "." + qualitymap.get("file_ext"));
                    video.setDownloadSize(SizeFormatter.getSize(officialVideoDownloadFilesizeStr));
                    video.setLinkID(manyvidsKeyPrefix + "video/" + videoID + "/quality/" + videoQualityLabel);
                    officialVideoDownloads.add(video);
                    if (videoQualityLabel.equalsIgnoreCase("original")) {
                        /* Best possible quality -> Only take this if available */
                        officialVideoDownloads.clear();
                        officialVideoDownloads.add(video);
                        break;
                    }
                }
                ret.addAll(officialVideoDownloads);
                crawlVideoStreams = false;
            }
        }
        if (crawlVideoStreams) {
            logger.info("Crawling stream downloadurls");
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
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        if (!StringUtils.isEmpty(description)) {
            fp.setComment(description);
        }
        fp.setPackageKey(manyvidsKeyPrefix + "video/" + videoID);
        /* Set additional properties */
        for (final DownloadLink result : ret) {
            result._setFilePackage(fp);
            result.setAvailable(true);
        }
        return ret;
    }
}

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
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import org.jdownloader.plugins.components.config.EromeComConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
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
public class EromeComCrawler extends PluginForDecrypt {
    public EromeComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void init() {
        setRequestIntervalLimitGlobal();
    }

    public static void setRequestIntervalLimitGlobal() {
        Browser.setRequestIntervalLimitGlobal("erome.com", 1500);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(410);
        return br;
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "erome.com" });
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

    private static final String PATTERN_ALBUM   = "/a/([A-Za-z0-9]+)";
    private static final String PATTERN_PROFILE = "/([\\w\\-]+)(\\?page=\\d+)?";

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "(" + PATTERN_ALBUM + "|" + PATTERN_PROFILE + ")");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    protected DownloadLink createDownloadlink(String link) {
        final DownloadLink ret = super.createDownloadlink(link);
        ret.setProperty(DirectHTTP.PROPERTY_CUSTOM_HOST, getHost());
        return ret;
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final String contenturl = param.getCryptedUrl().replaceFirst("(?i)http://", "https://");
        br.getPage(contenturl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getHttpConnection().getResponseCode() == 410) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String albumID = new Regex(br._getURL().getPath(), PATTERN_ALBUM).getMatch(0);
        String username = null;
        if (albumID != null) {
            String title = br.getRegex("property=\"og:title\" content=\"([^\"]+)\"").getMatch(0);
            if (title == null) {
                title = br.getRegex("class=\"col-sm-12 page-content\">\\s*<h1>([^<]+)</h1>").getMatch(0);
            }
            // final String preloadImage = br.getRegex("<link rel=\"preload\" href=\"(https?://[^\"]+)\" as=\"image\"").getMatch(0);
            final String uploadername = br.getRegex("id=\"user_name\"[^>]*>([^<]+)<").getMatch(0);
            final String bottomAlbumDescription = br.getRegex("<p id=\"legend\"[^>]*>(.*?)</p>").getMatch(0);
            final String[] mediagrouphtmls = br.getRegex("<div class=\"media-group\" id=\"\\d+\"[^>]*>(.*?)</div>\\s*</div>(.*?)").getColumn(0);
            if (mediagrouphtmls == null || mediagrouphtmls.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final EromeComConfig cfg = PluginJsonConfig.get(this.getConfigInterface());
            for (final String mediagrouphtml : mediagrouphtmls) {
                final String directurlImage = new Regex(mediagrouphtml, "class=\"img\" data-src=\"(https?://[^\"]+)\"").getMatch(0);
                final String directurlVideo = new Regex(mediagrouphtml, "<source src=\"(https?://[^\"]+)\" type='video/mp4'").getMatch(0);
                if (directurlImage == null && directurlVideo == null) {
                    continue;
                } else if (directurlImage != null) {
                    ret.add(this.createDownloadlink(directurlImage));
                } else {
                    ret.add(this.createDownloadlink(directurlVideo));
                    final String videoThumbnail = new Regex(mediagrouphtml, "poster=\"(https?://[^\"]+)\"").getMatch(0);
                    /* Add video thumbnail if user wants that. */
                    if (videoThumbnail != null && cfg.isAddThumbnail()) {
                        ret.add(this.createDownloadlink(videoThumbnail));
                    } else if (videoThumbnail == null) {
                        logger.warning("Failed to find video thumbnail for video: " + directurlVideo);
                    }
                }
            }
            if (ret.isEmpty()) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setCleanupPackageName(false);
            /*
             * Uploaders are often using the same album titles so it is important to include the albumID in packagenames so those won't be
             * merged.
             */
            if (uploadername != null && title != null) {
                fp.setName(Encoding.htmlDecode(uploadername).trim() + " - " + albumID + " - " + Encoding.htmlDecode(title).trim());
            } else if (title != null) {
                fp.setName(albumID + " - " + Encoding.htmlDecode(title).trim());
            } else {
                /* Final fallback */
                fp.setName(albumID);
            }
            if (bottomAlbumDescription != null) {
                fp.setComment(Encoding.htmlDecode(bottomAlbumDescription).trim());
            }
            for (final DownloadLink result : ret) {
                /*
                 * Crucial for linkchecking without Referer header their servers will return "405 Not Allowed" at least for video
                 * directurls.
                 */
                result.setContainerUrl(param.getCryptedUrl());
                result.setReferrerUrl(br.getURL());
                /* Disable chunkload to prevent issues as we don't have fine tuning for connections per file and simultaneous downloads. */
                result.setProperty(DirectHTTP.NOCHUNKS, true);
                result.setProperty(DirectHTTP.PROPERTY_MAX_CONCURRENT, cfg.getMaxSimultaneousDownloads());
                result.setAvailable(true);
                result._setFilePackage(fp);
            }
        } else if ((username = new Regex(br._getURL().getPath(), PATTERN_PROFILE).getMatch(0)) != null) {
            if (!br.containsHTML("\\?t=posts\"")) {
                // no profile url
                return ret;
            } else if (br.containsHTML("<p>No albums</p>")) {
                return ret;
            }
            /* Crawl all album-items of profile */
            final HashSet<String> dupes = new HashSet<String>();
            int page = 1;
            final String urlPage = new Regex(br._getURL().getQuery(), "page=(\\d+)").getMatch(0);
            if (urlPage != null) {
                page = Integer.parseInt(urlPage);
            }
            do {
                int numberofNewItemsThisPage = 0;
                final String[] albumurls = br.getRegex(PATTERN_ALBUM).getColumn(-1);
                if (albumurls == null || albumurls.length == 0) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                for (final String albumurl : albumurls) {
                    if (!dupes.add(albumurl)) {
                        /* Skip duplicates */
                        continue;
                    }
                    numberofNewItemsThisPage++;
                    final DownloadLink album = this.createDownloadlink(br.getURL(albumurl).toExternalForm());
                    ret.add(album);
                    distribute(album);
                }
                logger.info("Crawled page " + page + " | Number of new items on this page: " + numberofNewItemsThisPage + " | Found items so far: " + ret.size());
                if (this.isAbort()) {
                    logger.info("Stopping because: Aborted by user");
                    break;
                } else if (numberofNewItemsThisPage == 0) {
                    logger.info("Stopping because: Failed to find any new items on current page");
                    break;
                } else if (urlPage != null) {
                    logger.info("Stopping because: single page mode");
                    break;
                }
                page++;
                final String nextPageUrl = br.getRegex("(/" + Pattern.quote(username) + "\\?page=" + page + ")").getMatch(0);
                if (nextPageUrl == null) {
                    logger.info("Stopping because: Reached end");
                    break;
                } else {
                    /* Continue to next page */
                    br.getPage(nextPageUrl);
                }
            } while (!this.isAbort());
        } else {
            /* Unsupported URL -> developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return ret;
    }

    @Override
    public Class<? extends EromeComConfig> getConfigInterface() {
        return EromeComConfig.class;
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 2;
    }
}

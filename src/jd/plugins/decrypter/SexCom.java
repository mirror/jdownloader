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
import java.util.Set;
import java.util.regex.Pattern;

import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class SexCom extends PornEmbedParser {
    public SexCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "sex.com" });
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

    private static final String PATTERN_RELATIVE_VIDEO           = "(?i)/video/\\d+.*?";
    private static final String PATTERN_RELATIVE_EXTERN_REDIRECT = "(?i)/link/out\\?id=\\d+";
    private static final String PATTERN_RELATIVE_USER            = "(?i)/user/([a-z0-9\\-]+)/([a-z0-9\\-]+)/";
    private static final String PATTERN_RELATIVE_PIN             = "(?i)/pin/\\d+(-[a-z0-9\\-]+)?/";
    private static final String PATTERN_RELATIVE_PICTURE         = "(?i)/picture/\\d+/?";

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "(" + PATTERN_RELATIVE_VIDEO + "|" + PATTERN_RELATIVE_USER + "|" + PATTERN_RELATIVE_PIN + "|" + PATTERN_RELATIVE_PICTURE + "|" + PATTERN_RELATIVE_EXTERN_REDIRECT + ")");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String externID;
        String title;
        br.setAllowedResponseCodes(502);
        final String contenturl = param.getCryptedUrl().replaceFirst("(?i)http://", "https://");
        String redirect = null;
        if (contenturl.matches(PATTERN_RELATIVE_EXTERN_REDIRECT)) {
            /* Single link which redirects to another website */
            br.setFollowRedirects(false);
            br.getPage(contenturl);
            redirect = this.br.getRedirectLocation();
            ret.add(this.createDownloadlink(redirect));
            return ret;
        }
        br.setFollowRedirects(true);
        br.getPage(contenturl);
        if (isOffline(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        redirect = br.getRegex("onclick=\"window\\.location\\.href=\\'(/[^<>\"]*?)\\'").getMatch(0);
        if (redirect != null) {
            br.getPage(redirect);
        }
        final Pattern videopatternfull = Pattern.compile("https?://[^/]+" + PATTERN_RELATIVE_VIDEO);
        final Pattern userpatternfull = Pattern.compile("https?://[^/]+" + PATTERN_RELATIVE_USER);
        if (new Regex(br.getURL(), userpatternfull).patternFind()) {
            /* Find all items of profile. Those can be spread across multiple pages -> Handle pagination */
            final Set<String> dupes = new HashSet<String>();
            final String userProfilePin = br.getRegex("\"user_profile_picture\"\\s*>\\s*<a\\s*href\\s*=\\s*\"(/pin/\\d+)").getMatch(0);
            dupes.add(userProfilePin);
            int page = 1;
            do {
                int numberofNewItems = 0;
                final String[] urls = HTMLParser.getHttpLinks(br.getRequest().getHtmlCode(), br.getURL());
                for (final String url : urls) {
                    if (this.canHandle(url) && !new Regex(url, userpatternfull).patternFind() && dupes.add(url)) {
                        final DownloadLink dl = createDownloadlink(br.getURL(url).toExternalForm());
                        ret.add(dl);
                        distribute(dl);
                        numberofNewItems++;
                    }
                }
                logger.info("Crawled page " + page + " | Found items so far: " + ret.size());
                final String next = br.getRegex("rel\\s*=\\s*\"next\"\\s*href\\s*=\\s*\"(https?://[^\"]*page=\\d+)").getMatch(0);
                if (this.isAbort()) {
                    logger.info("Stopping because: Aborted by user");
                    break;
                } else if (numberofNewItems == 0) {
                    logger.info("Stopping because: Failed to find any new items on current page");
                    break;
                } else if (next == null) {
                    logger.info("Stopping because: Failed to find nextpage");
                    break;
                } else {
                    /* Continue to next page */
                    br.getPage(next);
                    page++;
                }
            } while (!this.isAbort());
        } else if (new Regex(br.getURL(), videopatternfull).patternFind() || br.containsHTML("<h1>\\s*Video\\s*.*?Pin")) {
            ret.addAll(this.findLink());
        } else {
            /* "PIN" item */
            title = br.getRegex("<title>\\s*([^<>\"]*?)\\s*(?:\\|\\s*Sex Videos and Pictures\\s*\\|\\s*Sex\\.com)?\\s*</title>").getMatch(0);
            if (title == null || title.length() <= 2) {
                title = br.getRegex("addthis:title=\"([^<>\"]*?)\"").getMatch(0);
            }
            if (title == null || title.length() <= 2) {
                title = br.getRegex("property=\"og:title\" content=\"([^<>]*?)\\-  Pin #\\d+ \\| Sex\\.com\"").getMatch(0);
            }
            if (title == null || title.length() <= 2) {
                title = br.getRegex("<div class=\"pin\\-header navbar navbar\\-static\\-top\">[\t\n\r ]+<div class=\"navbar\\-inner\">[\t\n\r ]+<h1>([^<>]*?)</h1>").getMatch(0);
            }
            if (title == null || title.length() <= 2) {
                title = new Regex(param.getCryptedUrl(), "(\\d+)/?$").getMatch(0);
            }
            final String name = br.getRegex("(?:Picture|Video|Gif)\\s*-\\s*<span itemprop\\s*=\\s*\"name\"\\s*>\\s*(.*?)\\s*</span>").getMatch(0);
            if (name != null) {
                title = name;
            }
            title = Encoding.htmlDecode(title).trim();
            title = title.replace("#", "");
            externID = br.getRegex("<div class=\"from\">From <a rel=\"nofollow\" href=\"(https?://[^<>\"]*?)\"").getMatch(0);
            if (externID != null) {
                ret.add(createDownloadlink(externID));
                return ret;
            }
            externID = br.getRegex("<link rel=\"image_src\" href=\"(http[^<>\"]*?)\"").getMatch(0);
            // For .gif images
            if (externID == null) {
                externID = br.getRegex("<div class=\"image_frame\"[^<>]*>\\s*(?:<[^<>]*>)?\\s*<img alt=[^<>]*?src=\"(https?://[^<>\"]*?)\"").getMatch(0);
            }
            if (externID != null) {
                /* Fix encoding */
                externID = Encoding.htmlOnlyDecode(externID);
                final DownloadLink dl = createDownloadlink(DirectHTTP.createURLForThisPlugin(externID));
                // final String filePath = new URL(externID).getPath();
                dl.setContentUrl(contenturl);
                dl.setFinalFileName(this.applyFilenameExtension(title, ".webp"));
                /* 2023-01-04: Add custom header to prefer .webp image (same way browser is doing it). */
                final ArrayList<String[]> customHeaders = new ArrayList<String[]>();
                customHeaders.add(new String[] { "Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8" });
                dl.setProperty(DirectHTTP.PROPERTY_HEADERS, customHeaders);
                dl.setAvailable(true);
                ret.add(dl);
                return ret;
            }
            throw new DecrypterException("Decrypter broken for link: " + param.getCryptedUrl());
        }
        return ret;
    }

    @Override
    protected boolean isOffline(final Browser br) {
        final int responseCode = br.getHttpConnection().getResponseCode();
        if (responseCode == 404 || responseCode == 502) {
            return true;
        } else {
            return false;
        }
    }

    private ArrayList<DownloadLink> findLink() throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        ret.addAll(findEmbedUrls(br, false));
        if (!ret.isEmpty()) {
            return ret;
        }
        final String embedLink = br.getRegex("\"(/video/embed[^<>\"]*?)\"").getMatch(0);
        if (embedLink != null) {
            br.getPage(embedLink);
        }
        String externID = br.getRegex("(file|src):\\s*(\"|')(/video/stream[^<>\"]*?)(\"|')").getMatch(2);
        if (externID == null) {
            externID = br.getRegex("file:[\t\n\r ]*?\"([^<>\"]*?)\"").getMatch(0);
        }
        if (externID == null) {
            externID = br.getRegex("src: '([^<>']+)',\\s*type: 'video/mp4'").getMatch(0);
        }
        if (externID != null) {
            String title = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)-  Pin #\\d+ \\| Sex\\.com\"").getMatch(0);
            if (title == null) {
                title = br.getRegex("<title>([^<>\"]*?)\\| Sex\\.com</title>").getMatch(0);
            }
            if (title == null) {
                title = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
            }
            final String betterTitle = br.getRegex("(?:Picture|Video|Gif)\\s*-\\s*<span itemprop\\s*=\\s*\"name\"\\s*>\\s*(.*?)\\s*</span>").getMatch(0);
            if (betterTitle != null) {
                title = betterTitle;
            }
            if (Encoding.isHtmlEntityCoded(title)) {
                title = Encoding.htmlDecode(title);
            }
            final DownloadLink fina = createDownloadlink("directhttp://" + br.getURL(externID).toExternalForm());
            fina.setContentUrl(br.getURL());
            if (title != null) {
                fina.setFinalFileName(title + ".mp4");
            }
            ret.add(fina);
            return ret;
        }
        externID = br.getRegex("\"(/link/out\\?id=\\d+)\" data\\-hostname").getMatch(0);
        if (externID == null) {
            externID = br.getRegex("href=\"([^<>\"]+)\" data-rel=\"source\"").getMatch(0); // Picture
        }
        if (externID != null) {
            ret.add(this.createDownloadlink(br.getURL(externID).toExternalForm()));
            return ret;
        }
        return null;
    }
}
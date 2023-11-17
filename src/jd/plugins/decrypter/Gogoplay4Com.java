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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.AbstractRecaptchaV2;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class Gogoplay4Com extends PluginForDecrypt {
    public Gogoplay4Com(PluginWrapper wrapper) {
        super(wrapper);
    }

    /** HTML tags to easily find this plugin in the future: streaming.php, download.php */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "gogoplay4.com", "gogoplay5.com", "gogoplay1.com", "goload.pro", "asianwatch.net", "dembed1.com", "membed.net", "membed1.net", "asianplay.net", "asianplay.pro", "gogohd.net", "anihdplay.com", "goone.pro" });
        ret.add(new String[] { "asianload.net", "asianembed.io", "k-vid.net", "asianhdplay.net", "asianhdplay.pro", "asianhd1.com" });
        return ret;
    }

    private List<String> getDeadDomains() {
        final ArrayList<String> deadDomains = new ArrayList<String>();
        deadDomains.add("membed.net");
        deadDomains.add("membed1.net");
        deadDomains.add("dembed1.com");
        return deadDomains;
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(download\\?id=[^/]+|embedplus\\?id=[^/]+|streaming\\.php\\?id=[^/]+|videos/[\\w\\-]+)");
        }
        return ret.toArray(new String[0]);
    }

    private static final String TYPE_DOWNLOAD         = "(?i)(?:https?://[^/]+)?/download\\?id=[\\w\\-]+.*";
    private static final String TYPE_EMBEDPLUS        = "(?i)(?:https?://[^/]+)?/embedplus\\?id=[\\w\\-]+.*";
    private static final String TYPE_STREAMING        = "(?i)(?:https?://[^/]+)?/streaming\\.php\\?id=[\\w\\-]+.*";
    private static final String TYPE_STREAM_SELFEMBED = "(?i)https?://[^/]+/videos/([\\w\\-]+)";

    /**
     * Domain independent handling: Checks if URL looks like it can be handled by this plugin without taking care about whether or not we
     * know the domain.
     */
    public static final boolean looksLikeSupportedPattern(final String url) {
        if (url == null) {
            return false;
        } else if (looksLikeSupportedPatternStreaming(url)) {
            return true;
        } else if (url.matches(TYPE_DOWNLOAD)) {
            return true;
        } else if (url.matches(TYPE_EMBEDPLUS)) {
            return true;
        } else {
            return false;
        }
    }

    public static final boolean looksLikeSupportedPatternStreaming(final String url) {
        if (url.matches(TYPE_STREAMING)) {
            return true;
        } else {
            return false;
        }
    }

    private Browser prepBR(final Browser br) {
        br.setFollowRedirects(true);
        return br;
    }

    private String getContentURL(final CryptedLink param) {
        String contentURL = param.getCryptedUrl();
        final String hostFromAddedURLWithoutSubdomain = Browser.getHost(contentURL, false);
        final List<String> deadDomains = getDeadDomains();
        if (deadDomains != null && deadDomains.contains(hostFromAddedURLWithoutSubdomain)) {
            contentURL = param.getCryptedUrl().replaceFirst(Pattern.quote(hostFromAddedURLWithoutSubdomain) + "/", getHost() + "/");
            logger.info("Corrected domain in added URL: " + hostFromAddedURLWithoutSubdomain + " --> " + getHost());
        }
        return contentURL;
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        prepBR(br);
        if (param.getCryptedUrl().matches(TYPE_STREAM_SELFEMBED)) {
            return this.crawlStream(param);
        } else {
            return this.crawlDownloadlinks(param);
        }
    }

    private ArrayList<DownloadLink> crawlStream(final CryptedLink param) throws IOException, PluginException, InterruptedException, DecrypterException {
        final String contenturl = getContentURL(param);
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.getPage(contenturl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String[] urls = HTMLParser.getHttpLinks(br.getRequest().getHtmlCode(), br.getURL());
        for (final String url : urls) {
            if (looksLikeSupportedPatternStreaming(url)) {
                if (!this.canHandle(url)) {
                    logger.warning("!Developer! You need to add the following domain to this plugin in order to make the currently added link work: " + Browser.getHost(url));
                }
                ret.add(this.createDownloadlink(url));
            }
        }
        return ret;
    }

    private ArrayList<DownloadLink> crawlDownloadlinks(final CryptedLink param) throws IOException, PluginException, InterruptedException, DecrypterException {
        final String contenturl = getContentURL(param);
        final String hostInsideContentURL = Browser.getHost(contenturl, true);
        final UrlQuery query = UrlQuery.parse(contenturl);
        final String id = query.get("id");
        if (id == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String titleFromQuery = query.get("title");
        String packageName;
        if (titleFromQuery != null) {
            /* Use title in query as packagename */
            packageName = titleFromQuery;
        } else {
            /* Get packagename from HTML */
            packageName = br.getRegex("<title>([^<>\"]+)</title>").getMatch(0);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setPackageKey("gogoplay4://" + id);
        if (packageName != null) {
            packageName = Encoding.htmlDecode(packageName).trim();
            fp.setName(packageName);
        } else {
            /* Fallback */
            fp.setName(id);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (contenturl.matches(TYPE_STREAMING)) {
            /* This step doesn't require a captcha to be solved. */
            logger.info("Looking for streamingURLs");
            try {
                br.getPage(contenturl);
                final String[] embedurls = br.getRegex("data-video=\"(https?://[^\"]+)").getColumn(0);
                if (embedurls != null && embedurls.length > 0) {
                    for (final String embedurl : embedurls) {
                        if (embedurl.equals(contenturl) || embedurl.contains("id=" + id)) {
                            logger.info("Found origin again; skipping it: " + embedurl);
                            continue;
                        }
                        final DownloadLink stream = this.createDownloadlink(embedurl);
                        stream._setFilePackage(fp);
                        ret.add(stream);
                        distribute(stream);
                    }
                } else {
                    logger.info("Failed to find any streamURLs");
                }
            } catch (final Exception e) {
                logger.log(e);
                logger.warning("Stream crawler failed");
            }
        }
        br.getPage("https://" + hostInsideContentURL + "/download?" + query.toString());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* 0-2 captchas are required */
        if (AbstractRecaptchaV2.containsRecaptchaV2Class(br) || (br.containsHTML("grecaptcha\\.execute") && br.containsHTML("captcha_v3"))) {
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            final UrlQuery firstCaptcha = new UrlQuery();
            final String recaptchaV3Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br) {
                @Override
                public TYPE getType() {
                    return TYPE.INVISIBLE;
                }
            }.getToken();
            firstCaptcha.add("captcha_v3", Encoding.urlEncode(recaptchaV3Response));
            firstCaptcha.add("id", Encoding.urlEncode(id));
            br.postPage("/download", firstCaptcha);
            if (AbstractRecaptchaV2.containsRecaptchaV2Class(br) || (br.containsHTML("grecaptcha\\.render") && br.containsHTML("captcha_v2"))) {
                /* 2022-03-24: Can be two captchas required?! */
                final UrlQuery nextCaptcha = new UrlQuery();
                final String siteKey = br.getRegex("site_key\\s*=\\s*'(" + AbstractRecaptchaV2.apiKeyRegex + ")'").getMatch(0);
                if (siteKey == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br, siteKey).getToken();
                nextCaptcha.add("captcha_v2", Encoding.urlEncode(recaptchaV2Response));
                nextCaptcha.add("id", Encoding.urlEncode(id));
                br.postPage("/download", nextCaptcha);
                if (AbstractRecaptchaV2.containsRecaptchaV2Class(br) || (br.containsHTML("grecaptcha.render") && br.containsHTML("captcha_v2"))) {
                    /* Website prompts for captcha again -> Should never happen */
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
            }
        }
        final String[] streamLinks = br.getRegex("class=\"dowload\"[^>]*><a\\s*href=\"(https?://[^\"]+)\"").getColumn(0);
        if (streamLinks == null || streamLinks.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (final String streamLink : streamLinks) {
            final DownloadLink link;
            if (streamLink.matches("(?i)^https?://gogo-cdn\\.com/.*")) {
                link = createDownloadlink(DirectHTTP.createURLForThisPlugin(streamLink));
            } else if (streamLink.matches("(?i)https?://[^/]+/download\\.php\\?url=[a-zA-Z0-9_/\\+\\=\\-%]+")) {
                /* 2023-08-17 */
                link = createDownloadlink(DirectHTTP.createURLForThisPlugin(streamLink));
                /* Extract some additional information so we can skip linkcheck. */
                final String b64 = UrlQuery.parse(streamLink).get("url");
                final String b64decodedURL = Encoding.Base64Decode(b64);
                final String filename = Plugin.getFileNameFromURL(b64decodedURL);
                if (filename != null && StringUtils.endsWithCaseInsensitive(filename, ".mp4")) {
                    link.setName(filename);
                    link.setAvailable(true);
                } else {
                    logger.warning("Failed to find filename in: " + b64decodedURL);
                }
            } else {
                link = createDownloadlink(streamLink);
            }
            /* Important otherwise we cannot use their selfhosted URLs! */
            link.setReferrerUrl(contenturl);
            ret.add(link);
        }
        fp.addLinks(ret);
        return ret;
    }
}

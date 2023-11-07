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
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class ProtectMylinksCom extends antiDDoSForDecrypt {
    public ProtectMylinksCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "protect-mylinks.com" });
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
            String regex = "https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(";
            regex += "decrypt(?:\\.php)?\\?i=[a-f0-9]{16}";
            regex += "|f\\?i=[a-f0-9]{16}";
            regex += "|v(?:\\.php)?\\?auth=[a-f0-9]{40}\\&l=\\d+\\&i=[a-f0-9]{16}";
            regex += ")";
            ret.add(regex);
        }
        return ret.toArray(new String[0]);
    }

    private static final String TYPE_FOLDER   = "(?i)https?://[^/]+/decrypt.+";
    private static final String TYPE_SINGLE_1 = "(?i)https?://[^/]+/f\\?i=([a-f0-9]{16})";
    private static final String TYPE_SINGLE_2 = "(?i)https?://[^/]+/v(?:\\.php)?\\?auth=([a-f0-9]{40})\\&l=(\\d+)\\&i=([a-f0-9]{16})";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (param.getCryptedUrl().matches(TYPE_FOLDER)) {
            return this.crawlFolder(param);
        } else if (param.getCryptedUrl().matches(TYPE_SINGLE_2)) {
            final Regex linkinfo = new Regex(param.getCryptedUrl(), TYPE_SINGLE_2);
            /* Important! Otherwise we'll getö prompted to solve a captcha again! */
            final String authID = linkinfo.getMatch(0);
            // final String linkPositionStr = linkinfo.getMatch(1);
            final String folderID = linkinfo.getMatch(2);
            br.setCookie(this.getHost(), folderID, authID);
            br.setFollowRedirects(false);
            getPage(param.getCryptedUrl());
            final String redirectURL = br.getRedirectLocation();
            if (redirectURL == null) {
                /* Assume that URL is offline */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (this.canHandle(redirectURL)) {
                /*
                 * Assume redirect to folder --> Session has expired --> This should never happen and if it does, we should consider adding
                 * extra handling for that (solve captcha, then only crawl that one single link).
                 */
                // return this.processCrawlFolder(param, linkPositionStr, this.br);
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            } else {
                ret.add(this.createDownloadlink(redirectURL));
            }
        } else if (param.getCryptedUrl().matches(TYPE_SINGLE_1)) {
            /* Old handling! */
            br.setFollowRedirects(true);
            getPage(param.getCryptedUrl());
            // can contain some slider event.
            final Form captcha = br.getFormbyKey("_token");
            if (captcha != null) {
                captcha.put("Submit", "");
                submitForm(br, captcha);
            }
            /* 2017-04-20: Server will always return 404 (fake 404)Server */
            final String finallink = br.getRegex("window\\.location\\s*?=\\s*?\"((?:https?:)?//[^<>\"]+)\";").getMatch(0);
            if (finallink == null && br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (finallink == null) {
                return null;
            }
            ret.add(this.createDownloadlink(Request.getLocation(finallink, br.getRequest())));
        } else {
            /* Unsupported URL --> This should never happen! */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return ret;
    }

    private ArrayList<DownloadLink> crawlFolder(final CryptedLink param) throws Exception {
        br.setFollowRedirects(true);
        getPage(param.getCryptedUrl());
        /* Multiple links + captcha */
        if (br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML("alert alert-danger text-center")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return processCrawlFolder(param, null, this.br);
    }

    /** Call this after verifying that what you want to crawl is online! */
    private ArrayList<DownloadLink> processCrawlFolder(final CryptedLink param, final String targetIndex, final Browser br) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String title = br.getRegex("(?i)value=\"Title\\s*:\\s*([^<>\"]+)\"").getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        if (title != null) {
            fp.setName(Encoding.htmlDecode(title).trim());
            fp.setAllowMerge(true);
        }
        final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
        postPage(br.getURL(), "submit=Decrypt+link&g-recaptcha-response=" + Encoding.urlEncode(recaptchaV2Response));
        final String[] urls = HTMLParser.getHttpLinks(br.getRequest().getHtmlCode(), br.getURL());
        for (final String url : urls) {
            if (!url.matches(TYPE_SINGLE_2)) {
                continue;
            }
            final UrlQuery query = UrlQuery.parse(url);
            final String thisIndexStr = query.get("l");
            final DownloadLink link = createDownloadlink(url);
            if (fp != null) {
                link._setFilePackage(fp);
            }
            if (StringUtils.equals(thisIndexStr, targetIndex)) {
                logger.info("Found targetIndex: " + targetIndex + " | " + url);
                ret.clear();
                ret.add(link);
                break;
            } else {
                ret.add(link);
            }
        }
        if (ret.isEmpty()) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return ret;
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        /* 2022-01-31: Let's be gentle */
        return 3;
    }
}

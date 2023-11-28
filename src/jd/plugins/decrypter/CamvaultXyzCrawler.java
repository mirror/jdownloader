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
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.hoster.CamvaultXyz;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class CamvaultXyzCrawler extends PluginForDecrypt {
    public CamvaultXyzCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public void init() {
        setRequestIntervalLimitGlobal();
    }

    public static void setRequestIntervalLimitGlobal() {
        for (final String[] domainlist : getPluginDomains()) {
            for (final String domain : domainlist) {
                Browser.setRequestIntervalLimitGlobal(domain, true, 1000);
            }
        }
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "camvault.xyz" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/download/[A-Za-z0-9\\-_]+\\-\\d+\\.html");
        }
        return ret.toArray(new String[0]);
    }

    public static final boolean isOffline(final Browser br) {
        if (br.getHttpConnection().getResponseCode() == 404) {
            return true;
        } else if (br.containsHTML("(?i)>\\s*The video you have tried to access does no")) {
            return true;
        } else {
            return false;
        }
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        /* Login if account is available */
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        final CamvaultXyz hosterPlugin = (CamvaultXyz) this.getNewPluginForHostInstance(this.getHost());
        final String contenturl = param.getCryptedUrl();
        if (account != null) {
            hosterPlugin.login(account, contenturl, true);
        } else {
            br.getPage(contenturl);
        }
        if (isRateLimitReached(br)) {
            throw new DecrypterRetryException(RetryReason.HOST_RATE_LIMIT);
        } else if (isOffline(br)) {
            /* 2022-10-04: This may also happen for premium-only links when user is missing permission to view those. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String[] videoTokens = br.getRegex("name=\"videoToken\" type=\"hidden\" value=\"([^<>\"]+)\"").getColumn(0);
        if (videoTokens == null || videoTokens.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        FilePackage fp = null;
        boolean crawlExternalDownloadurls = false;
        if (videoTokens.length == 1) {
            /* Selfhosted content? Pass to hosterplugin. */
            logger.info("Looks like selfhosted content");
            final DownloadLink selfhostedVideo = new DownloadLink(hosterPlugin, this.getHost(), this.getHost(), contenturl, true);
            CamvaultXyz.parseFileInfo(br, selfhostedVideo);
            if (selfhostedVideo.getName() != null) {
                fp = FilePackage.getInstance();
                fp.setName(selfhostedVideo.getName());
                selfhostedVideo._setFilePackage(fp);
            }
            ret.add(selfhostedVideo);
            distribute(selfhostedVideo);
            if (br.containsHTML("#megaDownloadModal")) {
                crawlExternalDownloadurls = true;
            }
        } else {
            crawlExternalDownloadurls = true;
        }
        if (crawlExternalDownloadurls) {
            /* 2023-03-24: TODO: Check if it is even still possible that an item can contain multiple/externally hosted items. */
            logger.info("Looks like externally hosted content");
            br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            /* Look for external downloadurls. */
            int progressnum = 0;
            for (final String videoToken : videoTokens) {
                progressnum++;
                logger.info("Crawling item " + progressnum + "/" + videoTokens.length + ": " + videoToken);
                final Browser br2 = this.br.cloneBrowser();
                br2.postPage("/gallery/megadownload", "captcha=&token=" + Encoding.urlEncode(videoToken));
                if (isRateLimitReached(br2)) {
                    throw new DecrypterRetryException(RetryReason.HOST_RATE_LIMIT);
                }
                final Map<String, Object> entries = restoreFromString(br2.getRequest().getHtmlCode(), TypeRef.MAP);
                final String reCaptchaSiteKey = (String) entries.get("sitekey");
                if (!StringUtils.isEmpty(reCaptchaSiteKey)) {
                    /* Usually a reCaptchaV2 is required! */
                    logger.info("Captcha required");
                    final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br2, reCaptchaSiteKey).getToken();
                    br2.postPage("/gallery/megadownload", "captcha=" + Encoding.urlEncode(recaptchaV2Response) + "&token=" + Encoding.urlEncode(videoToken));
                }
                br2.getRequest().setHtmlCode(Encoding.unicodeDecode(br2.toString()));
                br2.getRequest().setHtmlCode(PluginJSonUtils.unescape(br2.toString()));
                final String[] dllinks = br2.getRegex("download\\-link\"><a href=\"(https?[^<>\"]+)\"").getColumn(0);
                for (final String dllink : dllinks) {
                    final DownloadLink item = createDownloadlink(dllink);
                    if (fp != null) {
                        item._setFilePackage(fp);
                    }
                    ret.add(item);
                    distribute(item);
                }
                if (this.isAbort()) {
                    logger.info("Stopping because: Aborted by user");
                    break;
                }
            }
        }
        return ret;
    }

    public static boolean isRateLimitReached(final Browser br) {
        return StringUtils.equalsIgnoreCase(br.getRequest().getHtmlCode(), "Rate limit reached");
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        /* 2022-10-04: Set to 1 to avoid hitting rate-limit */
        return 1;
    }
}

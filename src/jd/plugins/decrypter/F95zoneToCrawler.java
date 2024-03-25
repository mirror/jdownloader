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
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.F95zoneTo;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { F95zoneTo.class })
public class F95zoneToCrawler extends PluginForDecrypt {
    public F95zoneToCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void init() {
        for (final String[] domainlist : getPluginDomains()) {
            for (final String domain : domainlist) {
                Browser.setRequestIntervalLimitGlobal(domain, true, 1500);
            }
        }
    }

    public static List<String[]> getPluginDomains() {
        return F95zoneTo.getPluginDomains();
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(masked/.+|threads/([^/#\\?]+\\.\\d+|\\d+)/((page|post)-\\d+)?)");
        }
        return ret.toArray(new String[0]);
    }

    private final String PATTERN_SINGLE   = "(?i)https?://[^/]+/masked/.+";
    private final String PATTERN_THREAD_1 = "(?i)https?://[^/]+/threads/([^/#\\?]+)\\.\\d+/((page|post)-\\d+)?";
    private final String PATTERN_THREAD_2 = "(?i)https?://[^/]+/threads/\\d+/((page|post)-\\d+)?";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        if (account == null) {
            throw new AccountRequiredException();
        }
        final F95zoneTo hosterplugin = (F95zoneTo) this.getNewPluginForHostInstance(this.getHost());
        hosterplugin.login(account, false);
        br.setFollowRedirects(true);
        if (param.getCryptedUrl().matches(PATTERN_SINGLE)) {
            return crawlSingleLink(param);
        } else if (param.getCryptedUrl().matches(PATTERN_THREAD_1) || param.getCryptedUrl().matches(PATTERN_THREAD_2)) {
            return crawlForumThreadPage(param);
        } else {
            /* Unsupported URL */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    private ArrayList<DownloadLink> crawlSingleLink(final CryptedLink param) throws Exception {
        br.setAllowedResponseCodes(400);
        final boolean superfastCrawling = true;
        final String action;
        if (!superfastCrawling) {
            br.getPage(param.getCryptedUrl());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            action = br.getURL();
        } else {
            br.getHeaders().put("Referer", param.getCryptedUrl());
            action = param.getCryptedUrl();
        }
        final UrlQuery query = new UrlQuery();
        query.add("xhr", "1");
        query.add("download", "1");
        br.postPage(action, query);
        if (br.getHttpConnection().getResponseCode() == 400) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        String status = entries.get("status").toString();
        boolean captchaWasRequired = false;
        if (StringUtils.equalsIgnoreCase(status, "captcha")) {
            /* {"status":"captcha","msg":"Please complete the CAPTCHA to continue"} */
            final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br, "6LcwQ5kUAAAAAAI-_CXQtlnhdMjmFDt-MruZ2gov").getToken();
            query.add("captcha", Encoding.urlEncode(recaptchaV2Response));
            br.postPage(action, query);
            entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            status = entries.get("status").toString();
            captchaWasRequired = true;
        }
        if (!StringUtils.equals(status, "ok")) {
            if (captchaWasRequired) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        final String url = entries.get("msg").toString();
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        ret.add(this.createDownloadlink(url));
        return ret;
    }

    private ArrayList<DownloadLink> crawlForumThreadPage(final CryptedLink param) throws Exception {
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String urlSlug = new Regex(param.getCryptedUrl(), PATTERN_THREAD_1).getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        final String title = br.getRegex("property=\"og:title\" content=\"([^\"]+)\"").getMatch(0);
        if (title != null) {
            fp.setName(Encoding.htmlDecode(title).trim());
        } else if (urlSlug != null) {
            /* Fallback */
            fp.setName(urlSlug.replace("-", " ").trim());
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String[] externalURLs = br.getRegex("<a href=\"(https?://[^\"]+)\"[^<]*class=\"link link--external\"").getColumn(0);
        for (final String externalURL : externalURLs) {
            ret.add(this.createDownloadlink(externalURL));
        }
        fp.addLinks(ret);
        return ret;
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        /* 2022-10-05: Try to prevent captchas when crawling single links. */
        return 1;
    }
}

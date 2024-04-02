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
package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class MydirtyhobbyCom extends PluginForHost {
    public MydirtyhobbyCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://" + getHost() + "/");
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setAllowedResponseCodes(410);
        br.setFollowRedirects(true);
        /* Important else we'll get redirected to a "Are you 18+" page. */
        br.setCookie(getHost(), "AGEGATEPASSED", "1");
        return br;
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    @Override
    public String getAGBLink() {
        return "https://cdn1-l-ha-e11.mdhcdn.com/u/TermsofUse_de.pdf";
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "mydirtyhobby.com", "mydirtyhobby.de" });
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
            ret.add("https?://(?:[a-z]+\\.)?" + buildHostsPatternPart(domains) + "/profil/(\\d+)([A-Za-z0-9\\-]+)/videos/(\\d+)([A-Za-z0-9\\-]+)");
        }
        return ret.toArray(new String[0]);
    }

    private String dllink = null;

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return "mydirtyhobby://video/" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(2);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, null, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        final String urlSlug = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(3);
        String titleUrl = null;
        if (urlSlug != null) {
            titleUrl = urlSlug.replace("-", " ").trim();
        }
        final String default_extension = ".mp4";
        if (!link.isNameSet()) {
            link.setName(titleUrl + default_extension);
        }
        dllink = null;
        if (account != null) {
            this.login(account, false);
        }
        br.getPage(link.getPluginPatternMatcher());
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (this.br.getHttpConnection().getResponseCode() == 410) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String username = this.br.getRegex("class=\"fa fa-calendar fa-fw fa-dt\"></i>.*?title=\"([^<>\"]+)\">([^<>\"]+)</a>").getMatch(0);
        // if (username == null) {
        // username = "amateur";
        // }
        String filename = br.getRegex("<h\\d+ class=\"page\\-title pull\\-left\">([^<>\"]+)</h\\d+>").getMatch(0);
        if (filename == null) {
            /* Fallback */
            filename = titleUrl;
        }
        if (filename != null) {
            filename = Encoding.htmlDecode(filename).trim();
            if (username != null) {
                filename = username + " - " + filename;
            }
            link.setFinalFileName(filename + default_extension);
        }
        if (account == null) {
            /* Nothing more to do when we're not logged in. */
            return AvailableStatus.TRUE;
        }
        final String json = br.getRegex("ProfileVideo,\\s*\"profile_page\",\\s*(\\{.*?\\})\\s*\\)\\s*</script>").getMatch(0);
        if (json != null) {
            final Map<String, Object> entries = restoreFromString(json, TypeRef.MAP);
            final List<Map<String, Object>> sources = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(entries, "content/videoPurchased/src");
            if (sources != null) {
                for (final Map<String, Object> source : sources) {
                    final String type = source.get("type").toString();
                    /* Look for progressive video-stream */
                    if (type.equalsIgnoreCase("video/mp4")) {
                        this.dllink = source.get("src").toString();
                        break;
                    }
                }
            }
        }
        if (dllink != null && !isDownload) {
            /* Get- and set filesize */
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.isContentDecoded()) {
                        link.setDownloadSize(con.getCompleteContentLength());
                    } else {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        this.requestFileInformation(link, null, true);
        throw new AccountRequiredException();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            br.setCookiesExclusive(true);
            final Cookies cookies = account.loadCookies("");
            /* Re-use cookies whenever possible - avoid login captcha! */
            if (cookies != null) {
                br.setCookies(cookies);
                if (!force) {
                    /* Do not validate cookies */
                    return;
                }
                br.getPage("https://" + this.getHost());
                if (isLoggedIn(br)) {
                    logger.info("Cookie login successful");
                    account.saveCookies(br.getCookies(br.getHost()), "");
                    return;
                } else {
                    /* Full login needed */
                    logger.info("Cookie login failed");
                    br.clearCookies(null);
                    account.clearCookies("");
                }
            }
            br.getPage("https://www." + getHost() + "/n/login");
            final UrlQuery query = new UrlQuery();
            query.add("username", Encoding.urlEncode(account.getUser()));
            query.add("password", Encoding.urlEncode(account.getPass()));
            query.add("regContinuity", "1");
            query.add("continuity_redirect", "/");
            query.add("rv", "true");
            /*
             * 2016-07-22: In browser it might happen too that the first login attempt will always fail with correct logindata - second
             * attempt must not even require a captcha but will usually be successful!!
             */
            boolean success = false;
            String failureErrormessage = null;
            // String jwt = null;
            for (int i = 0; i <= 1; i++) {
                /*
                 * In case we need a captcha it will only appear after the first login attempt so we need (max) 2 attempts to ensure that
                 * user can enter the captcha if needed.
                 */
                if (br.containsHTML("class=\"g\\-recaptcha\"")) {
                    if (this.getDownloadLink() == null) {
                        // login wont contain downloadlink
                        this.setDownloadLink(new DownloadLink(this, "Account Login!", this.getHost(), this.getHost(), true));
                    }
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, this.br).getToken();
                    query.addAndReplace("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                } else if (i > 0) {
                    logger.info("Round >=2 but no captcha -> Quit login loop");
                    break;
                }
                final PostRequest req = br.createPostRequest("/n/login", query);
                req.getHeaders().put("Accept", "application/json, text/plain, */*");
                req.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.setAllowedResponseCodes(400);
                br.getPage(req);
                final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                final Number result = (Number) entries.get("result");
                if (result != null && result.intValue() == 1) {
                    success = true;
                    // jwt = entries.get("jwt").toString();
                    break;
                } else {
                    /* Failure */
                    final Object messageO = entries.get("message");
                    if (messageO instanceof List) {
                        final List<String> errormessages = (List<String>) messageO;
                        failureErrormessage = errormessages.get(0);
                    } else {
                        failureErrormessage = messageO.toString();
                    }
                    continue;
                }
            }
            if (!success) {
                throw new AccountInvalidException(failureErrormessage);
            }
            /* Double-check */
            br.getPage("/");
            if (!isLoggedIn(br)) {
                /* This should never happen! */
                logger.warning("Login via WebAPI was successful but according to HTML code we are not logged in");
                throw new AccountInvalidException();
            }
            account.saveCookies(br.getCookies(br.getHost()), "");
        }
    }

    private boolean isLoggedIn(final Browser br) {
        return br.containsHTML("(/\\?ac=dologout|/logout\")");
    }

    /** There are no free- or premium accounts. Users can only watch the videos they bought. */
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        final String coinsStr = br.getRegex("\"coins\":\"?(\\d+)").getMatch(0);
        ai.setUnlimitedTraffic();
        if (br.containsHTML("isVip\\s*:\\s*true")) {
            account.setType(AccountType.PREMIUM);
        } else {
            account.setType(AccountType.FREE);
        }
        account.setConcurrentUsePossible(true);
        final String coinsHumanReadable;
        if (coinsStr != null) {
            coinsHumanReadable = coinsStr;
        } else {
            coinsHumanReadable = "Unknown";
        }
        ai.setStatus(account.getType().getLabel() + " | Coins: " + coinsHumanReadable);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, account, true);
        if (br.containsHTML("name=\"buy\"")) {
            throw new AccountRequiredException("This content needs to be bought separately");
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        link.setProperty("premium_directlink", dllink);
        dl.startDownload();
    }

    @Override
    public boolean canHandle(final DownloadLink downloadLink, final Account account) throws Exception {
        if (account == null) {
            /* Without account its not possible to download any link for this host. */
            return false;
        } else {
            return true;
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
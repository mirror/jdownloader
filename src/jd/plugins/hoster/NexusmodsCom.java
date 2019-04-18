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

import java.net.URL;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "nexusmods.com" }, urls = { "https?://(?:www\\.)?nexusmods\\.com+/Core/Libs/Common/Widgets/DownloadPopUp\\?id=\\d+.+" })
public class NexusmodsCom extends antiDDoSForHost {
    public NexusmodsCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.nexusmods.com/signup");
    }

    @Override
    public String getAGBLink() {
        return "https://help.nexusmods.com/article/18-terms-of-service";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME                  = true;
    private static final int     FREE_MAXCHUNKS               = 0;
    private static final int     FREE_MAXDOWNLOADS            = 20;
    private static final boolean ACCOUNT_FREE_RESUME          = true;
    private static final int     ACCOUNT_FREE_MAXCHUNKS       = 0;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS    = 20;
    // private static final boolean ACCOUNT_PREMIUM_RESUME = true;
    // private static final int ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    private String               dllink;
    private boolean              loginRequired;

    public boolean isOffline(final Browser br) {
        return br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("No files have been uploaded yet|>File not found<|>Not found<|/noimage-1.png");
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        final String url = link.getPluginPatternMatcher();
        if (StringUtils.contains(url, "nmm=1")) {
            link.setPluginPatternMatcher(url.replace("nmm=1", "nmm=0"));
        }
    }

    public boolean isLoginRequired(final Browser br) {
        if (br.containsHTML("<h1>Error</h1>") && br.containsHTML("<h2>Adult-only content</h2>")) {
            // adult only content.
            return true;
        } else if (br.containsHTML("You need to be a member and logged in to download files larger")) {
            // large files
            return true;
        } else if (br.containsHTML(">Please login or signup to download this file<")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final String fid = getFID(link.getPluginPatternMatcher());
        getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        {
            String loadBox = br.getRegex("loadBox\\('(https?://.*?)'").getMatch(0);
            if (loadBox != null) {
                getPage(loadBox);
                loadBox = br.getRegex("loadBox\\('(https?://.*?skipdonate)'").getMatch(0);
                if (loadBox != null) {
                    getPage(loadBox);
                }
            }
        }
        loginRequired = isLoginRequired(br);
        dllink = br.getRegex("window\\.location\\.href\\s*=\\s*\"(http[^<>\"]+)\";").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("\"(https?://filedelivery\\.nexusmods\\.com/[^<>\"]+)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("\"(https?://(?:www\\.)?nexusmods\\.com/[^<>\"]*Libs/Common/Managers/Downloads\\?Download[^<>\"]+)\"").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("id\\s*=\\s*\"dl_link\"\\s*value\\s*=\\s*\"(https?://(?:[a-z0-9]*\\.)?(?:nexusmods|nexus-cdn)\\.com/[^<>\"]*?)\"").getMatch(0);
                    if (dllink == null) {
                        dllink = br.getRegex("data-link\\s*=\\s*\"(https?://(?:premium-files|fs-[a-z0-9]+)\\.(?:nexusmods|nexus-cdn)\\.com/[^<>\"]*?)\"").getMatch(0);
                    }
                }
            }
        }
        String filename = br.getRegex("filedelivery\\.nexusmods\\.com/\\d+/([^<>\"]+)\\?fid=").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("data-link\\s*=\\s*\"https?://[^<>\"]+/files/\\d+/([^<>\"/]+)\\?").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("data-link\\s*=\\s*\"https?://[^<>\"]+/\\d+/\\d+/([^<>\"/]+)\\?").getMatch(0);
                if (filename == null && dllink != null) {
                    filename = getFileNameFromURL(new URL(dllink));
                }
            }
            if (filename == null && !link.isNameSet()) {
                filename = fid;
            }
        }
        if (filename != null) {
            filename = Encoding.htmlDecode(filename).trim();
            link.setName(filename);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, null, FREE_RESUME, FREE_MAXCHUNKS);
    }

    private void doFree(final DownloadLink downloadLink, final Account account, final boolean resumable, final int maxchunks) throws Exception, PluginException {
        if (dllink == null) {
            if (loginRequired) {
                if (account != null) {
                    /*
                     * 2019-01-23: Added errorhandling but this should never happen because if an account exists we should be able to
                     * download!
                     */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Login failure");
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
                }
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
        }
        dl.startDownload();
    }

    public String getFID(final String dlurl) {
        String ret = new Regex(dlurl, "id=(\\d+)").getMatch(0);
        if (ret == null) {
            ret = new Regex(dlurl, "mods/(\\d+)").getMatch(0);
        }
        return ret;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    private boolean isCookieSet(Account account, String key) {
        final String value = br.getCookie(account.getHoster(), key);
        return StringUtils.isNotEmpty(value) && !StringUtils.equalsIgnoreCase(value, "deleted");
    }

    public void login(final Account account) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    br.setCookies(account.getHoster(), cookies);
                    getPage("https://www." + account.getHoster());
                    final boolean isLoggedinCookies = isCookieSet(account, "member_id") && isCookieSet(account, "sid") && isCookieSet(account, "pass_hash");
                    final boolean isLoggedinHTML = br.containsHTML("class=\"username\"");
                    if (!isLoggedinCookies || !isLoggedinHTML) {
                        logger.info("Existing login invalid: Full login required!");
                        br.clearCookies(getHost());
                    } else {
                        account.saveCookies(br.getCookies(account.getHoster()), "");
                        return;
                    }
                }
                getPage("https://www." + account.getHoster() + "/Core/Libs/Common/Widgets/LoginPopUp?url=%2F%2Fwww.nexusmods.com%2F");
                final PostRequest request = new PostRequest("https://www.nexusmods.com/Sessions?TryNewLogin");
                request.put("username", Encoding.urlEncode(account.getUser()));
                request.put("password", Encoding.urlEncode(account.getPass()));
                request.put("uri", "%2F%2Fwww.nexusmods.com%2F");
                final DownloadLink original = this.getDownloadLink();
                if (original == null) {
                    this.setDownloadLink(new DownloadLink(this, "Account", getHost(), "http://" + br.getRequest().getURL().getHost(), true));
                }
                try {
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, "6LfTA2EUAAAAAIyUT3sr2W8qKUV1IauZl-CduEix").getToken();
                    if (recaptchaV2Response == null) {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                    request.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                } finally {
                    if (original == null) {
                        this.setDownloadLink(null);
                    }
                }
                request.setContentType("application/x-www-form-urlencoded");
                sendRequest(request);
                if (!isCookieSet(account, "member_id") || !isCookieSet(account, "sid") || !isCookieSet(account, "pass_hash")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(br.getCookies(account.getHoster()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account);
        ai.setUnlimitedTraffic();
        getPage("/users/myaccount");
        if (StringUtils.equalsIgnoreCase(br.getRegex("\"premium-desc\">\\s*(.*?)\\s*<").getMatch(0), "Inactive")) {
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setConcurrentUsePossible(false);
        } else {
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setConcurrentUsePossible(true);
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        /* Important! Login before requestFileInformation! */
        login(account);
        requestFileInformation(link);
        /* Free- and premium download is the same. */
        doFree(link, account, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS);
    }

    public void getPage(Browser ibr, String page) throws Exception {
        super.getPage(ibr, page);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_FREE_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
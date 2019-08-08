//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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

import java.io.IOException;

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "xtube.com" }, urls = { "https?://(?:www\\.)?xtube\\.com/(?:video-watch/(?:embedded/)?|(watch|play_re)\\.php\\?v=)[A-Za-z0-9_\\-]+" })
public class XTubeCom extends PluginForHost {
    public XTubeCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
        this.enablePremium("https://www.xtube.com/vip/join");
    }

    private static final String  ALLOW_MULTIHOST_USAGE           = "ALLOW_MULTIHOST_USAGE";
    private static final boolean default_allow_multihoster_usage = false;
    private String               dllink                          = null;
    private boolean              privateVideo                    = false;
    private boolean              server_issues                   = false;

    public void correctDownloadLink(DownloadLink link) {
        link.setPluginPatternMatcher(link.getPluginPatternMatcher().replace("play_re", "watch").replace("/embedded/", "/"));
    }

    @Override
    public String getAGBLink() {
        return "https://wiki2.xtube.com/index.php?title=Terms_of_Use&action=purge";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        /* Offline links should also have nice filenames */
        link.setName(new Regex(link.getPluginPatternMatcher(), "([A-Za-z0-9_\\-]+)$").getMatch(0));
        // this.setBrowserExclusive();
        br.setCookie(this.getHost(), "cookie_warning", "deleted");
        br.setCookie(this.getHost(), "cookie_warning", "S");
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getURL().contains("msg=Invalid+Video+ID") || br.containsHTML(">Video not available<|img/removed_video|>This video has been removed from XTube") || this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /*
         * 2019-08-08: Some videos are still downloadable via the 'old way' so we only set this if we really aren't able to find a
         * downloadurl!
         */
        boolean maybePremiumonly = br.containsHTML("data-tr-action=\"watchpage_login_from_videoplayer\"");
        if (!maybePremiumonly) {
            /* When loggedin and the video still isn't downloadable the HTML looks different! */
            /*
             * Although when not loggedin, website states that such content is downloadable via FREE account it seems like this is only
             * possible when you "buy any amount of credits to unlock the offer" which essentially means that credits- or a premium account
             * is required and this content still cannot be viewed/downloaded via free account!
             */
            maybePremiumonly = br.containsHTML("data-tr-action=\"watchpage_get_free_movie_background\"");
        }
        String filename = null;
        if (br.getURL().contains("play.php?preview_id=")) {
            filename = br.getRegex("class=\"sectionNoStyleHeader\">([^<>\"]*?)</div>").getMatch(0);
        } else {
            filename = br.getRegex("<h1>\\s*(.*?)\\s*</h1>").getMatch(0);
            // For DVD preview links
            if (filename == null) {
                filename = br.getRegex("id=\"videoDetails\">[\t\n\r ]+<p class=\"title\">([^<>\"]*?)</p>").getMatch(0);
            }
        }
        String ownerName = br.getRegex("\\?field_subscribe_user_id=([^<>\"]*?)\"").getMatch(0);
        if (ownerName == null) {
            ownerName = "undefined";
        }
        /* Find highest quality available! */
        for (int i = 2000; i >= 100; i--) {
            dllink = this.br.getRegex("\"" + i + "\":\"(http[^<>\"]+)\"").getMatch(0);
            if (dllink != null) {
                dllink = dllink.replace("\\", "");
                break;
            }
        }
        String fileID = new Regex(link.getPluginPatternMatcher(), "xtube\\.com/watch\\.php\\?v=(.+)").getMatch(0);
        if (fileID == null) {
            fileID = br.getRegex("contentId\" value=\"([^\"]+)\"").getMatch(0);
        }
        if (fileID != null) {
            link.setLinkID(this.getHost() + "://" + fileID);
        }
        if (dllink == null) {
            /* Try the old way. */
            if ("undefined".equals(ownerName)) {
                final String contentOwnerId = br.getRegex("contentOwnerId\" value=\"([^\"]+)\"").getMatch(0);
                if (contentOwnerId != null) {
                    ownerName = contentOwnerId;
                }
            }
            if (fileID == null) {
                logger.warning("fileID is null");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final Browser brc = br.cloneBrowser();
            brc.postPage("https://www.xtube.com/find_video.php", "user%5Fid=" + Encoding.urlEncode(ownerName) + "&clip%5Fid=&video%5Fid=" + Encoding.urlEncode(fileID));
            dllink = brc.getRegex("\\&filename=(http.*?)($|\r|\n| )").getMatch(0);
            if (dllink == null) {
                dllink = brc.getRegex("\\&filename=(%2Fvideos.*?hash.+)").getMatch(0);
            }
        }
        if (filename == null) {
            /* This should never happen */
            logger.warning("filename is null");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename).trim();
        link.setFinalFileName(filename + ".mp4");
        if (!StringUtils.isEmpty(dllink) && this.dllink.startsWith("http")) {
            dllink = Encoding.htmlDecode(dllink);
            if (dllink.contains("/notfound")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(this.dllink);
                if (con.getResponseCode() == 403) {
                    privateVideo = true;
                } else if (con.getContentType().contains("text") || !con.isOK() || con.getLongContentLength() == -1) {
                    server_issues = true;
                } else {
                    link.setDownloadSize(con.getLongContentLength());
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        } else {
            this.privateVideo = maybePremiumonly;
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (this.privateVideo) {
            /*
             * Free account, paid account or account with money ('coins') required to buy/view content. This may still happen if the user
             * uses an account as some videos need to be bought separately!
             */
            throw new AccountRequiredException();
        } else if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                boolean isLoggedin = false;
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    br.getPage("https://" + this.getHost() + "/");
                    isLoggedin = this.isLoggedin();
                }
                if (!isLoggedin) {
                    /*
                     * 2019-08-08: Try up to two times because first login may fail and captcha needs to be completed (captcha is not
                     * necessarily displayed on first attempt although sometimes required to login!).
                     */
                    int counter = 0;
                    do {
                        final boolean ajaxLogin = false;
                        if (ajaxLogin) {
                            br.getPage("https://" + this.getHost() + "/");
                            br.postPage("/ajax/auth", "panel=login&url=https%3A%2F%2Fwww.xtube.com%2F&trackCategory=login&trAction=header_login_button");
                        } else {
                            br.getPage("https://www." + this.getHost() + "/auth/login/");
                        }
                        Form loginform = br.getFormbyProperty("id", "authFormLoginForm");
                        if (loginform == null) {
                            logger.warning("Failed to find loginform");
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        loginform.put("user_id", account.getUser());
                        loginform.put("password", account.getPass());
                        loginform.put("rememberMe", "1");
                        // loginform.put("", "");
                        if (loginform.containsHTML("g-recaptcha")) {
                            final DownloadLink dlinkbefore = this.getDownloadLink();
                            final DownloadLink dl_dummy;
                            if (dlinkbefore != null) {
                                dl_dummy = dlinkbefore;
                            } else {
                                dl_dummy = new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster(), true);
                                this.setDownloadLink(dl_dummy);
                            }
                            final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                            if (dlinkbefore != null) {
                                this.setDownloadLink(dlinkbefore);
                            }
                            loginform.put("g-recaptcha-response", recaptchaV2Response);
                        }
                        br.submitForm(loginform);
                        isLoggedin = this.isLoggedin();
                        counter++;
                    } while (!isLoggedin && counter <= 1);
                    if (!isLoggedin) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean isLoggedin() {
        return br.getCookie(this.getHost(), "xtube_auth", Cookies.NOTDELETEDPATTERN) != null && br.getCookie(this.getHost(), "xtube_cookies", Cookies.NOTDELETEDPATTERN) != null;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (final PluginException e) {
            throw e;
        }
        ai.setUnlimitedTraffic();
        String moneyStr = null;
        double money = 0;
        final String cookie_accountinfo_json = br.getCookie(br.getHost(), "xtube_cookies");
        if (cookie_accountinfo_json != null) {
            moneyStr = PluginJSonUtils.getJson(Encoding.htmlDecode(cookie_accountinfo_json), "cookie_xtube_money");
        }
        if (moneyStr != null && moneyStr.matches("\\d+\\.\\d+")) {
            money = Double.parseDouble(moneyStr);
        }
        /*
         * 2019-08-08: Money can be used to buy videos and there are also "VIP GOLD" accounts which we did not have for testing:
         * https://www.xtube.com/vip/join It seems like VIP accounts can get higher quality videos.
         */
        if (money > 0) {
            ai.setStatus("Account with money: $ " + moneyStr);
            account.setType(AccountType.PREMIUM);
        } else {
            ai.setStatus("Account without money");
            account.setType(AccountType.FREE);
        }
        account.setConcurrentUsePossible(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        login(account, false);
        requestFileInformation(link);
        this.handleFree(link);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return false;
    }

    private void setConfigElements() {
        String user_text;
        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
            user_text = "Erlaube den Download von Links dieses Anbieters über Multihoster (nicht empfohlen)?\r\n<html><b>Kann die Anonymität erhöhen, aber auch die Fehleranfälligkeit!</b>\r\nAktualisiere deine(n) Multihoster Account(s) nach dem Aktivieren dieser Einstellung um diesen Hoster in der Liste der unterstützten Hoster deines/r Multihoster Accounts zu sehen (sofern diese/r ihn unterstützen).</html>";
        } else {
            user_text = "Allow links of this host to be downloaded via multihosters (not recommended)?\r\n<html><b>This might improve anonymity but perhaps also increase error susceptibility!</b>\r\nRefresh your multihoster account(s) after activating this setting to see this host in the list of the supported hosts of your multihost account(s) (in case this host is supported by your used multihost(s)).</html>";
        }
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_MULTIHOST_USAGE, JDL.L("plugins.hoster." + this.getClass().getName() + ".ALLOW_MULTIHOST_USAGE", user_text)).setDefaultValue(default_allow_multihoster_usage));
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        if (this.getPluginConfig().getBooleanProperty(ALLOW_MULTIHOST_USAGE, default_allow_multihoster_usage)) {
            return true;
        } else {
            return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}
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

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.CaptchaException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "pan.baidu.com" }, urls = { "http://(?:www\\.)?pan\\.baidudecrypted\\.com/\\d+" }, flags = { 2 })
public class PanBaiduCom extends PluginForHost {

    public PanBaiduCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium();
    }

    @Override
    public String getAGBLink() {
        return "http://pan.baidu.com/";
    }

    private String              DLLINK                                     = null;
    private static final String TYPE_FOLDER_LINK_NORMAL_PASSWORD_PROTECTED = "http://(www\\.)?pan\\.baidu\\.com/share/init\\?shareid=\\d+\\&uk=\\d+";
    private static final String USER_AGENT                                 = "netdisk;4.8.3.1;PC;PC-Windows;6.3.9600;WindowsBaiduYunGuanJia";
    // private static final String USER_AGENT =
    // "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36";
    private static final String APPID                                      = "250528";

    private static final String NICE_HOST                                  = "pan.baidu.com";
    private static final String NICE_HOSTproperty                          = "panbaiducom";

    /* Connection stuff */
    private final boolean       FREE_RESUME                                = true;
    private final int           FREE_MAXCHUNKS                             = 0;
    private final int           FREE_MAXDOWNLOADS                          = -1;

    /** Known API errors/responses: -20 = Captcha needed / captcha wrong */

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        br = new Browser();
        if (downloadLink.getBooleanProperty("offline", false)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.getHeaders().put("Accept-Charset", null);
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        // Other or older User-Agents might get slow speed
        br.getHeaders().put("User-Agent", USER_AGENT);
        // From decrypter
        DLLINK = downloadLink.getStringProperty("dlink", null);
        // From host plugin
        if (DLLINK == null) {
            DLLINK = downloadLink.getStringProperty("panbaidudirectlink", null);
        }
        if (DLLINK == null) {
            // We might need to enter a captcha to get the link so let's just stop here
            downloadLink.setAvailable(true);
            return AvailableStatus.TRUE;
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, "freedirectlink");
    }

    private void doFree(final DownloadLink downloadLink, final String directlinkproperty) throws Exception {
        final String fsid = downloadLink.getStringProperty("important_fsid", null);
        String sign;
        String tsamp;
        String passCode = downloadLink.getDownloadPassword();
        DLLINK = checkDirectLink(downloadLink, directlinkproperty);
        if (DLLINK == null) {
            /* Needed to get the pcsett cookie on http://.pcs.baidu.com/ to avoid "hotlinking forbidden" errormessage later */
            getPage(this.br, "http://pcs.baidu.com/rest/2.0/pcs/file?method=plantcookie&type=ett");
            final String original_url = downloadLink.getStringProperty("mainLink", null);
            final String shareid = downloadLink.getStringProperty("origurl_shareid", null);
            final String uk = downloadLink.getStringProperty("origurl_uk", null);
            final String link_password = downloadLink.getStringProperty("important_link_password", null);
            final String link_password_cookie = downloadLink.getStringProperty("important_link_password_cookie", null);
            if (shareid == null || uk == null) {
                /* Should never happen */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (link_password_cookie != null) {
                br.setCookie("http://pan.baidu.com/", "BDCLND", link_password_cookie);
            }
            /* Access mainpage before or we might get random 403- and 404's !!! */
            this.br.getPage("http://pan.baidu.com/");
            getPage(this.br, original_url);
            /* Re-check here for offline because if we always used the directlink before, we cannot know if the link is still online. */
            if (br.containsHTML("id=\"share_nofound_des\"")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }

            /* Experimental code */
            String i_frame = br.getRegex("<iframe src=\"(http://pan\\.baidu\\.com/share/link\\?shareid=\\d+\\&uk=\\d+\\&t=[A-Za-z0-9]+)\"").getMatch(0);
            if (i_frame == null) {
                i_frame = br.getRegex("<iframe src=\"(http://pan\\.baidu\\.com/share/link\\?shareid=\\d+\\&uk=\\d+\\&t=[A-Za-z0-9]+)\"").getMatch(0);
            }
            if (i_frame != null) {
                logger.info("Found i_frame - accessing it!");
                getPage(this.br, i_frame);
            } else {
                logger.info("Found no i_frame");
            }

            /* Fallback handling if the password cookie didn't work */
            if (link_password != null && br.getURL().matches(TYPE_FOLDER_LINK_NORMAL_PASSWORD_PROTECTED)) {
                postPage(this.br, "http://pan.baidu.com/share/verify?" + "vcode=&shareid=" + shareid + "&uk=" + uk + "&t=" + System.currentTimeMillis(), "&pwd=" + Encoding.urlEncode(link_password));
                if (!br.containsHTML("\"errno\":0")) {
                    // Wrong password -> Impossible
                    logger.warning("pan.baidu.com: Couldn't download password protected link even though the password is given...");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                getPage(this.br, original_url);
            }
            sign = br.getRegex("FileUtils\\.share_sign=\"([a-z0-9]+)\"").getMatch(0);
            if (sign == null) {
                sign = br.getRegex("yunData\\.SIGN = \"([a-z0-9]+)\"").getMatch(0);
            }
            if (sign == null) {
                sign = br.getRegex("\"sign\":\"([^<>\"]*?)\"").getMatch(0);
            }
            tsamp = br.getRegex("FileUtils\\.share_timestamp=\"(\\d+)\"").getMatch(0);
            if (tsamp == null) {
                tsamp = br.getRegex("yunData\\.TIMESTAMP = \"(\\d+)\"").getMatch(0);
            }
            if (tsamp == null) {
                tsamp = br.getRegex("\"timestamp\":(\\d+)").getMatch(0);
            }
            if (sign == null || tsamp == null) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "File only downloadable via account or only via the pan.baidu.com App/Downloadmanager");
            }
            Browser br2 = prepAjax(br.cloneBrowser());
            try {
                getPage(br2, "http://pan.baidu.com/share/autoincre?channel=chunlei&clienttype=0&web=1&type=1&shareid=" + shareid + "&uk=" + uk + "&sign=" + sign + "&timestamp=" + tsamp + "&bdstoken=null");
            } catch (final Throwable e) {
            }
            /* Last revision without API & csflg handling: 26909 */
            final String postLink = "http://pan.baidu.com/api/sharedownload?sign=" + sign + "&timestamp=" + tsamp + "&bdstoken=&channel=chunlei&clienttype=0&web=1&app_id=" + APPID;
            String postData = "encrypt=0&product=share&uk=" + uk + "&primaryid=" + shareid + "&fid_list=%5B" + fsid + "%5D";
            final String specialCookie = br.getCookie("http://pan.baidu.com/", "BDCLND");
            if (link_password_cookie != null) {
                postData += "&extra=%7B%22sekey%22%3A%22" + link_password_cookie + "%22%7D";
            } else if (specialCookie != null) {
                logger.info("Special cookie is available --> Using it as 'sekey'");
                postData += "&extra=%7B%22sekey%22%3A%22" + specialCookie + "%22%7D";
            }
            br2 = prepAjax(br.cloneBrowser());
            postPage(br2, postLink, postData);
            if (br2.containsHTML("\"errno\":\\-20")) {
                final int repeat = 3;
                for (int i = 1; i <= repeat; i++) {
                    getPage(br2, "http://pan.baidu.com/api/getcaptcha?prod=share&bdstoken=&channel=chunlei&clienttype=0&web=1&app_id=" + APPID);
                    String captchaLink = getJson(br2, "vcode_img");
                    final String vcode_str = new Regex(captchaLink, "([A-Z0-9]+)$").getMatch(0);
                    if (captchaLink == null || vcode_str == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    String code = null;
                    try {
                        code = getCaptchaCode(captchaLink, downloadLink);
                    } catch (final Throwable e) {
                        if (e instanceof CaptchaException) {
                            // JD2 reference to skip button we should abort!
                            throw (CaptchaException) e;
                        }
                        // failure of image download! or opening file?, retry should get new captcha image..
                        logger.info("Captcha download failed -> Retrying!");
                        throw new PluginException(LinkStatus.ERROR_RETRY, "Captcha download failed");
                    }
                    if (code == null || "".equals(code) || code.matches("\\s+")) {
                        // this covers users entering: empty, whitespace. usually happens by accident.
                        if (i + 1 != repeat) {
                            continue;
                        } else {
                            // exhausted retry count
                            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                        }
                    }
                    br2 = prepAjax(br.cloneBrowser());
                    postPage(br2, postLink, postData + "&vcode_input=" + Encoding.urlEncode(code) + "&vcode_str=" + vcode_str);
                    captchaLink = getJson(br2, "img");
                    if (!br2.containsHTML("\"errno\":\\-20")) {
                        // success!
                        break;
                    } else if (i + 1 == repeat && captchaLink != null) {
                        // exhausted retry count
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    } else if (i + 1 != repeat) {
                        // since captchaLink can't be null it must contain captcha and it's fine to continue
                        continue;
                    }
                }
            }
            if (br2.containsHTML("\"errno\":\\-20")) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            } else if (br2.containsHTML("\"errno\":112")) {
                handlePluginBroken(downloadLink, "unknownerror112", 3);
            } else if (br2.containsHTML("\"errno\":118")) {
                logger.warning("It seems like one or multiple parameters are missing in the previous request(s)");
                handlePluginBroken(downloadLink, "unknownerror118", 3);
            } else if (br2.containsHTML("No htmlCode read")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error_dllink", 5 * 60 * 1000l);
            }
            DLLINK = getJson(br2, "dlink");
            if (DLLINK == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html") || dl.getConnection().getResponseCode() == 403) {
            br.followConnection();
            if (br.containsHTML("\"error_code\":31326")) {
                /* Should only happen in case they detect and block us */
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error 31326", 30 * 60 * 1000l);
            }
        }
        downloadLink.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())));
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        downloadLink.setProperty(directlinkproperty, DLLINK);
        this.dl.startDownload();
    }

    private void getPage(final Browser br, final String url) throws IOException, PluginException {
        br.getPage(url);
        if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("/error/core.html")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File is on a damaged HDD, cannot download at the moment");
        }
    }

    @SuppressWarnings("deprecation")
    private void postPage(final Browser br, final String url, final String postdata) throws IOException, PluginException {
        br.postPage(url, postdata);
        if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("/error/core.html")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File is on a damaged HDD, cannot download at the moment");
        }
    }

    private Browser prepAjax(final Browser prepBr) {
        prepBr.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        prepBr.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        return prepBr;
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from String source.
     *
     * @author raztoki
     * */
    private String getJson(final String source, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(source, key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     * */
    private String getJson(final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(br.toString(), key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from provided Browser.
     *
     * @author raztoki
     * */
    private String getJson(final Browser ibr, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(ibr.toString(), key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value given JSon Array of Key from JSon response provided String source.
     *
     * @author raztoki
     * */
    private String getJsonArray(final String source, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJsonArray(source, key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value given JSon Array of Key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     * */
    private String getJsonArray(final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJsonArray(br.toString(), key);
    }

    /**
     * Wrapper<br/>
     * Tries to return String[] value from provided JSon Array
     *
     * @author raztoki
     * @param source
     * @return
     */
    private String[] getJsonResultsFromArray(final String source) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJsonResultsFromArray(source);
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property, null);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            final Browser br2 = br.cloneBrowser();
            br2.setFollowRedirects(true);
            try {
                con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1 || con.getResponseCode() == 403) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Exception e) {
                }
            }
        }
        return dllink;
    }

    /**
     * Is intended to handle out of date errors which might occur seldom by re-tring a couple of times before throwing the out of date
     * error.
     *
     * @param dl
     *            : The DownloadLink
     * @param error
     *            : The name of the error
     * @param maxRetries
     *            : Max retries before out of date error is thrown
     */
    private void handlePluginBroken(final DownloadLink dl, final String error, final int maxRetries) throws PluginException {
        int timesFailed = dl.getIntegerProperty(NICE_HOSTproperty + "failedtimes_" + error, 0);
        dl.getLinkStatus().setRetryCount(0);
        if (timesFailed <= maxRetries) {
            logger.info(NICE_HOST + ": " + error + " -> Retrying");
            timesFailed++;
            dl.setProperty(NICE_HOSTproperty + "failedtimes_" + error, timesFailed);
            throw new PluginException(LinkStatus.ERROR_RETRY, "Final download link not found");
        } else {
            dl.setProperty(NICE_HOSTproperty + "failedtimes_" + error, Property.NULL);
            logger.info(NICE_HOST + ": " + error + " -> Plugin is broken");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    private static Object LOCK = new Object();

    @SuppressWarnings("deprecation")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    this.br.setCookies(this.getHost(), cookies);
                    return;
                }
                br.setFollowRedirects(false);
                br.getPage("http://pan.baidu.com");
                /* TODO: Maybe add a value for 'gid' */
                br.getPage("https://passport.baidu.com/v2/api/?getapi&tpl=netdisk&subpro=netdisk_web&apiver=v3&tt=" + System.currentTimeMillis() + "&class=login&gid=&logintype=basicLogin&callback=bd__cbs__38gea8");
                final String token = this.br.getRegex("\"token\"[\t\n\r ]*?:[\t\n\r ]*?\"([^<>\"]+)\"").getMatch(0);
                if (token == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                String errorno = null;
                for (int i = 0; i <= 1; i++) {
                    String postData = "staticpage=http%3A%2F%2Fpan.baidu.com%2Fres%2Fstatic%2Fthirdparty%2Fpass_v3_jump.html&charset=utf-8&token=" + Encoding.urlEncode(token) + "&tpl=netdisk&subpro=netdisk_web&apiver=v3&tt=" + System.currentTimeMillis() + "&codestring=&safeflg=0&u=http%3A%2F%2Fpan.baidu.com%2F&isPhone=false&detect=1&gid=&quick_user=0&logintype=basicLogin&logLoginType=pc_loginBasic&idc=&loginmerge=true&foreignusername=&" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&verifycode=&mem_pass=on&ppui_logintime=0&countrycode=&callback=parent.bd__pcbs__22ko42";
                    br.postPage("https://passport.baidu.com/v2/api/?login", postData);
                    if (this.br.containsHTML("blabla_TODO")) {
                        /* Login captcha needed */
                        final String captchaurl = "https://passport.baidu.com/cgi-bin/genimage?jxIcaptchaservice";
                        final DownloadLink dummyLink = new DownloadLink(this, "Account", this.getHost(), "http://" + this.getHost(), true);
                        final String c = getCaptchaCode(captchaurl, dummyLink);
                    }
                    errorno = this.br.getRegex("err_no=(\\d+)").getMatch(0);
                    break;
                }
                if (br.getCookie(this.getHost(), "UBI") == null || "257".equals(errorno)) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        /* 2016-04-21: So far all accounts are handled as free accounts - free does not have any limits anyways! */
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        ai.setStatus("Registered (free) user");
        account.setValid(true);
        return ai;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        doFree(link, "account_free_directlink");
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return true;
    }
}
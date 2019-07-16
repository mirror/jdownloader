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
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
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
import jd.plugins.CaptchaException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.StringUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "pan.baidu.com" }, urls = { "https?://(?:www\\.)?pan\\.baidudecrypted\\.com/\\d+" })
public class PanBaiduCom extends PluginForHost {
    public PanBaiduCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium();
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://pan.baidu.com/";
    }

    private String               DLLINK                                     = null;
    private static final String  TYPE_FOLDER_LINK_NORMAL_PASSWORD_PROTECTED = "https?://(www\\.)?pan\\.baidu\\.com/share/init\\?shareid=\\d+\\&uk=\\d+";
    // private static final String USER_AGENT =
    // "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36";
    private static final String  APPID                                      = "250528";
    private static final String  NICE_HOST                                  = "pan.baidu.com";
    private static final String  NICE_HOSTproperty                          = "panbaiducom";
    public static final long     trust_cookie_age                           = 300000l;
    /* Connection stuff */
    private static final boolean FREE_RESUME                                = true;
    private static final int     FREE_MAXCHUNKS                             = 1;
    private static final int     FREE_MAXDOWNLOADS                          = 20;
    // note: CAN NOT be negative or zero! (ie. -1 or 0) Otherwise math sections fail. .:. use [1-20]
    private static AtomicInteger totalMaxSimultanFreeDownload               = new AtomicInteger(FREE_MAXDOWNLOADS);
    // don't touch the following!
    private static AtomicInteger maxFree                                    = new AtomicInteger(1);
    private boolean              accountOnly                                = true;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        br = new Browser();
        if (downloadLink.getBooleanProperty("offline", false)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.getHeaders().put("Accept-Charset", null);
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        // Other or older User-Agents might get slow speed
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

    private void doFree(final DownloadLink downloadLink, final String directlinkproperty) throws Exception {
        String passCode = downloadLink.getDownloadPassword();
        DLLINK = checkDirectLink(downloadLink, directlinkproperty);
        if (DLLINK == null) {
            final String fsid = downloadLink.getStringProperty("important_fsid", null);
            String sign;
            String tsamp;
            /* Needed to get the pcsett cookie on http://.pcs.baidu.com/ to avoid "hotlinking forbidden" errormessage later */
            getPage(this.br, "https://pcs.baidu.com/rest/2.0/pcs/file?method=plantcookie&type=ett");
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
                br.setCookie("https://pan.baidu.com/", "BDCLND", link_password_cookie);
            }
            /* Access mainpage before or we might get random 403- and 404's !!! */
            this.br.getPage("https://" + this.getHost() + "/");
            br.setFollowRedirects(true);
            getPage(this.br, original_url);
            /* Re-check here for offline because if we always used the directlink before, we cannot know if the link is still online. */
            if (br.containsHTML("id=\"share_nofound_des\"")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            /* Experimental code */
            String i_frame = br.getRegex("<iframe src=\"(https?://pan\\.baidu\\.com/share/link\\?shareid=\\d+\\&uk=\\d+\\&t=[A-Za-z0-9]+)\"").getMatch(0);
            if (i_frame == null) {
                i_frame = br.getRegex("<iframe src=\"(https?://pan\\.baidu\\.com/share/link\\?shareid=\\d+\\&uk=\\d+\\&t=[A-Za-z0-9]+)\"").getMatch(0);
            }
            if (i_frame != null) {
                logger.info("Found i_frame - accessing it!");
                getPage(this.br, i_frame);
            } else {
                logger.info("Found no i_frame");
            }
            /* Fallback handling if the password cookie didn't work */
            if (link_password != null && br.getURL().matches(TYPE_FOLDER_LINK_NORMAL_PASSWORD_PROTECTED)) {
                postPage(this.br, "https://pan.baidu.com/share/verify?" + "vcode=&shareid=" + shareid + "&uk=" + uk + "&t=" + System.currentTimeMillis(), "&pwd=" + Encoding.urlEncode(link_password));
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
                throw new AccountRequiredException();
            }
            Browser br2 = prepAjax(br.cloneBrowser());
            try {
                getPage(br2, "https://pan.baidu.com/share/autoincre?channel=chunlei&clienttype=0&web=1&type=1&shareid=" + shareid + "&uk=" + uk + "&sign=" + sign + "&timestamp=" + tsamp + "&bdstoken=null");
            } catch (final Throwable e) {
            }
            /* Last revision without API & csflg handling: 26909 */
            final String postLink = "https://pan.baidu.com/api/sharedownload?sign=" + sign + "&timestamp=" + tsamp + "&bdstoken=&channel=chunlei&clienttype=0&web=1&app_id=" + APPID;
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
                    getPage(br2, "https://pan.baidu.com/api/getcaptcha?prod=share&bdstoken=&channel=chunlei&clienttype=0&web=1&app_id=" + APPID);
                    String captchaLink = PluginJSonUtils.getJsonValue(br2, "vcode_img");
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
                    captchaLink = PluginJSonUtils.getJsonValue(br2, "img");
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
            DLLINK = PluginJSonUtils.getJsonValue(br2, "dlink");
            if (DLLINK == null && accountOnly) {
                /* DLLINK null and not logged in but download is only possible via account */
                throw new AccountRequiredException();
            } else if (DLLINK == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        br.setFollowRedirects(true);
        final int maxchunks;
        if (this.getPluginConfig().getBooleanProperty("ALLOW_UNLIMITED_CHUNKS", false)) {
            maxchunks = 0;
        } else {
            maxchunks = FREE_MAXCHUNKS;
        }
        br.getHeaders().put("Accept-Encoding", "identity");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, FREE_RESUME, maxchunks);
        if (dl.getConnection().getContentType().contains("html") || dl.getConnection().getResponseCode() == 403) {
            if (dl.getConnection().getResponseCode() == 403 && accountOnly) {
                throw new AccountRequiredException();
            } else if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            handleJsonErrorcodes(this.br);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())));
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        String md5 = getMD5FromDispositionHeader(dl.getConnection());
        if (md5 != null) {
            /* There is also a case where MD5 is not present. Setting only if there is MD5. */
            downloadLink.setMD5Hash(md5);
        }
        downloadLink.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        try {
            // add a download slot
            controlFree(+1);
            // start the dl
            dl.startDownload();
        } finally {
            // remove download slot
            controlFree(-1);
        }
    }

    /**
     * Prevents more than one free download from starting at a given time. One step prior to dl.startDownload(), it adds a slot to maxFree
     * which allows the next singleton download to start, or at least try.
     *
     * This is needed because xfileshare(website) only throws errors after a final dllink starts transferring or at a given step within pre
     * download sequence. But this template(XfileSharingProBasic) allows multiple slots(when available) to commence the download sequence,
     * this.setstartintival does not resolve this issue. Which results in x(20) captcha events all at once and only allows one download to
     * start. This prevents wasting peoples time and effort on captcha solving and|or wasting captcha trading credits. Users will experience
     * minimal harm to downloading as slots are freed up soon as current download begins.
     *
     * @param controlFree
     *            (+1|-1)
     */
    public synchronized void controlFree(final int num) {
        logger.info("maxFree was = " + maxFree.get());
        maxFree.set(Math.min(Math.max(1, maxFree.addAndGet(num)), totalMaxSimultanFreeDownload.get()));
        logger.info("maxFree now = " + maxFree.get());
    }

    private String getMD5FromDispositionHeader(final URLConnectionAdapter urlConnection) {
        return urlConnection.getHeaderField("Content-MD5");
    }

    private void getPage(final Browser br, final String url) throws IOException, PluginException {
        br.getPage(url);
        handleUrlErrors(br);
    }

    @SuppressWarnings("deprecation")
    private void postPage(final Browser br, final String url, final String postdata) throws IOException, PluginException {
        br.postPage(url, postdata);
        handleUrlErrors(br);
    }

    private void handleUrlErrors(final Browser br) throws PluginException {
        String url_to_check = br.getRedirectLocation();
        if (url_to_check == null) {
            url_to_check = br.getURL();
        }
        if (url_to_check != null) {
            if (url_to_check.contains("/error/core.html")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File is on a damaged HDD, cannot download at the moment");
            }
        }
        final long responsecode = br.getHttpConnection().getResponseCode();
        if (responsecode == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
        } else if (responsecode == 404) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
        }
    }

    private void handleJsonErrorcodes(final Browser br) throws PluginException {
        final String error_code_str = PluginJSonUtils.getJson(br, "error_code");
        final String error_msg = PluginJSonUtils.getJson(br, "error_msg");
        final int error_code = error_code_str != null ? Integer.parseInt(error_code_str) : 0;
        switch (error_code) {
        case 0:
            /* Everything alright */
            break;
        case 31064:
            /* { "error_code":31064, "error_msg":"xcode expire time out error" } */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, error_msg, 60 * 1000l);
        case 31326:
            /* Should only happen in case they detect- and block us. */
            /* 2016-12-15: Which error_msg will this be?? */
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error 31326", 30 * 60 * 1000l);
        }
        String url_to_check = br.getRedirectLocation();
        if (url_to_check == null) {
            url_to_check = br.getURL();
        }
        if (url_to_check != null) {
            if (url_to_check.contains("/error/core.html")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File is on a damaged HDD, cannot download at the moment");
            }
        }
    }

    private Browser prepAjax(final Browser prepBr) {
        prepBr.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        prepBr.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        return prepBr;
    }

    /**
     * private String checkDirectLink(final DownloadLink downloadLink, final String property) { String dllink =
     * downloadLink.getStringProperty(property, null); if (dllink != null) { URLConnectionAdapter con = null; final Browser br2 =
     * br.cloneBrowser(); br2.setFollowRedirects(true); try { con = br2.openGetConnection(dllink); if (con.getContentType().contains("html")
     * || con.getLongContentLength() == -1 || con.getResponseCode() == 403) { downloadLink.setProperty(property, Property.NULL); dllink =
     * null; } } catch (final Exception e) { downloadLink.setProperty(property, Property.NULL); dllink = null; } finally { try {
     * con.disconnect(); } catch (final Exception e) { } } } return dllink; }
     *
     * /** Is intended to handle out of date errors which might occur seldom by re-tring a couple of times before throwing the out of date
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

    @SuppressWarnings("deprecation")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= trust_cookie_age) {
                        /* We trust these cookies --> Do not check them */
                        return;
                    }
                    if (isLoggedInHtml()) {
                        /* Save new cookie timestamp (will also avoid unnecessary login captchas). */
                        account.saveCookies(this.br.getCookies(this.getHost()), "");
                        return;
                    }
                    /* Not logged in? Perform full login! */
                }
                final boolean loginV3_qrcode = false;
                if (loginV3_qrcode) {
                    br.setFollowRedirects(true);
                    br.getPage("https://passport.baidu.com/v2/?login");
                    br.getPage("https://passport.baidu.com/v2/api/getqrcode?lp=pc&qrloginfrom=pc&gid=TODO_FIXME&callback=tangram_guid_" + System.currentTimeMillis() + "&apiver=v3&tt=" + System.currentTimeMillis() + "&tpl=pp&_=" + System.currentTimeMillis());
                    // final String url_qrcode = PluginJSonUtils.getJson(br, "imgurl");
                    final String sign = PluginJSonUtils.getJson(br, "sign");
                    if (StringUtils.isEmpty(sign)) {
                        logger.warning("Failed to find sign");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    br.getHeaders().put("User-Agent", "Mozilla/5.0 (Linux; Android 4.4.2; Nexus 4 Build/KOT49H) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/34.0.1847.114 Mobile Safari/537.36");
                    String vcodestr = null;
                    String traceid = null;
                    String userid = null;
                    for (int i = 0; i <= 3; i++) {
                        br.getPage("https://wappass.baidu.com/wp/?qrlogin&t=" + System.currentTimeMillis() + "&error=0&sign=" + sign + "&cmd=login&lp=pc&tpl=pp&uaonly=&client_id=&adapter=3&client=&qrloginfrom=pc&traceid=");
                        final Form loginform = br.getForm(0);
                        if (loginform == null) {
                            logger.warning("Failed to find loginform");
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        if (loginform.getAction() == null) {
                            loginform.setAction("https://wappass.baidu.com/wp/api/login?tt=" + System.currentTimeMillis());
                        }
                        loginform.put("username", account.getUser());
                        loginform.put("password", "TODO_pw_hash");
                        /* Although we're using a mobile User-Agent, this is the correct value */
                        loginform.put("isphone", "0");
                        loginform.put("countrycode", "");
                        loginform.put("mobilenum", "");
                        loginform.put("logLoginType", "wap_loginTouch");
                        loginform.put("vcodefrom", "login");
                        /* TODO */
                        // loginform.put("servertime", "TODO");
                        // loginform.put("gid", "TODO");
                        // loginform.put("dv", "TODO");
                        // loginform.put("tk", "TODO");
                        // loginform.put("traceid", "TODO");
                        if (!true) {
                            /* Captcha required */
                            /*
                             * E.g. https://wappass.baidu.com/cgi-bin/genimage?njGd206e2ff64fae257027915cfde0154149520de06930213c3&v=
                             * 1541637709658
                             */
                            final String captchaurl = "https://wappass.baidu.com/cgi-bin/genimage?" + vcodestr + "&v=TODO";
                            final DownloadLink dummyLink = new DownloadLink(this, "Account", this.getHost(), "http://" + this.getHost(), true);
                            final String captchaCode = getCaptchaCode(captchaurl, dummyLink);
                            loginform.put("vericode", captchaCode);
                        }
                        br.submitForm(loginform);
                        /* Required in form for the next loop (e.g. captcha required/wrong) */
                        vcodestr = PluginJSonUtils.getJson(br, "codeString");
                        traceid = PluginJSonUtils.getJson(br, "traceid");
                        userid = PluginJSonUtils.getJson(br, "userid");
                        /* E.g. +57123*****89 */
                        final String phonenumber_censored = PluginJSonUtils.getJson(br, "phone");
                        if (!StringUtils.isEmpty(userid)) {
                            break;
                        }
                    }
                    if (userid == null) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    logger.info("First login stage complete");
                    /* Okay we're one step further but not yet fully logged in ... */
                } else {
                    br.setFollowRedirects(false);
                    final String getdata = "tpl=pp&callback=bdPass.api.login._needCodestringCheckCallback&index=0&logincheck=&time=0&username=" + Encoding.urlEncode(account.getUser());
                    this.br.getPage("https://passport.baidu.com/v2/api/?logincheck&" + getdata);
                    /* Get captcha if required. */
                    String captchaCode = "";
                    final String codestring = PluginJSonUtils.getJson(this.br, "codestring");
                    if (codestring != null && !codestring.equalsIgnoreCase("null")) {
                        final String captchaurl = "https://passport.baidu.com/cgi-bin/genimage?" + codestring;
                        final DownloadLink dummyLink = new DownloadLink(this, "Account", this.getHost(), "https://" + this.getHost(), true);
                        captchaCode = getCaptchaCode(captchaurl, dummyLink);
                    }
                    this.br.getPage("https://passport.baidu.com/v2/api/?getapi&class=login&tpl=pp&tangram=false");
                    final String logintoken = this.br.getRegex("login_token=\\'([^<>\"\\']+)\\'").getMatch(0);
                    if (logintoken == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final String loginpost = "token=" + Encoding.urlEncode(logintoken) + "&ppui_logintime=1600000&charset=utf-8&codestring=&isPhone=false&index=0&u=&safeflg=0&staticpage=" + Encoding.urlEncode("http://www.baidu.com/cache/user/html/jump.html") + "&loginType=1&tpl=pp&callback=parent.bd__pcbs__qvljue&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&verifycode=" + Encoding.urlEncode(captchaCode) + "&mem_pass=on&apiver=v3";
                    br.postPage("https://passport.baidu.com/v2/api/?login", loginpost);
                    if (!isLoggedInHtml()) {
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

    private boolean isLoggedInHtml() throws Exception {
        this.br.getPage("http://www.baidu.com/home/msg/data/personalcontent");
        final String errorNo = PluginJSonUtils.getJsonValue(this.br, "errNo");
        return "0".equals(errorNo);
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        /* 2016-04-21: So far all accounts are handled as free accounts - free does not have any limits anyways! */
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        ai.setStatus("Registered (free) user");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        doFree(link, "account_free_directlink");
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "ALLOW_UNLIMITED_CHUNKS", "Allow unlimited [=20] connections per file (chunks)?\r\nWarning: This can cause download issues.").setDefaultValue(false));
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return maxFree.get();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return maxFree.get();
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return true;
    }
}
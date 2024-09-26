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

import org.appwork.utils.encoding.URLEncode;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "pan.baidu.com" }, urls = { "https?://(?:www\\.)?pan\\.baidudecrypted\\.com/\\d+" })
public class PanBaiduCom extends PluginForHost {
    public PanBaiduCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium();
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.getHeaders().put("Accept-Charset", null);
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public String getAGBLink() {
        return "https://" + getHost();
    }

    private String               dllink                                     = null;
    private static final String  TYPE_FOLDER_LINK_NORMAL_PASSWORD_PROTECTED = "(?i)https?://(www\\.)?pan\\.baidu\\.com/share/init\\?shareid=\\d+\\&uk=\\d+";
    private static final String  APPID                                      = "250528";
    private static final String  NICE_HOST                                  = "pan.baidu.com";
    private static final String  NICE_HOSTproperty                          = "panbaiducom";
    /* Connection stuff */
    private static final boolean FREE_RESUME                                = true;
    private static final int     FREE_MAXCHUNKS                             = 1;
    private static final boolean accountOnly                                = true;
    public static final String   PROPERTY_FSID                              = "important_fsid";
    public static final String   PROPERTY_SHORTURL_ID                       = "shorturl_id";
    public static final String   PROPERTY_PASSWORD_COOKIE                   = "important_link_password_cookie";
    public static final String   PROPERTY_INTERNAL_MD5_HASH                 = "internal_md5hash";
    public static final String   PROPERTY_INTERNAL_PATH                     = "internal_path";
    public static final String   PROPERTY_INTERNAL_UK                       = "origurl_uk";
    public static final String   PROPERTY_INTERNAL_SHAREID                  = "origurl_shareid";
    public static final String   PROPERTY_SERVER_FILENAME                   = "server_filename";

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    public int getMaxChunks(final DownloadLink link, final Account account) {
        return 1;
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fsid = link.getStringProperty(PROPERTY_FSID);
        final String internal_md5_hash = link.getStringProperty(PROPERTY_INTERNAL_MD5_HASH);
        if (fsid != null || internal_md5_hash != null) {
            return this.getHost() + "://" + fsid + "_" + internal_md5_hash;
        } else {
            return super.getLinkID(link);
        }
    }

    @Override
    public String getPluginContentURL(final DownloadLink link) {
        final String url_legacy = link.getStringProperty("mainLink");
        if (url_legacy != null) {
            return url_legacy;
        }
        final String shorturl_id = link.getStringProperty(PROPERTY_SHORTURL_ID);
        final String uk = link.getStringProperty(PROPERTY_INTERNAL_UK);
        final String shareid = link.getStringProperty(PROPERTY_INTERNAL_SHAREID);
        final String internal_path = link.getStringProperty(PROPERTY_INTERNAL_PATH);
        if (shorturl_id != null) {
            String url = "https://pan.baidu.com/s/" + shorturl_id;
            if (internal_path != null) {
                url += "#list/path=" + URLEncode.encodeURIComponent(internal_path);
            }
            return url;
        } else if (uk != null && shareid != null) {
            return String.format("https://pan.baidu.com/share/link?shareid=%s&uk=%s", shareid, uk);
        } else {
            /* Fallback */
            return super.getPluginContentURL(link);
        }
    }

    @Override
    public String buildExternalDownloadURL(final DownloadLink link, final PluginForHost plugin) {
        if (plugin != null && plugin.hasFeature(LazyPlugin.FEATURE.MULTIHOST)) {
            String url = this.getPluginContentURL(link);
            /* Include unique fileID in links for multihosters so they can match them correctly. */
            final String fsid = link.getStringProperty(PROPERTY_FSID);
            if (fsid != null) {
                url += "&fsid=" + fsid;
            }
            return url;
        } else {
            return super.buildExternalDownloadURL(link, plugin);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        dllink = link.getStringProperty("dlink", null);
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        doFree(link, "freedirectlink");
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property, null);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            final Browser br2 = br.cloneBrowser();
            br2.setFollowRedirects(true);
            try {
                con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1 || con.getResponseCode() == 403) {
                    link.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                link.setProperty(property, Property.NULL);
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

    private void doFree(final DownloadLink link, final String directlinkproperty) throws Exception {
        String passCode = link.getDownloadPassword();
        dllink = checkDirectLink(link, directlinkproperty);
        if (dllink == null) {
            final String fsid = link.getStringProperty(PROPERTY_FSID);
            String sign;
            String tsamp;
            /* Needed to get the pcsett cookie on http://.pcs.baidu.com/ to avoid "hotlinking forbidden" errormessage later */
            getPage(this.br, "https://pcs.baidu.com/rest/2.0/pcs/file?method=plantcookie&type=ett");
            final String original_url = this.getPluginContentURL(link);
            final String shareid = link.getStringProperty("origurl_shareid");
            final String uk = link.getStringProperty("origurl_uk");
            final String link_password = link.getDownloadPassword();
            final String link_password_cookie = link.getStringProperty(PROPERTY_PASSWORD_COOKIE);
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
                        code = getCaptchaCode(captchaLink, link);
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
                handlePluginBroken(link, "unknownerror112", 3);
            } else if (br2.containsHTML("\"errno\":118")) {
                logger.warning("It seems like one or multiple parameters are missing in the previous request(s)");
                handlePluginBroken(link, "unknownerror118", 3);
            } else if (br2.containsHTML("No htmlCode read")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error_dllink", 5 * 60 * 1000l);
            }
            dllink = PluginJSonUtils.getJsonValue(br2, "dlink");
            if (dllink == null && accountOnly) {
                /* DLLINK null and not logged in but download is only possible via account */
                throw new AccountRequiredException();
            } else if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        final int maxchunks;
        if (this.getPluginConfig().getBooleanProperty("ALLOW_UNLIMITED_CHUNKS", false)) {
            maxchunks = 0;
        } else {
            maxchunks = FREE_MAXCHUNKS;
        }
        br.getHeaders().put("Accept-Encoding", "identity");
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, FREE_RESUME, maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
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
        link.setFinalFileName(Encoding.htmlDecode(getFileNameFromConnection(dl.getConnection())));
        if (passCode != null) {
            link.setDownloadPassword(passCode);
        }
        link.setProperty(directlinkproperty, dl.getConnection().getURL().toExternalForm());
        dl.startDownload();
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

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            if (true) {
                /* Login broken since 2018, see: https://board.jdownloader.org/showthread.php?t=78948 */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
    }

    private boolean isLoggedInHtml(final Browser br) throws Exception {
        return br.containsHTML("TODO_BROKEN_SINCE_2018");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        login(account, true);
        final AccountInfo ai = new AccountInfo();
        /* 2016-04-21: So far all accounts are handled as free accounts - free does not have any limits anyways! */
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        doFree(link, "account_free_directlink");
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return true;
    }

    @Override
    public boolean canHandle(final DownloadLink downloadLink, final Account account) throws Exception {
        if (accountOnly && account == null) {
            /* without account its not possible to download any link for this host */
            return false;
        } else {
            return true;
        }
    }
}
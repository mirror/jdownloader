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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.appwork.storage.JSonStorage;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.net.httpconnection.HTTPConnection.RequestMethod;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "fshare.vn" }, urls = { "https?://(?:www\\.)?(?:mega\\.1280\\.com|fshare\\.vn)/file/([0-9A-Z]+)" })
public class FShareVn extends PluginForHost {
    private final String         SERVERERROR                           = "Tài nguyên bạn yêu cầu không tìm thấy";
    private final String         IPBLOCKED                             = "<li>Tài khoản của bạn thuộc GUEST nên chỉ tải xuống";
    private String               dllink                                = null;
    /* Connection stuff */
    private static final boolean FREE_RESUME                           = false;
    private static final int     FREE_MAXCHUNKS                        = 1;
    private static final int     FREE_MAXDOWNLOADS                     = 1;
    // private static final boolean ACCOUNT_FREE_RESUME = false;
    // private static final int ACCOUNT_FREE_MAXCHUNKS = 1;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS             = 1;
    private static final boolean ACCOUNT_PREMIUM_RESUME                = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS             = -3;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS          = -1;
    /** 2019-08-29: Disabled API as a workaround. TODO: Fix issues and enable it again! */
    /** We can use mobile API for different things */
    /**
     * 2019-09-16: API is disabled for now as it only returns error 400 and 500 for us. As far as we could find out this is because their
     * API(-key) is limited to http/2 while we use http/1.
     */
    private static AtomicBoolean USE_API                               = new AtomicBoolean(false);
    private static final boolean use_api_for_premium_account_downloads = true;
    /** 2019-05-08: API works for free- and premium accounts! */
    private static final boolean use_api_for_free_account_downloads    = true;
    /** 2019-07-16: From now on we are able to get account information via API! */
    private static final boolean use_api_for_login_fetch_account_info  = true;

    public FShareVn(PluginWrapper wrapper) {
        super(wrapper);
        setStartIntervall(2000l);
        Browser.setRequestIntervalLimitGlobal(getHost(), 500);
        this.enablePremium("https://www.fshare.vn/payment/package/?type=vip");
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("mega.1280.com", "fshare.vn").replace("http://", "https://"));
        if (link.getSetLinkID() == null) {
            final String uid = getUID(link);
            if (uid != null) {
                link.setLinkID(this.getHost() + "://" + uid);
            }
        }
    }

    private String getUID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public String rewriteHost(String host) {
        if ("mega.1280.com".equals(host)) {
            return "fshare.vn";
        }
        return super.rewriteHost(host);
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        br = new Browser();
        this.setBrowserExclusive();
        correctDownloadLink(link);
        br.setFollowRedirects(false);
        // enforce english
        br.getHeaders().put("Referer", link.getDownloadURL());
        prepBrowserWebsite(this.br);
        String redirect = br.getRedirectLocation();
        if (redirect != null) {
            final boolean follows_redirects = br.isFollowingRedirects();
            URLConnectionAdapter con = null;
            br.setFollowRedirects(true);
            try {
                con = br.openHeadConnection(redirect);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    br.followConnection();
                    if (con.getRequestMethod() == RequestMethod.HEAD) {
                        br.getPage(redirect);
                    }
                } else {
                    link.setName(getFileNameFromHeader(con));
                    link.setVerifiedFileSize(con.getLongContentLength());
                    // lets also set dllink
                    dllink = br.getURL();
                    return AvailableStatus.TRUE;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
                br.setFollowRedirects(follows_redirects);
            }
        }
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("content=\"Error 404\"|<title>Not Found - Fshare<|<title>Fshare \\– Dịch vụ chia sẻ số 1 Việt Nam \\– Cần là có \\- </title>|b>Liên kết bạn chọn không tồn tại trên hệ thống Fshare</|<li>Liên kết không chính xác, hãy kiểm tra lại|<li>Liên kết bị xóa bởi người sở hữu\\.<|>\\s*Your requested file does not existed\\.\\s*<|>The file has been deleted by the user\\.<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("file\" title=\"(.*?)\">").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<p><b>Tên file:</b> (.*?)</p>").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("<i class=\"fa fa\\-file[^\"]*?\"></i>\\s*(.*?)\\s*</div>").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("<title>(?:Fshare - )?(.*?)(?: - Fshare)?</title>").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("<i class=\"fa fa\\-file\\-o\"></i>\\s*(.*?)\\s*</div>").getMatch(0);
        }
        String filesize = br.getRegex("<i class=\"fa fa-hdd-o\"></i>\\s*(.*?)\\s*</div>").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex(">\\s*([\\d\\.]+ [K|M|G]B)\\s*<").getMatch(0);
        }
        if (filename != null) {
            /* Server sometimes sends bad filenames */
            link.setFinalFileName(Encoding.htmlDecode(filename));
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    /*
     * Use this also to verify login-token. If everything works as designed, we will only have to do 2 API-calls until downloadstart!
     */
    public AvailableStatus requestFileInformationAPI(final DownloadLink link, final Account account) throws Exception {
        this.setBrowserExclusive();
        final String token = this.loginAPI(account, false);
        prepBrowserAPI(br);
        final PostRequest filecheckReq = br.createJSonPostRequest("https://" + getAPIHost() + "/api/fileops/get", String.format("{\"token\":\"%s\",\"url\":\"%s\"}", token, link.getDownloadURL()));
        br.openRequestConnection(filecheckReq);
        handleAPIResponseCodes();
        br.loadConnection(null);
        final String filename = PluginJSonUtils.getJson(br, "name");
        final String size = PluginJSonUtils.getJson(br, "size");
        final String deleted = PluginJSonUtils.getJson(br, "deleted");
        /* 2019-05-08: This is NOT a hash we can use for anything! */
        // final String hash = PluginJSonUtils.getJson(br, "hash_index");
        if ("1".equals(deleted)) {
            /* This should never happen (should return 404 for dead URLs) */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (!StringUtils.isEmpty(filename)) {
            link.setFinalFileName(filename);
        }
        if (!StringUtils.isEmpty(size) && size.matches("\\d+")) {
            link.setDownloadSize(Long.parseLong(size));
        }
        return AvailableStatus.TRUE;
    }

    private void handleAPIResponseCodes() throws Exception {
        if (br.getHttpConnection().getResponseCode() == 201) {
            logger.info("session_id cookie invalid");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "session_id cookie invalid", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        } else if (br.getHttpConnection().getResponseCode() == 400) {
            logger.info("Seems like stored logintoken expired");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Login token invalid", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        } else if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    public void doFree(final DownloadLink downloadLink, final Account acc) throws Exception {
        if (dllink != null) {
            /* these are effectively premium links? */
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
            if (!dl.getConnection().getContentType().contains("html")) {
                dl.startDownload();
                return;
            } else {
                br.followConnection();
                dllink = null;
            }
        }
        final String directlinkproperty;
        if (acc != null) {
            directlinkproperty = "account_free_directlink";
        } else {
            directlinkproperty = "directlink";
        }
        dllink = this.checkDirectLink(downloadLink, directlinkproperty);
        if (dllink == null) {
            simulateBrowser();
            if (dllink == null) {
                if (br.containsHTML(IPBLOCKED)) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 2 * 60 * 60 * 1000l);
                }
                /* we want _csrf token */
                final String csrf = br.getRegex("_csrf-app\" value=\"([^<>\"]+)\"").getMatch(0);
                final Browser ajax = br.cloneBrowser();
                ajax.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                ajax.getHeaders().put("x-requested-with", "XMLHttpRequest");
                ajax.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                // final String postdata = "_csrf-app=" + csrf + "&DownloadForm%5Bpwd%5D=&DownloadForm%5Blinkcode%5D=" +
                // getUID(downloadLink) + "&ajax=download-form&undefined=undefined";
                // ajax.postPage("/download/get", postdata);
                Form form = br.getFormbyAction("/download/get");
                for (int i = 1; i < 3; i++) {
                    ajax.submitForm(form);
                    if (ajax.containsHTML("url")) {
                        break;
                    }
                    if (ajax.containsHTML("Too many download sessions") || ajax.containsHTML("Quá nhiều phiên tải")) {
                        sleep(10 * 1001l, downloadLink);
                    }
                }
                if (ajax.containsHTML("Too many download sessions") || ajax.containsHTML("Quá nhiều phiên tải")) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Too many download sessions", 3 * 60 * 1000l);
                } else if (ajax.containsHTML("\"errors\":")) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, ajax.toString(), 3 * 60 * 1000l);
                }
                if (StringUtils.containsIgnoreCase(PluginJSonUtils.getJsonValue(ajax, "msg"), "Server error") && StringUtils.containsIgnoreCase(PluginJSonUtils.getJsonValue(ajax, "msg"), "please try again later")) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
                }
                dllink = PluginJSonUtils.getJsonValue(ajax, "url");
                if (dllink != null && br.containsHTML(IPBLOCKED) || ajax.containsHTML(IPBLOCKED)) {
                    final String nextDl = br.getRegex("LÆ°á»£t táº£i xuá»‘ng káº¿ tiáº¿p lÃ : ([^<>]+)<").getMatch(0);
                    logger.info("Next download: " + nextDl);
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 2 * 60 * 60 * 1000l);
                }
                if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                    final Recaptcha rc = new Recaptcha(br, this);
                    rc.parse();
                    rc.load();
                    File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    String c = getCaptchaCode("recaptcha", cf, downloadLink);
                    rc.setCode(c);
                    if (br.containsHTML("frm_download")) {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                }
                if (dllink == null) {
                    dllink = br.getRegex("window\\.location='(.*?)'").getMatch(0);
                    if (dllink == null) {
                        dllink = br.getRegex("value=\"Download\" name=\"btn_download\" value=\"Download\"  onclick=\"window\\.location='(http://.*?)'\"").getMatch(0);
                        if (dllink == null) {
                            dllink = br.getRegex("<form action=\"(http://download[^\\.]+\\.fshare\\.vn/download/[^<>]+/.*?)\"").getMatch(0);
                        }
                    }
                }
                logger.info("downloadURL = " + dllink);
                // Waittime
                String wait = PluginJSonUtils.getJsonValue(ajax, "wait_time");
                if (wait == null) {
                    br.getRegex("var count = \"(\\d+)\";").getMatch(0);
                    if (wait == null) {
                        wait = br.getRegex("var count = (\\d+);").getMatch(0);
                        if (wait == null) {
                            wait = "35";
                            // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                    }
                }
                // No downloadlink shown, host is buggy
                if (dllink == null && "0".equals(wait)) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
                }
                if (wait == null || dllink == null || !dllink.contains("fshare.vn")) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                sleep(Long.parseLong(wait) * 1001l, downloadLink);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, FREE_RESUME, FREE_MAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML(SERVERERROR)) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.fsharevn.Servererror", "Servererror!"), 60 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(directlinkproperty, dllink);
        dl.startDownload();
    }

    /** Sets required headers and required language */
    public static void prepBrowserWebsite(final Browser br) throws IOException {
        /* Sometime the page is extremely slow! */
        br.setReadTimeout(120 * 1000);
        br.getHeaders().put("User-Agent", jd.plugins.hoster.MediafireCom.stringUserAgent());
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        br.getHeaders().put("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        br.getHeaders().put("Accept-Encoding", "gzip, deflate");
        br.setCustomCharset("utf-8");
        br.getPage("https://www.fshare.vn/site/location?lang=vi"); // en - English version is having problems in version 3
    }

    /** Sets required headers */
    public static void prepBrowserAPI(final Browser br) throws IOException {
        /* Sometime the page is extremely slow! */
        br.setReadTimeout(120 * 1000);
        br.setAllowedResponseCodes(new int[] { 201, 400, 500 });
        /*
         * 2019-08-29: Do not use this User-Agent anymore as their API will return error 407 then! Keep in mind that unsupported User-Agents
         * might also lead to errorcodes 403 or 407!
         */
        // br.getHeaders().put("User-Agent", "okhttp/3.6.0");
    }

    @Override
    public String getAGBLink() {
        return "http://www.fshare.vn/policy.php?action=sudung";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_PREMIUM_MAXDOWNLOADS;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, null);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        if (account.getType() == AccountType.FREE) {
            if (USE_API.get() && use_api_for_free_account_downloads) {
                logger.info("Free account API download");
                dllink = this.getDllinkAPI(link, account);
            } else {
                requestFileInformation(link);
                if (dllink == null) {
                    logger.info("Free account website download");
                    loginWebsite(account, false);
                    br.getPage(link.getDownloadURL());
                    dllink = br.getRedirectLocation();
                } else {
                    logger.info("Free account download: Not logging in because user tries to download a premium-direct-download-url via free account");
                }
            }
            doFree(link, account);
        } else {
            final String directlinkproperty = "directlink_account";
            dllink = this.checkDirectLink(link, directlinkproperty);
            if (dllink == null) {
                dllink = getDllinkPremium(link, account);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                if (br.containsHTML(SERVERERROR)) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.fsharevn.Servererror", "Servererror!"), 60 * 60 * 1000l);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    public String getDllinkPremium(final DownloadLink link, final Account account) throws Exception {
        // we get page again, because we do not take directlink from requestfileinfo.
        final String dllink;
        if (USE_API.get() && use_api_for_premium_account_downloads) {
            dllink = getDllinkAPI(link, account);
        } else {
            dllink = getDllinkPremiumWebsite(link, account);
        }
        if (StringUtils.isEmpty(dllink) || !dllink.startsWith("http")) {
            logger.warning("dllink is null");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (StringUtils.containsIgnoreCase(dllink, "Server error") && StringUtils.containsIgnoreCase(dllink, "please try again later")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
        } else if (dllink.contains("logout")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "FATAL premium error");
        }
        return dllink;
    }

    /** Retrieves downloadurl via API which is also used in their mobile apps. */
    private String getDllinkAPI(final DownloadLink link, final Account account) throws Exception {
        /* Ensure that we're logged-in! */
        try {
            requestFileInformationAPI(link, account);
        } catch (final PluginException e) {
            /* Try to recognize old token here and then force it into new login attempt */
            if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                this.loginAPI(account, true);
            } else {
                e.printStackTrace();
                throw e;
            }
        }
        final String token = getAPITokenAndSetCookies(account);
        if (StringUtils.isEmpty(token)) {
            /* Login failure? This should never happen! */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Login token missing", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
        /*
         * 2019-08-29: Seems like other User-Agends are allowed for all other requests but if we do not use this one for this request, we
         * will likely get response 407!
         */
        br.getHeaders().put("User-Agent", "okhttp/3.6.0");
        /**
         * Every login via this method invalidates all previously generated tokens! <br>
         * 2019-08-29: Seems like tokens are valid longer and a download-request does NOT invalidate them!!
         */
        final PostRequest downloadReq = br.createJSonPostRequest("https://" + getAPIHost() + "/api/session/download", String.format("{\"token\":\"%s\",\"url\":\"%s\"}", token, link.getPluginPatternMatcher()));
        br.openRequestConnection(downloadReq);
        handleAPIResponseCodes();
        br.loadConnection(null);
        return PluginJSonUtils.getJson(br, "location");
    }

    /** Retrieves downloadurl via website. */
    private String getDllinkPremiumWebsite(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        loginWebsite(account, false);
        br.getPage(link.getPluginPatternMatcher());
        String dllink = br.getRedirectLocation();
        final String uid = getUID(link);
        if (dllink != null && dllink.endsWith("/file/" + uid)) {
            br.getPage(dllink);
            if (br.containsHTML("Your account is being used from another device")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Your account is being used from another device", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            dllink = br.getRedirectLocation();
        }
        if (dllink == null || dllink.matches(".+/file/.+\\?token=\\d+")) {
            if (br.containsHTML(">\\s*Fshare suspect this account has been stolen or is being used by other people\\.|Please press “confirm” to get a verification code, it’s sent to your email address\\.<")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Account determined as stolen or shared...", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            dllink = br.getRegex("\"(https?://[a-z0-9]+\\.fshare\\.vn/(vip|dl)/[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                /* Maybe user has disabled direct-download */
                final Browser ajax = br.cloneBrowser();
                ajax.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                ajax.getHeaders().put("x-requested-with", "XMLHttpRequest");
                ajax.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                for (int i = 1; i < 3; i++) {
                    final Form dlfast = br.getFormbyAction("/download/get");
                    if (dlfast == null) {
                        break;
                    }
                    /* Fix form */
                    if (!dlfast.hasInputFieldByName("ajax")) {
                        dlfast.put("ajax", "download-form");
                    }
                    if (!dlfast.hasInputFieldByName("undefined")) {
                        dlfast.put("undefined", "undefined");
                    }
                    dlfast.remove("DownloadForm%5Bpwd%5D");
                    dlfast.put("DownloadForm[pwd]", "");
                    dlfast.put("fcode5", "");
                    ajax.submitForm(dlfast);
                    if (ajax.containsHTML("Too many download sessions") || ajax.containsHTML("Quá nhiều phiên tải")) {
                        sleep(10 * 1001l, link);
                        continue;
                    }
                    dllink = PluginJSonUtils.getJsonValue(ajax, "url");
                }
                if (ajax.containsHTML("Too many download sessions") || ajax.containsHTML("Quá nhiều phiên tải")) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Too many download sessions", 3 * 60 * 1000l);
                } else if (ajax.containsHTML("\"errors\":")) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown error occured", 3 * 60 * 1000l);
                }
                if (dllink == null) {
                    final String msg = PluginJSonUtils.getJsonValue(ajax, "msg");
                    if (StringUtils.containsIgnoreCase(msg, "try again")) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, msg, 5 * 60 * 1000l);
                    }
                }
            }
        }
        return dllink;
    }

    /** Returns APIToken and sets cookies if both is available. */
    private String getAPITokenAndSetCookies(final Account account) {
        final String token = account.getStringProperty("token", null);
        if (token != null) {
            final Cookies cookies = account.loadCookies("apicookies");
            if (cookies != null) {
                br.setCookies(getAPIHost(), cookies);
            }
        }
        return token;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return false;
    }

    private String getAPIHost() {
        return "api.fshare.vn";
    }

    private boolean isLoggedinWebsite() {
        return br.containsHTML("class =\"user__profile\"") && br.getCookie(br.getHost(), "fshare-app", Cookies.NOTDELETEDPATTERN) != null;
    }

    private void loginWebsite(Account account, boolean force) throws Exception {
        synchronized (account) {
            try {
                prepBrowserWebsite(this.br);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    br.setCookies(this.getHost(), cookies);
                    br.getPage("https://www." + this.getHost() + "/file/manager");
                    if (isLoggedinWebsite()) {
                        account.saveCookies(br.getCookies(this.getHost()), "");
                        return;
                    }
                    br.clearCookies(br.getHost());
                }
                final boolean isFollowingRedirects = br.isFollowingRedirects();
                br.setFollowRedirects(true);
                br.getHeaders().put("Referer", "https://www.fshare.vn/site/login");
                br.getPage("https://www.fshare.vn"); // 503 with /site/location?lang=en
                final String csrf = br.getRegex("name=\"_csrf-app\" value=\"([^<>\"]+)\"").getMatch(0);
                final String cookie_fshare_app_old = br.getCookie(br.getHost(), "fshare-app", Cookies.NOTDELETEDPATTERN);
                if (csrf == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else if (cookie_fshare_app_old == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                br.setFollowRedirects(false);
                /*
                 * 2018-02-06: Do NOT use the long session cookies as that could cause "Too many sessions"(or similar) error when trying to
                 * start downloads!
                 */
                br.postPage("/site/login", "_csrf-app=" + csrf + "&LoginForm%5Bemail%5D=" + Encoding.urlEncode(account.getUser()) + "&LoginForm%5Bpassword%5D=" + Encoding.urlEncode(account.getPass()) + "&LoginForm%5BrememberMe%5D=0");
                br.followRedirect();
                if (br.containsHTML("Tài khoản của quý khách hiện đang đăng nhập trên nhiều thiết")) {
                    // Tài khoản của quý khách hiện đang đăng nhập trên nhiều thiết bị và trình duyệt.Quý khách vui lòng đăng xuất trên các
                    // thiết bị hoặc trình duyệt trước đó và tiến hành đăng nhập lại.
                    throw new AccountUnavailableException("Your account is currently logged on multiple devices and browsers. Please log out on the device or browser beforehand and proceed to log in again.", 30 * 60 * 1000l);
                }
                final String cookie_fshare_app_new = br.getCookie(br.getHost(), "fshare-app", Cookies.NOTDELETEDPATTERN);
                if (cookie_fshare_app_new == null || StringUtils.equalsIgnoreCase(cookie_fshare_app_new, cookie_fshare_app_old)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                if (br.getURL().contains("/resend")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nYour account is not activated yet. Confirm the activation mail to use it.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(br.getCookies(this.getHost()), "");
                br.setFollowRedirects(isFollowingRedirects);
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    /**
     * Login via API which is also used by their mobile app. <br />
     * Biggest issue when using this API: We cannot get the account information.<br />
     * Thx to: https://github.com/tudoanh/get_fshare/blob/master/get_fshare/get_fshare.py
     *
     * @return
     */
    private String loginAPI(final Account account, final boolean verifyCookies) throws Exception {
        synchronized (account) {
            try {
                prepBrowserAPI(this.br);
                String token = this.getAPITokenAndSetCookies(account);
                final Cookies cookies = account.loadCookies("apicookies");
                boolean loggedIN = false;
                if (token != null && cookies != null) {
                    br.setCookies(getAPIHost(), cookies);
                    if (!verifyCookies && System.currentTimeMillis() - account.getCookiesTimeStamp("") <= 300000l) {
                        /* Trust young cookies if we're not forced to check them */
                        return token;
                    }
                    // br.setAllowedResponseCodes(new int[] { 409 });
                    // final PostRequest loginCheckReq2 = br.createJSonPostRequest("https://api.fshare.vn/api/session/upload",
                    // String.format("{\"token\":\"%s\",\"name\":\"20mo.dat\",\"path\":\"/Music\",\"secure\":true,\"size\":10000}", token));
                    // br.openRequestConnection(loginCheckReq2);
                    // br.loadConnection(null);
                    /*
                     * We do not have any official way to check whether that token is valid so we will use their filecheck-function with a
                     * dummy URL. 404 = token is VALID[returns correct offline-state for our dummy-URL], 400 = token is INVALID and full
                     * login is required!, 201 = token is valid but session_id (cookie) is wrong/expired --> Full login required
                     */
                    final boolean use_dummy_linkcheck_as_login_check = false;
                    if (use_dummy_linkcheck_as_login_check) {
                        /* Old */
                        final PostRequest loginCheckReq = br.createJSonPostRequest("https://" + getAPIHost() + "/api/fileops/get", String.format("{\"token\":\"%s\",\"url\":\"%s\"}", token, "https://www.fshare.vn/file/JDTESTJDJDJD"));
                        br.openRequestConnection(loginCheckReq);
                        if (br.getHttpConnection().getResponseCode() == 404) {
                            /*
                             * 404 = our non-existant dummy file is offline --> API responded with expected result which means we're
                             * loggedin!
                             */
                            loggedIN = true;
                        } else {
                            /* E.g. response 201 or 400 */
                            // br.loadConnection(null);
                            loggedIN = false;
                        }
                    } else {
                        /* 2019-08-29: New */
                        br.getPage("https://" + getAPIHost() + "/api/user/get");
                        /* 2019-08-29: E.g. failure: {"code":201,"msg":"Not logged in yet!"} */
                        loggedIN = br.getHttpConnection().isOK() && br.getHttpConnection().getResponseCode() != 201;
                    }
                    if (loggedIN) {
                        logger.info("Old login-token/cookie is valid");
                    } else {
                        logger.info("Old login-token/cookie is INVALID");
                    }
                }
                if (!loggedIN) {
                    br.clearCookies(getAPIHost());
                    logger.info("Performing full login");
                    final Map<String, Object> map = new HashMap<String, Object>();
                    map.put("user_email", account.getUser());
                    map.put("password", account.getPass());
                    map.put("app_key", "L2S7R6ZMagggC5wWkQhX2+aDi467PPuftWUMRFSn");
                    final PostRequest loginReq = br.createJSonPostRequest("https://" + getAPIHost() + "/api/user/login", JSonStorage.toString(map));
                    br.getPage(loginReq);
                    // final String code = PluginJSonUtils.getJson(br, "code");
                    token = PluginJSonUtils.getJson(br, "token");
                    final String session_id = PluginJSonUtils.getJson(br, "session_id");
                    if (StringUtils.isEmpty(token) || StringUtils.isEmpty(session_id) || br.getHttpConnection().getResponseCode() == 400) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    /* Same key as in browser (website-version) but not usable for website-sessions! */
                    br.setCookie(this.getHost(), "session_id", session_id);
                    account.setProperty("token", token);
                }
                account.saveCookies(br.getCookies(getAPIHost()), "apicookies");
                return token;
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("apicookies");
                    account.removeProperty("token");
                }
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        if (USE_API.get() && use_api_for_login_fetch_account_info) {
            return fetchAccountInfoAPI(account);
        } else {
            return fetchAccountInfoWebsite(account);
        }
    }

    public AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        this.loginWebsite(account, true);
        final AccountInfo ai = new AccountInfo();
        br.getPage("https://www." + this.getHost() + "/account/profile");
        final String validUntil = br.getRegex("(?:Expire|Hạn dùng):</a>\\s*<span.*?>([^<>]*?)</span>").getMatch(0); // Version 3 (2018)
        final String accountType = br.getRegex("(?:Account type|Loại tài khoản)</a>\\s*?<span>([^<>\"]+)</span>").getMatch(0);
        if (StringUtils.equalsIgnoreCase(accountType, "VIP")) {
            if (validUntil != null) {
                long validuntil = 0;
                if (validUntil.contains("-")) {
                    validuntil = TimeFormatter.getMilliSeconds(validUntil, "dd-MM-yyyy", Locale.ENGLISH);
                } else {
                    validuntil = TimeFormatter.getMilliSeconds(validUntil, "dd/MM/yyyy", Locale.ENGLISH);
                }
                if (validuntil > 0) {
                    validuntil += 24 * 60 * 60 * 1000l;
                }
                ai.setValidUntil(validuntil, br, "EEE, dd MMM yyyy HH:mm:ss z");
            }
            if (ai.isExpired()) {
                account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
                account.setConcurrentUsePossible(true);
                ai.setStatus("Free (expired Premium)Account");
                account.setType(AccountType.FREE);
            } else {
                ai.setStatus("VIP Account");
                account.setType(AccountType.PREMIUM);
                account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
                account.setConcurrentUsePossible(true);
            }
        } else if (StringUtils.equalsIgnoreCase("Bundle", accountType)) {
            /* This is a kind of account that they give to their ADSL2+/FTTH service users. It works like VIP. */
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setConcurrentUsePossible(true);
            ai.setStatus("Bundle Account");
            account.setType(AccountType.PREMIUM);
        } else if (validUntil != null) {
            long validuntil = 0;
            if (validUntil.contains("-")) {
                validuntil = TimeFormatter.getMilliSeconds(validUntil, "dd-MM-yyyy", Locale.ENGLISH);
            } else {
                validuntil = TimeFormatter.getMilliSeconds(validUntil, "dd/MM/yyyy", Locale.ENGLISH);
            }
            if (validuntil > 0) {
                validuntil += 24 * 60 * 60 * 1000l;
            }
            ai.setValidUntil(validuntil, br, "EEE, dd MMM yyyy HH:mm:ss z");
            if (ai.isExpired()) {
                account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
                account.setConcurrentUsePossible(true);
                ai.setStatus("Free (expired Premium)Account");
                account.setType(AccountType.FREE);
            } else {
                account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
                account.setConcurrentUsePossible(true);
                ai.setStatus("Premium Account");
                account.setType(AccountType.PREMIUM);
            }
        } else {
            account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
            account.setConcurrentUsePossible(true);
            ai.setStatus("Free Account");
            account.setType(AccountType.FREE);
        }
        return ai;
    }

    public AccountInfo fetchAccountInfoAPI(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        this.loginAPI(account, true);
        if (br.getURL() == null || !br.getURL().contains("/api/user/get")) {
            br.getPage("https://" + getAPIHost() + "/api/user/get");
        }
        long validuntil = 0;
        final String validuntilStr = PluginJSonUtils.getJson(br, "expire_vip");
        /* 2019-07-16: Website does not display any traffic related information so we'll leave this out for now! */
        // final String trafficStr = PluginJSonUtils.getJson(br, "traffic");
        // final String traffic_usedStr = PluginJSonUtils.getJson(br, "traffic_used");
        // final String account_type = PluginJSonUtils.getJson(br, "account_type");
        final String webspace_usedStr = PluginJSonUtils.getJson(br, "webspace_used");
        if (!StringUtils.isEmpty(validuntilStr) && validuntilStr.matches("\\d+")) {
            validuntil = Long.parseLong(validuntilStr) * 1000;
        }
        if (!StringUtils.isEmpty(webspace_usedStr) && webspace_usedStr.matches("\\d+")) {
            ai.setUsedSpace(webspace_usedStr);
        }
        if (validuntil > System.currentTimeMillis()) {
            ai.setValidUntil(validuntil, br);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setConcurrentUsePossible(true);
            ai.setStatus("Premium Account");
            account.setType(AccountType.PREMIUM);
        } else {
            /** 2019-05-08: So far we cannot obtain any account information via API :( */
            /* As long as we cannot get any information via API, we'll simply treat every account as FREE-account. */
            account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
            account.setConcurrentUsePossible(true);
            if (validuntil > 0) {
                ai.setStatus("Free (expired Premium)Account");
            } else {
                ai.setStatus("Free Account");
            }
            account.setType(AccountType.FREE);
        }
        return ai;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (acc.getType() == AccountType.FREE) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }

    private LinkedHashSet<String> dupe = new LinkedHashSet<String>();

    /**
     * @author raztoki
     */
    private void simulateBrowser() throws InterruptedException {
        final AtomicInteger requestQ = new AtomicInteger(0);
        final AtomicInteger requestS = new AtomicInteger(0);
        final ArrayList<String> links = new ArrayList<String>();
        String[] l1 = br.getRegex("\\s+(?:src|href)=(\"|')(.*?)\\1").getColumn(1);
        if (l1 != null) {
            links.addAll(Arrays.asList(l1));
        }
        l1 = br.getRegex("\\s+(?:src|href)=(?!\"|')([^\\s]+)").getColumn(0);
        if (l1 != null) {
            links.addAll(Arrays.asList(l1));
        }
        for (final String link : links) {
            // lets only add links related to this hoster.
            final String correctedLink = Request.getLocation(link, br.getRequest());
            if (!"fshare.vn".equalsIgnoreCase(Browser.getHost(correctedLink))) {
                continue;
            }
            if (!correctedLink.contains(".png") && !correctedLink.contains(".js") && !correctedLink.contains(".css")) {
                continue;
            }
            if (dupe.add(correctedLink)) {
                final Thread simulate = new Thread("SimulateBrowser") {
                    public void run() {
                        final Browser rb = br.cloneBrowser();
                        rb.getHeaders().put("Cache-Control", null);
                        // open get connection for images, need to confirm
                        if (correctedLink.matches(".+\\.png.*")) {
                            rb.getHeaders().put("Accept", "image/webp,*/*;q=0.8");
                        }
                        if (correctedLink.matches(".+\\.js.*")) {
                            rb.getHeaders().put("Accept", "*/*");
                        } else if (correctedLink.matches(".+\\.css.*")) {
                            rb.getHeaders().put("Accept", "text/css,*/*;q=0.1");
                        }
                        URLConnectionAdapter con = null;
                        try {
                            requestQ.getAndIncrement();
                            con = rb.openGetConnection(correctedLink);
                        } catch (final Exception e) {
                        } finally {
                            try {
                                con.disconnect();
                            } catch (final Exception e) {
                            }
                            requestS.getAndIncrement();
                        }
                    }
                };
                simulate.start();
                Thread.sleep(100);
            }
        }
        while (requestQ.get() != requestS.get()) {
            Thread.sleep(1000);
        }
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return dllink;
    }
}
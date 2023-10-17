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
import org.appwork.storage.TypeRef;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.net.httpconnection.HTTPConnection.RequestMethod;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.components.config.FshareVnConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
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
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "fshare.vn" }, urls = { "https?://(?:www\\.)?(?:mega\\.1280\\.com|fshare\\.vn)/file/([0-9A-Z]+)" })
public class FShareVn extends PluginForHost {
    private final String         SERVERERROR                            = "Tài nguyên bạn yêu cầu không tìm thấy";
    private final String         IPBLOCKED                              = "<li>Tài khoản của bạn thuộc GUEST nên chỉ tải xuống";
    private String               dllink                                 = null;
    /* Connection stuff */
    private static final boolean FREE_RESUME                            = false;
    private static final int     FREE_MAXCHUNKS                         = 1;
    private static final int     FREE_MAXDOWNLOADS                      = 1;
    // private static final boolean ACCOUNT_FREE_RESUME = false;
    // private static final int ACCOUNT_FREE_MAXCHUNKS = 1;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS              = 1;
    private static final boolean ACCOUNT_PREMIUM_RESUME                 = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS              = -3;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS           = -1;
    /* 2021-07-09: Website account mode is broken && Deprecated! */
    private static AtomicBoolean USE_API_IN_ACCOUNT_MODE                = new AtomicBoolean(true);
    private static final boolean use_api_for_premium_account_downloads  = true;
    private static final boolean use_api_for_free_account_downloads     = true;
    private static final boolean use_api_for_login_fetch_account_info   = true;
    private static final String  PROPERTY_ACCOUNT_TOKEN                 = "token";
    private static final String  PROPERTY_ACCOUNT_COOKIES               = "apicookies";
    private static final String  PROPERTY_ACCOUNT_API_HELP_DIALOG_SHOWN = "api_help_dialog_shown";
    private static final String  apiCredentialsHelpPage                 = "https://support.jdownloader.org/Knowledgebase/Article/View/fshare-custom-api-credentials";
    private static final Object  apiCredentialsHelpDialogLock           = new Object();

    public FShareVn(PluginWrapper wrapper) {
        super(wrapper);
        Browser.setRequestIntervalLimitGlobal(getHost(), 500);
        this.enablePremium("https://www.fshare.vn/payment/package/?type=vip");
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.COOKIE_LOGIN_OPTIONAL };
    }

    public void correctDownloadLink(final DownloadLink link) {
        link.setPluginPatternMatcher(link.getPluginPatternMatcher().replace("mega.1280.com/", "fshare.vn/").replace("http://", "https://"));
        final String uid = getUID(link);
        if (uid != null) {
            link.setLinkID(this.getHost() + "://" + uid);
        }
    }

    private String getUID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        if (account != null && USE_API_IN_ACCOUNT_MODE.get()) {
            /* Prefer availablecheck via API */
            return this.requestFileInformationAPI(link, account);
        } else {
            return requestFileInformationWebsite(link);
        }
    }

    public AvailableStatus requestFileInformationWebsite(final DownloadLink link) throws IOException, PluginException {
        if (!link.isNameSet()) {
            link.setName(this.getUID(link));
        }
        this.setBrowserExclusive();
        correctDownloadLink(link);
        br.setFollowRedirects(false);
        br.getHeaders().put("Referer", link.getPluginPatternMatcher());
        prepBrowserWebsite(this.br);
        String redirect = br.getRedirectLocation();
        if (redirect != null) {
            final boolean follows_redirects = br.isFollowingRedirects();
            URLConnectionAdapter con = null;
            br.setFollowRedirects(true);
            try {
                con = br.openHeadConnection(redirect);
                if (!looksLikeDownloadableContent(con)) {
                    br.followConnection(true);
                    if (con.getRequestMethod() == RequestMethod.HEAD) {
                        br.getPage(redirect);
                    }
                } else {
                    /* Directurl */
                    link.setName(getFileNameFromHeader(con));
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    dllink = con.getURL().toString();
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
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("content=\"Error 404\"|<title>Not Found - Fshare<|<title>Fshare \\– Dịch vụ chia sẻ số 1 Việt Nam \\– Cần là có \\- </title>|b>Liên kết bạn chọn không tồn tại trên hệ thống Fshare</|<li>Liên kết không chính xác, hãy kiểm tra lại|<li>Liên kết bị xóa bởi người sở hữu\\.<|>\\s*Your requested file does not existed\\.\\s*<|>The file has been deleted by the user\\.<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("/images/Filenotfound")) {
            /* 2022-09-14 */
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
            filesize = br.getRegex("(?:>|\\|)\\s*([\\d\\.]+ [K|M|G]B)\\s*<").getMatch(0);
        }
        /* Filename/size is not available for password protected items via website (and always via API!). */
        final boolean isPasswordProtected = websiteGetPasswordProtectedForm(this.br) != null;
        if (isPasswordProtected) {
            link.setPasswordProtected(true);
        } else {
            link.setPasswordProtected(false);
            if (filename != null) {
                /* Server sometimes sends bad filenames */
                link.setFinalFileName(Encoding.htmlDecode(filename).trim());
            }
            if (filesize != null) {
                link.setDownloadSize(SizeFormatter.getSize(filesize));
            }
        }
        return AvailableStatus.TRUE;
    }

    public AvailableStatus requestFileInformationAPI(final DownloadLink link, final Account account) throws Exception {
        this.setBrowserExclusive();
        Map<String, Object> entries = null;
        AccountUnavailableException e = null;
        synchronized (account) {
            while (true) {
                final String token;
                try {
                    token = getAPITokenAndSetCookies(account, br);
                } catch (PluginException pe) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, null, pe);
                }
                final PostRequest filecheckReq = br.createJSonPostRequest("https://" + getAPIHost() + "/api/fileops/get", String.format("{\"token\":\"%s\",\"url\":\"%s\"}", token, link.getPluginPatternMatcher()));
                br.getPage(filecheckReq);
                try {
                    checkErrorsAPI(this.br, account, token);
                } catch (final AccountUnavailableException aue) {
                    logger.log(e);
                    if (e == null) {
                        e = aue;
                    } else {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, null, aue);
                    }
                }
                entries = restoreFromString(br.toString(), TypeRef.MAP);
                break;
            }
        }
        final String filename = entries.get("name").toString();
        final String description = (String) entries.get("description");
        final long deleted = JavaScriptEngineFactory.toLong(entries.get("deleted"), 1);
        final long size = JavaScriptEngineFactory.toLong(entries.get("size"), 0);
        final String pwd = (String) entries.get("pwd");
        /* 2019-05-08: This is NOT a hash we can use for anything! */
        // final String hash = PluginJSonUtils.getJson(br, "hash_index");
        if (!StringUtils.isEmpty(filename)) {
            link.setFinalFileName(filename);
        }
        if (size > 0) {
            link.setVerifiedFileSize(size);
        }
        if (description != null && link.getComment() == null) {
            link.setComment(entries.get("description").toString());
        }
        if (pwd != null) {
            link.setPasswordProtected(true);
        } else {
            link.setPasswordProtected(false);
        }
        if (deleted == 1) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    private void checkErrorsAPI(final Browser br, final Account account, final String token) throws Exception {
        /* 2021-07-09: File offline can redirect to html page so our json parser would fail --> Check for this first! */
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getHttpConnection().getResponseCode() == 500) {
            synchronized (apiCredentialsHelpDialogLock) {
                /**
                 * 2021-09-23: API credentials have been banned. This happened multiple times to our "official" JD credentials which is why
                 * we've added the ability for users to use their own API credentials.
                 */
                if (StringUtils.equalsIgnoreCase(PluginJsonConfig.get(getConfigInterface()).getApiAppKey(), "JDDEFAULT") && !account.hasProperty(PROPERTY_ACCOUNT_API_HELP_DIALOG_SHOWN)) {
                    /* Show extended error dialog to user in case default JD API credentials were used. */
                    account.setProperty(PROPERTY_ACCOUNT_API_HELP_DIALOG_SHOWN, true);
                    showAPICustomCredentialsRequiredInformation();
                }
                throw new AccountUnavailableException("Banned API credentials - read this: " + apiCredentialsHelpPage, 60 * 60 * 1000l);
            }
        }
        final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
        String msg = "Unknown error";
        if (entries.containsKey("msg")) {
            msg = entries.get("msg").toString();
        }
        /* First check for some text-based errors, handle the rest via response-code */
        if (isDownloadPasswordRequiredOrInvalid(msg)) {
            /*
             * Along with http response 403. Happens when trying to download a password protected URL and not providing any download
             * password.
             */
            throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
        }
        if (br.getHttpConnection().getResponseCode() == 201) {
            /* This should never happen at this stage! */
            logger.info("session_id cookie invalid");
            if (account != null) {
                synchronized (account) {
                    if (StringUtils.equals(token, getAPIToken(account))) {
                        logger.info("Clear invalid token!");
                        account.removeProperty(PROPERTY_ACCOUNT_TOKEN);
                        account.removeProperty(PROPERTY_ACCOUNT_COOKIES);
                    }
                }
            }
            throw new AccountUnavailableException("Session expired", 30 * 1000l);
        } else if (br.getHttpConnection().getResponseCode() == 400) {
            logger.info("Seems like stored logintoken expired");
            if (account != null) {
                synchronized (account) {
                    if (StringUtils.equals(token, getAPIToken(account))) {
                        logger.info("Clear invalid token!");
                        account.removeProperty(PROPERTY_ACCOUNT_TOKEN);
                    }
                }
            }
            throw new AccountUnavailableException("Login token invalid", 5 * 60 * 1000l);
        } else if (br.getHttpConnection().getResponseCode() == 403) {
            /* 2021-07-09: E.g. {"code":403,"msg":"Application for vip accounts only"} */
            throw new AccountInvalidException(msg);
        } else if (br.getHttpConnection().getResponseCode() == 404) {
            /* {"code":404,"msg":"T\u1eadp tin kh\u00f4ng t\u1ed3n t\u1ea1i"} */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getHttpConnection().getResponseCode() == 405) {
            /** Account invalid e.g. fail on initial login attempt: {"code":405,"msg":"Authenticate fail!"} */
            throw new AccountInvalidException(msg);
        } else if (br.getHttpConnection().getResponseCode() != 200) {
            /* Other errors e.g. 406 account locked, see: https://www.fshare.vn/api-doc#/Login%20-%20Logout%20-%20User%20info/login */
            throw new AccountInvalidException(msg);
        }
    }

    private Thread showAPICustomCredentialsRequiredInformation() {
        final Thread thread = new Thread() {
            public void run() {
                try {
                    final String title = "fshare.vn - custom API credentials required";
                    String message = "Hello dear fshare.vn user\r\n";
                    message += "It seems like our default API credentials are not working anymore.\r\n";
                    message += "Please open the following URL if this didn't already happen automatically and follow the instructions to be able to continue using your fshare.vn premium account in JD.\r\n";
                    message += apiCredentialsHelpPage;
                    final ConfirmDialog dialog = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, title, message);
                    dialog.setTimeout(5 * 60 * 1000);
                    if (CrossSystem.isOpenBrowserSupported() && !Application.isHeadless()) {
                        CrossSystem.openURL(apiCredentialsHelpPage);
                    }
                    final ConfirmDialogInterface ret = UIOManager.I().show(ConfirmDialogInterface.class, dialog);
                    ret.throwCloseExceptions();
                } catch (final Throwable e) {
                    getLogger().log(e);
                }
            };
        };
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private boolean isDownloadPasswordRequiredOrInvalid(final String msg) {
        if (msg != null && msg.equalsIgnoreCase("Please insert your password")) {
            return true;
        } else if (msg != null && msg.equalsIgnoreCase("Mật khẩu không đúng, vui lòng nhập lại")) {
            /* Invalid download password. */
            /* {"code":123,"msg":"M\u1eadt kh\u1ea9u kh\u00f4ng \u0111\u00fang, vui l\u00f2ng nh\u1eadp l\u1ea1i"} */
            return true;
        } else {
            return false;
        }
    }

    public void doFree(final DownloadLink link, final Account acc) throws Exception {
        if (dllink != null) {
            /* these are effectively premium links? */
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
            if (looksLikeDownloadableContent(dl.getConnection())) {
                dl.startDownload();
            }
            return;
        } else {
            br.followConnection(true);
            dllink = null;
        }
        final String directlinkproperty;
        if (acc != null) {
            directlinkproperty = "account_free_directlink";
        } else {
            directlinkproperty = "directlink";
        }
        dllink = this.checkDirectLink(link, directlinkproperty);
        if (dllink == null) {
            if (br.containsHTML(IPBLOCKED)) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 2 * 60 * 60 * 1000l);
            }
            /* 2021-08-03: Removed - not needed anymore?! */
            // simulateBrowser();
            final Browser ajax = br.cloneBrowser();
            ajax.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
            ajax.getHeaders().put("x-requested-with", "XMLHttpRequest");
            ajax.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            final Form passwordProtectedForm = websiteGetPasswordProtectedForm(this.br);
            if (passwordProtectedForm != null) {
                String passCode = link.getDownloadPassword();
                if (passCode == null) {
                    passCode = getUserInput("Password?", link);
                }
                passwordProtectedForm.put("DownloadPasswordForm%5Bpassword%5D", Encoding.urlEncode(passCode));
                br.submitForm(passwordProtectedForm);
                if (br.getHttpConnection().getResponseCode() == 201 && br.toString().length() < 100) {
                    /* Small workaround for empty page with http response 201, happens sometimes... */
                    br.getPage(br.getURL());
                }
                if (websiteGetPasswordProtectedForm(this.br) != null) {
                    link.setDownloadPassword(null);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
                } else {
                    link.setDownloadPassword(passCode);
                }
            }
            // final String postdata = "_csrf-app=" + csrf + "&DownloadForm%5Bpwd%5D=&DownloadForm%5Blinkcode%5D=" +
            // getUID(downloadLink) + "&ajax=download-form&undefined=undefined";
            // ajax.postPage("/download/get", postdata);
            Form form = br.getFormbyAction("/download/get");
            for (int i = 1; i < 3; i++) {
                ajax.submitForm(form);
                if (ajax.containsHTML("url")) {
                    break;
                }
                if (ajax.containsHTML("(?i)Too many download sessions") || ajax.containsHTML("(?i)Quá nhiều phiên tải")) {
                    sleep(10 * 1001l, link);
                }
            }
            if (ajax.containsHTML("(?i)Too many download sessions") || ajax.containsHTML("(?i)Quá nhiều phiên tải")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Too many download sessions", 3 * 60 * 1000l);
            } else if (ajax.getHttpConnection().getContentType().contains("application/json")) {
                final Map<String, Object> data = JavaScriptEngineFactory.jsonToJavaMap(ajax.toString());
                if (data.containsKey("errors")) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, ajax.toString(), 3 * 60 * 1000l);
                } else if (((Boolean) data.get("policydowload")) == Boolean.TRUE) {
                    throw new AccountRequiredException();
                }
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
            sleep(Long.parseLong(wait) * 1001l, link);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, FREE_RESUME, FREE_MAXCHUNKS);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (br.containsHTML(SERVERERROR)) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 10 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        link.setProperty(directlinkproperty, dllink);
        dl.startDownload();
    }

    private Form websiteGetPasswordProtectedForm(final Browser br) {
        return br.getFormbyProperty("class", "password-form");
    }

    /** Sets required headers and required language */
    public static Browser prepBrowserWebsite(final Browser br) throws IOException {
        /* Sometime the page is extremely slow! */
        br.setReadTimeout(120 * 1000);
        // br.getHeaders().put("User-Agent", jd.plugins.hoster.MediafireCom.stringUserAgent());
        /* 2021-07-02: Do not user random User-Agent anymore. */
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        br.getHeaders().put("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        br.getHeaders().put("Accept-Encoding", "gzip, deflate, br");
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(true);
        br.getPage("https://www.fshare.vn/site/location?lang=vi"); // en - English version is having problems in version 3
        return br;
    }

    /**
     * Sets required headers.
     *
     * @return
     */
    private Browser prepBrowserAPI(final Browser br) {
        /* Sometimes their API is extremely slow! */
        br.setReadTimeout(120 * 1000);
        br.setAllowedResponseCodes(new int[] { 201, 400, 405, 406, 410, 424, 500 });
        /* Important! According to their API docs, API key ("App Key") is bound to User-Agent! */
        br.setHeader("User-Agent", getApiUserAgent());
        /* Important: Doing API calls with API URL in Referer will result in error response 400! */
        br.getHeaders().put("Referer", "https://fshare.vn/");
        return br;
    }

    private String getApiAppKey() {
        final String customApiAppKey = PluginJsonConfig.get(getConfigInterface()).getApiAppKey();
        if (customApiAppKey != null && !customApiAppKey.equalsIgnoreCase("JDDEFAULT")) {
            return customApiAppKey;
        } else {
            /* Default/fallback */
            return new String(HexFormatter.hexToByteArray("644D6E714D4D5A4D556E4E355970764B454E614568645151356A784471646474"));
        }
    }

    private String getApiUserAgent() {
        final String customApiUserAgent = PluginJsonConfig.get(getConfigInterface()).getApiUserAgent();
        if (customApiUserAgent != null && !customApiUserAgent.equalsIgnoreCase("JDDEFAULT")) {
            return customApiUserAgent;
        } else {
            /* Default/fallback */
            return "JDownloader-D0OCY1";
        }
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
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformationWebsite(link);
        doFree(link, null);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        if (account.getType() == AccountType.FREE) {
            if (USE_API_IN_ACCOUNT_MODE.get() && use_api_for_free_account_downloads) {
                logger.info("Free account API download");
                dllink = this.getDllinkAPI(link, account);
            } else {
                requestFileInformationWebsite(link);
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
            if (!attemptStoredDownloadurlDownload(link, directlinkproperty, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS)) {
                logger.info("Generating fresh directurl");
                dllink = getDllinkPremium(link, account);
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
                if (!looksLikeDownloadableContent(dl.getConnection())) {
                    br.followConnection(true);
                    if (br.containsHTML(SERVERERROR)) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 10 * 60 * 1000l);
                    } else if (br.containsHTML("(?i)>\\s*Quý khách đã vượt quá số lượt tải trong ngày")) {
                        /*
                         * Reached daily downloadlimit: <p class="content">Quý khách đã vượt quá số lượt tải trong ngày. Vui lòng đăng ký
                         * hoặc đăng nhập để nhận thêm lượt tải và nhiều ưu đãi khác </p>
                         */
                        throw new AccountUnavailableException("Daily downloadlimit reached", 5 * 60 * 1000l);
                    } else if (br.containsHTML("(?i)>\\s*Đã có lỗi xảy ra")) {
                        /* 2021-07-05 */
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown error occured");
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
                link.setProperty(directlinkproperty, this.dllink);
            } else {
                logger.info("Re-using stored directurl");
            }
            dl.startDownload();
        }
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link, final String directlinkproperty, final boolean resume, final int maxchunks) throws Exception {
        final String url = link.getStringProperty(directlinkproperty);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, resume, maxchunks);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                return true;
            } else {
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Throwable e) {
            logger.log(e);
            try {
                dl.getConnection().disconnect();
            } catch (Throwable ignore) {
            }
            return false;
        }
    }

    public String getDllinkPremium(final DownloadLink link, final Account account) throws Exception {
        // we get page again, because we do not take directlink from requestfileinfo.
        final String dllink;
        if (USE_API_IN_ACCOUNT_MODE.get() && use_api_for_premium_account_downloads) {
            dllink = getDllinkAPI(link, account);
            if (StringUtils.isEmpty(dllink)) {
                /* This should never happen! */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Failed to find final downloadurl");
            }
        } else {
            dllink = getDllinkPremiumWebsite(link, account);
            if (StringUtils.isEmpty(dllink) || !dllink.startsWith("http")) {
                logger.warning("dllink is null");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (StringUtils.containsIgnoreCase(dllink, "Server error") && StringUtils.containsIgnoreCase(dllink, "please try again later")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
            } else if (dllink.contains("logout")) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "FATAL premium error");
            }
        }
        return dllink;
    }

    /** https://www.fshare.vn/api-doc#/File%20manager/take-download-link */
    private String getDllinkAPI(final DownloadLink link, final Account account) throws Exception {
        /**
         * Login and check file info. 2021-07-09: file-check is not needed anymore: Download request will return http response 404 on
         * offline file!
         */
        // requestFileInformationAPI(link, account);
        AccountUnavailableException e = null;
        String passCode = link.getDownloadPassword();
        /* Ask for password right away if we know that an item is password protected --> Can save one http request */
        if (passCode == null && link.isPasswordProtected()) {
            passCode = getUserInput("Password?", link);
        }
        int loopCounter = 1;
        int passwordAttemptsCounter = 0;
        synchronized (account) {
            while (true) {
                logger.info("Download attempt number: " + loopCounter);
                /*
                 * 2021-08-03: Fresh browser instance with fresh headers for each loop else we may get error 400 (seemingly because of the
                 * "Referer" header).
                 */
                final Browser brc = prepBrowserAPI(new Browser());
                final String token = getAPITokenAndSetCookies(account, brc);
                final Map<String, Object> postdata = new HashMap<String, Object>();
                postdata.put("token", token);
                postdata.put("url", link.getPluginPatternMatcher());
                if (passCode != null) {
                    postdata.put("password", passCode);
                }
                final PostRequest downloadReq = brc.createJSonPostRequest("https://" + getAPIHost() + "/api/session/download", JSonStorage.serializeToJson(postdata));
                brc.getPage(downloadReq);
                final Map<String, Object> entries = restoreFromString(brc.toString(), TypeRef.MAP);
                if (isDownloadPasswordRequiredOrInvalid((String) entries.get("msg")) && passwordAttemptsCounter < 3) {
                    passCode = getUserInput("Password?", link);
                    passwordAttemptsCounter += 1;
                    link.setPasswordProtected(true);
                    continue;
                }
                /* if passwordAttemptsCounter is "too high", checkErrorsAPI will handle the "Wrong password" error just fine. */
                try {
                    checkErrorsAPI(brc, account, token);
                } catch (final AccountUnavailableException aue) {
                    logger.log(e);
                    if (e == null) {
                        e = aue;
                    } else {
                        throw aue;
                    }
                }
                if (passCode != null) {
                    link.setDownloadPassword(passCode);
                }
                return entries.get("location").toString();
            }
        }
    }

    /** Retrieves downloadurl via website. */
    @Deprecated
    private String getDllinkPremiumWebsite(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        loginWebsite(account, false);
        br.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        String dllink = null;
        try {
            con = br.openGetConnection(link.getPluginPatternMatcher());
            if (looksLikeDownloadableContent(con)) {
                logger.info("Found direct-URL");
                dllink = con.getURL().toString();
            } else {
                logger.info("Failed to find direct-URL --> Either user has disabled direct downloads in account or we have an error");
                br.followConnection();
                if (br.containsHTML("Your account is being used from another device")) {
                    throw new AccountUnavailableException("Your account is being used from another device", 5 * 60 * 1000l);
                } else if (br.containsHTML(">\\s*Fshare suspect this account has been stolen or is being used by other people\\.|Please press “confirm” to get a verification code, it’s sent to your email address\\.<")) {
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
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return dllink;
    }

    /** Returns APIToken and sets cookies if both is available. */
    private String getAPITokenAndSetCookies(final Account account, final Browser br) throws Exception {
        synchronized (account) {
            prepBrowserAPI(br);
            String token = getAPIToken(account);
            if (token == null) {
                token = loginAPI(account, br, true);
            }
            final Cookies cookies = account.loadCookies(PROPERTY_ACCOUNT_COOKIES);
            if (token != null && cookies != null) {
                br.setCookies(getAPIHost(), cookies);
                return token;
            } else {
                return null;
            }
        }
    }

    private String getAPIToken(final Account account) {
        synchronized (account) {
            return account.getStringProperty(PROPERTY_ACCOUNT_TOKEN);
        }
    }

    @Override
    public boolean hasAutoCaptcha() {
        return false;
    }

    private String getAPIHost() {
        return "api.fshare.vn";
    }

    @Deprecated
    private boolean isLoggedinWebsite() {
        return br.containsHTML("class =\"user__profile\"") && br.getCookie(br.getHost(), "fshare-app", Cookies.NOTDELETEDPATTERN) != null;
    }

    @Deprecated
    private void loginWebsite(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                prepBrowserWebsite(this.br);
                final Cookies cookies = account.loadCookies("");
                final Cookies userCookies = account.loadUserCookies();
                if (userCookies != null) {
                    if (!force) {
                        /* Don't verify cookies */
                        br.setCookies(userCookies);
                        return;
                    }
                    if (this.checkWebsiteCookies(account, userCookies)) {
                        return;
                    } else {
                        if (account.hasEverBeenValid()) {
                            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                        } else {
                            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                        }
                    }
                } else if (cookies != null) {
                    if (!force) {
                        /* Don't verify cookies */
                        br.setCookies(userCookies);
                        return;
                    }
                    if (this.checkWebsiteCookies(account, cookies)) {
                        account.saveCookies(br.getCookies(this.getHost()), "");
                        return;
                    }
                }
                logger.info("Performing full login");
                br.setFollowRedirects(true);
                br.getHeaders().put("Referer", "https://www." + this.getHost() + "/site/login");
                br.getPage("https://www." + this.getHost()); // 503 with /site/location?lang=en
                // br.getPage("/site/login");
                final String csrf = br.getRegex("name=\"_csrf-app\" value=\"([^<>\"]+)\"").getMatch(0);
                final String cookie_fshare_app_old = br.getCookie(br.getHost(), "fshare-app", Cookies.NOTDELETEDPATTERN);
                if (csrf == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else if (cookie_fshare_app_old == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                Form loginform = br.getFormbyProperty("id", "form-signup");
                if (loginform == null) {
                    logger.warning("Failed to find loginform");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("LoginForm%5Bemail%5D", Encoding.urlEncode(account.getUser()));
                loginform.put("LoginForm%5Bpassword%5D", Encoding.urlEncode(account.getPass()));
                loginform.remove("LoginForm%5BrememberMe%5D");
                loginform.remove("LoginForm%5BrememberMe%5D");
                /*
                 * 2018-02-06: Do NOT use the long session cookies as that could cause "Too many sessions"(or similar) error when trying to
                 * start downloads!
                 */
                loginform.put("LoginForm%5BrememberMe%5D", "0");
                /* 2021-07-02: New and necessary(?) */
                br.getHeaders().put("Origin", "https://www.fshare.vn");
                br.submitForm(loginform);
                if (br.containsHTML("Tài khoản của quý khách hiện đang đăng nhập trên nhiều thiết")) {
                    // Tài khoản của quý khách hiện đang đăng nhập trên nhiều thiết bị và trình duyệt.Quý khách vui lòng đăng xuất trên các
                    // thiết bị hoặc trình duyệt trước đó và tiến hành đăng nhập lại.
                    throw new AccountUnavailableException("Your account is currently logged on multiple devices and browsers. Please log out on the device or browser beforehand and proceed to log in again.", 5 * 60 * 1000l);
                }
                loginform = br.getFormbyProperty("id", "form-signup");
                if (loginform != null && loginform.hasInputFieldByName("LoginForm%5BverifyCode%5D")) {
                    /* Login captcha --> Same Form as before, will still contain mail & password but we need to fill in the captcha. */
                    logger.info("Handling login captcha");
                    final String captchaURL = loginform.getRegex("\"(/site/captchaV3[^<>\"\\']+)\"").getMatch(0);
                    if (captchaURL == null) {
                        logger.warning("Failed to find captchaURL");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    /* We do not want a long session cookie (see comment regarding first form we send!) */
                    loginform.remove("LoginForm%5BrememberMe%5D");
                    loginform.remove("LoginForm%5BrememberMe%5D");
                    loginform.put("LoginForm%5BrememberMe%5D", "0");
                    final DownloadLink dlinkbefore = this.getDownloadLink();
                    try {
                        if (dlinkbefore == null) {
                            this.setDownloadLink(new DownloadLink(this, "Account", this.getHost(), "http://" + account.getHoster(), true));
                        }
                        final String code = this.getCaptchaCode(captchaURL, this.getDownloadLink());
                        loginform.put("LoginForm%5BverifyCode%5D", Encoding.urlEncode(code));
                    } finally {
                        if (dlinkbefore != null) {
                            this.setDownloadLink(dlinkbefore);
                        }
                    }
                    br.submitForm(loginform);
                }
                /*
                 * 2020-03-18: 'You are logged in in many other browsers. Click on continue to dump all other sessions and confirm to use
                 * this one.' --> Without sending this form we will not be able to login!
                 */
                loginform = br.getFormbyProperty("id", "form-signup");
                if (loginform != null && br.containsHTML("class=\"img_noti_lock\"")) {
                    /* 2020-03-18: TODO: This does not work but I was not able to reproduce this anymore either :( */
                    /* We do not want a long session cookie (see comment regarding first form we send!) */
                    loginform.remove("LoginForm%5BrememberMe%5D");
                    loginform.remove("LoginForm%5BrememberMe%5D");
                    loginform.put("LoginForm%5BrememberMe%5D", "0");
                    final String remove_id = br.getRegex("name=\"remove_id\" value=\"(\\d+)\"").getMatch(0);
                    if (!loginform.hasInputFieldByName("remove_id") && remove_id != null) {
                        loginform.put("remove_id", remove_id);
                    }
                    br.submitForm(loginform);
                }
                final String cookie_fshare_app_new = br.getCookie(br.getHost(), "fshare-app", Cookies.NOTDELETEDPATTERN);
                if (cookie_fshare_app_new == null || StringUtils.equalsIgnoreCase(cookie_fshare_app_new, cookie_fshare_app_old)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if (br.getURL().contains("/resend")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nYour account is not activated yet. Confirm the activation mail to use it.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    @Deprecated
    private boolean checkWebsiteCookies(final Account account, final Cookies cookies) throws IOException {
        br.setCookies(cookies);
        br.getPage("https://www." + this.getHost() + "/account/profile");
        if (isLoggedinWebsite()) {
            logger.info("Cookie login successful");
            return true;
        } else {
            logger.info("Cookie login failed");
            this.br.clearAll();
            return false;
        }
    }

    /**
     * Login via API - docs: https://www.fshare.vn/api-doc
     */
    private String loginAPI(final Account account, final Browser br, final boolean verifyCookies) throws Exception {
        synchronized (account) {
            try {
                prepBrowserAPI(br);
                String token = getAPIToken(account);
                final Cookies cookies = account.loadCookies(PROPERTY_ACCOUNT_COOKIES);
                final String appKey = getApiAppKey();
                if (token != null && cookies != null) {
                    logger.info("Logging in via cookies");
                    br.setCookies(getAPIHost(), cookies);
                    if (!verifyCookies) {
                        /* Trust young cookies if we're not forced to check them */
                        logger.info("Trust cookies without check");
                        return token;
                    } else {
                        /* 2019-08-29: New */
                        br.getPage("https://" + getAPIHost() + "/api/user/get");
                        /* 2019-08-29: E.g. failure: {"code":201,"msg":"Not logged in yet!"} */
                        if (br.getHttpConnection().getResponseCode() == 201) {
                            logger.info("Cookie login failed");
                        } else {
                            logger.info("Cookie login looks good --> Checking for other errors");
                            checkErrorsAPI(br, account, null);
                            logger.info("Cookie login successful");
                            return token;
                        }
                    }
                }
                logger.info("Performing full login");
                /* Important: Clear Referer otherwise we may get error 400! */
                br.clearAll();
                prepBrowserAPI(br);
                final Map<String, Object> map = new HashMap<String, Object>();
                map.put("user_email", account.getUser());
                map.put("password", account.getPass());
                // map.put("app_key", "L2S7R6ZMagggC5wWkQhX2+aDi467PPuftWUMRFSn"); --> Their old public App API key
                map.put("app_key", appKey);
                final PostRequest loginReq = br.createJSonPostRequest("https://" + getAPIHost() + "/api/user/login", JSonStorage.toString(map));
                br.getPage(loginReq);
                checkErrorsAPI(br, account, null);
                final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
                token = (String) entries.get(PROPERTY_ACCOUNT_TOKEN);
                /*
                 * session_id is valid for 6 hours after last usage which means as long as the users' JD is always running, every default
                 * accountcheck will reset that expire-timer.
                 */
                final String sessionID = (String) entries.get("session_id");
                if (StringUtils.isEmpty(token) || StringUtils.isEmpty(sessionID)) {
                    /* This should never happen as the above errorhandling should cover all errors already */
                    throw new AccountUnavailableException("Unknown login failure", 5 * 60 * 1000l);
                }
                /* Same cookie key as in browser (website-version) but not usable for website-sessions! */
                br.setCookie(getAPIHost(), "session_id", sessionID);
                account.setProperty(PROPERTY_ACCOUNT_TOKEN, token);
                account.saveCookies(br.getCookies(getAPIHost()), PROPERTY_ACCOUNT_COOKIES);
                return token;
            } catch (final PluginException e) {
                /* Dump cookies on login failure */
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies(PROPERTY_ACCOUNT_COOKIES);
                    account.removeProperty(PROPERTY_ACCOUNT_TOKEN);
                }
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        if (USE_API_IN_ACCOUNT_MODE.get() && use_api_for_login_fetch_account_info) {
            return fetchAccountInfoAPI(account);
        } else {
            return fetchAccountInfoWebsite(account);
        }
    }

    @Deprecated
    public AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        this.loginWebsite(account, true);
        final AccountInfo ai = new AccountInfo();
        /* Only access this page if it has not been accessed before! */
        if (br.getURL() == null || !br.getURL().contains("/account/profile")) {
            /* Important: Clear Referer otherwise we may get error 400! */
            br.clearAll();
            prepBrowserAPI(br);
            br.getPage("https://www." + this.getHost() + "/account/profile");
        }
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
                // if (validuntil > 0) {
                // validuntil += 24 * 60 * 60 * 1000l;
                // }
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
        this.loginAPI(account, this.br, true);
        if (br.getURL() == null || !br.getURL().contains("/api/user/get")) {
            /* Important: Clear Referer otherwise we may get error 400! */
            prepBrowserAPI(br);
            br.getPage("https://" + getAPIHost() + "/api/user/get");
        }
        final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
        /**
         * 2019-07-16: Website does not display any traffic related information so we'll leave this out for now! All given values are 0.
         */
        // final String trafficStr = PluginJSonUtils.getJson(br, "traffic");
        // final String traffic_usedStr = PluginJSonUtils.getJson(br, "traffic_used");
        ai.setUsedSpace(JavaScriptEngineFactory.toLong(entries.get("webspace_used"), 0));
        ai.setCreateTime(JavaScriptEngineFactory.toLong(entries.get("joindate"), 0) * 1000);
        final String expire_vip = StringUtils.valueOfOrNull(entries.get("expire_vip"));
        final String account_type = StringUtils.valueOfOrNull(entries.get("account_type"));
        if ("Forever".equalsIgnoreCase(expire_vip) || "Forever".equalsIgnoreCase(account_type)) {
            ai.setValidUntil(-1);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setConcurrentUsePossible(true);
            /* 2021-07-09: E.g. "Vip" */
            ai.setStatus(account_type);
            account.setType(AccountType.PREMIUM);
        } else {
            final long validuntil = JavaScriptEngineFactory.toLong(expire_vip, 0) * 1000;
            if (validuntil > System.currentTimeMillis()) {
                ai.setValidUntil(validuntil, this.br);
                account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
                account.setConcurrentUsePossible(true);
                /* 2021-07-09: E.g. "Vip" */
                ai.setStatus(account_type);
                account.setType(AccountType.PREMIUM);
            } else {
                /* 2021-07-09: Free Accounts are not allowed to be used via API anymore so most likely this code won't ever be reached! */
                account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
                account.setConcurrentUsePossible(true);
                if (validuntil > 0) {
                    ai.setStatus("Free (expired Premium) Account");
                } else {
                    ai.setStatus("Free Account");
                }
                account.setType(AccountType.FREE);
            }
        }
        return ai;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        /* 2021-07-09 */
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            link.removeProperty("directlink_account");
        }
    }

    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        /* 2021-07-09: No captchas required at all */
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

    private String checkDirectLink(final DownloadLink link, final String property) {
        final String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openHeadConnection(dllink);
                if (!looksLikeDownloadableContent(con)) {
                    throw new IOException();
                } else {
                    return dllink;
                }
            } catch (final Exception e) {
                logger.log(e);
                link.setProperty(property, Property.NULL);
                return null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        } else {
            return null;
        }
    }

    @Override
    public Class<? extends FshareVnConfig> getConfigInterface() {
        return FshareVnConfig.class;
    }
}
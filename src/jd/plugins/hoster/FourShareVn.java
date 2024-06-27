//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import org.appwork.storage.JSonMapperException;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.Request;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.parser.html.InputField;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "4share.vn" }, urls = { "https?://(?:www\\.)?(?:up\\.)?4share\\.vn/f/([a-f0-9]{16})" })
public class FourShareVn extends PluginForHost {
    public FourShareVn(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://up.4share.vn/?act=gold");
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        /* 2024-06-26: Website is very slow */
        br.setReadTimeout(2 * 60 * 1000);
        br.setConnectTimeout(2 * 60 * 1000);
        return br;
    }

    private String getContentURL(final DownloadLink link) {
        return link.getPluginPatternMatcher().replaceFirst("up\\.4share\\.vn/", "4share.vn/");
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    public int getMaxChunks(final DownloadLink link, final Account account) {
        if (account != null && AccountType.PREMIUM.equals(account.getType())) {
            return 0;
        } else {
            return 1;
        }
    }

    @Override
    public String rewriteHost(String host) {
        if ("up.4share.vn".equals(host)) {
            return "4share.vn";
        }
        return super.rewriteHost(host);
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    /* API docs: https://4share.vn/documents/api/ */
    private final String  API_BASE                    = "https://api.4share.vn/api/v1/";
    private final String  PROPERTY_ACCOUNT_TOKEN      = "token";
    private final boolean USE_API_FOR_ACCOUNT_ACTIONS = true;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformationWebsite(link);
    }

    private AvailableStatus requestFileInformationWebsite(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.getPage(this.getContentURL(link));
        String filename = br.getRegex("<h1[^>]*>\\s*<strong>\\s*([^<>\"]+)\\s*</strong>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex(">\\s*Tên File\\s*:\\s*<strong>([^<>\"]*?)</strong>").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("<title>Download\\s*([^<>\"]+) \\| 4share\\.vn\\s*</title>").getMatch(0);
        }
        String filesize = br.getRegex(">\\s*Kích thước\\s*:\\s*<strong>\\s*(\\d+(?:\\.\\d+)?\\s*(?:B(?:yte)?|KB|MB|GB))\\s*</strong>").getMatch(0);
        if (filesize == null) {
            /* 2019-08-28 */
            filesize = br.getRegex("</h1>\\s*<strong>\\s*([^<>\"]+)\\s*</strong>").getMatch(0);
            if (filesize == null) {
                /* 2021-10-21 */
                filesize = br.getRegex("/strong>\\s*</h1>\\s*(\\d+[^<]+)<br/>").getMatch(0);
            }
        }
        if (filename != null) {
            link.setName(Encoding.htmlDecode(filename).trim());
        } else {
            logger.warning("Failed to find filename");
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        } else {
            logger.warning("Failed to find filesize");
        }
        final String md5 = br.getRegex("MD5\\s*:?\\s*([a-f0-9]{32})").getMatch(0);
        /* 2024-06-26: We currently can't handle crc32b, see https://svn.jdownloader.org/issues/90471 */
        // final String crc32b = br.getRegex("CRC32B\\s*:\\s*(\\d+)").getMatch(0);
        if (md5 != null) {
            link.setMD5Hash(md5);
        } else {
            logger.warning("Failed to find md5hash");
        }
        /* Website may still provide names of deleted files --> First check for file info, then check for offline status. */
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)>\\s*FID Không hợp lệ\\!|file not found|(F|f)ile (này)? đã bị xóa|File không tồn tại?| Error: FileLink da bi xoa|>Xin lỗi Bạn, file này không còn tồn tại|File suspended:") || !this.br.getURL().matches(".+[a-f0-9]{16}$")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)>\\s*File đã xóa?")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    /** https://4share.vn/documents/api/#api-File_and_Folder-Get_File_Info */
    private AvailableStatus requestFileInformationAPI(final DownloadLink link, final Account account) throws Exception {
        this.setBrowserExclusive();
        final String fid = this.getFID(link);
        if (!link.isNameSet()) {
            link.setName(fid);
        }
        final UrlQuery query = new UrlQuery();
        query.add("file_id", Encoding.urlEncode(fid));
        final PostRequest req = br.createPostRequest(API_BASE + "?cmd=get_file_info", query);
        final Map<String, Object> payload = (Map<String, Object>) this.callAPI(req, account, null);
        link.setFinalFileName(payload.get("name").toString());
        link.setVerifiedFileSize(((Number) payload.get("size")).longValue());
        /* 2024-06-26: Not sure about this. Could also mean a delete-date in the future. */
        // final Object delete_date = payload.get("delete_date");
        // if(delete_date != null) {
        // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // }
        return AvailableStatus.TRUE;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        if (USE_API_FOR_ACCOUNT_ACTIONS) {
            return fetchAccountInfoAPI(account);
        } else {
            return fetchAccountInfoWebsite(account);
        }
    }

    private AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        synchronized (account) {
            loginWebsite(account, true);
            br.getPage("/member");
            /*
             * TODO
             *
             * Ngày đăng ký: 2012-xx-xx 10:10:10 Ngày hết hạn: 2012-xx-xx 10:10:10 , còn 59 ngày sử dụng - Gold còn lại: 293 (293 - TKC + 0
             * - TKP ) Gold TKC - Tài khoản Chính, là loại gold nạp tiền trực tiếp; Gold TKP - Tài khoản Phụ, là loại Gold được thưởng Gold
             * đã dùng: 607 (607 + 0) Gold đã nạp: 900 (900 + ) Bạn đã download từ 4Share hôm nay : 91.01 GB [Tất cả: 1.34 TB]
             *
             * Feedback from customer: My account pays a monthly fee. Looks like there's a limit to it, I'm not sure how many GB it is. Gold
             * = Monthly renew
             */
            final String[] traffic = br.getRegex("<strong>\\s*([0-9\\.,]+ [GMKB]+)\\s*</strong>\\s*/Tổng số\\s*:\\s*<strong>\\s*([0-9\\.,]+ [GMKB]+)\\s*</strong>").getRow(0);
            if (traffic != null) {
                final long max = SizeFormatter.getSize(traffic[1]);
                final long left = SizeFormatter.getSize(traffic[0]);
                ai.setTrafficMax(max);
                ai.setTrafficLeft(left);
            } else {
                ai.setUnlimitedTraffic();
            }
            final String expire = br.getRegex("Ngày hết hạn:\\s*<b>\\s*(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})").getMatch(0);
            if (expire == null) {
                ai.setExpired(true);
                return ai;
            } else {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd hh:mm:ss", Locale.ENGLISH), br);
            }
            if (br.containsHTML("Gold TKC")) {
                ai.setStatus("Premium (Gold) User");
            } else {
                ai.setStatus("Premium (VIP) User");
            }
        }
        return ai;
    }

    /** https://4share.vn/documents/api/#api-AccessToken-GetUserInfomation */
    private AccountInfo fetchAccountInfoAPI(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        synchronized (account) {
            loginAPI(account, true);
            final PostRequest req = br.createPostRequest(API_BASE + "?cmd=get_user_info", new UrlQuery());
            final Map<String, Object> payload = (Map<String, Object>) this.callAPI(req, account, null);
            final String register_date = (String) payload.get("register_date");
            final String vip_time = (String) payload.get("vip_time");
            final Number quota_limit_download_daily_byte = (Number) payload.get("quota_limit_download_daily_byte");
            // final Number downloaded_lastday_byte = (Number)payload.get("downloaded_lastday_byte");
            if (register_date != null) {
                ai.setCreateTime(TimeFormatter.getMilliSeconds(register_date, "yyyy-MM-dd hh:mm:ss", Locale.ENGLISH));
            }
            long premiumValidUntil = -1;
            if (vip_time != null) {
                premiumValidUntil = TimeFormatter.getMilliSeconds(vip_time, "yyyy-MM-dd hh:mm:ss", Locale.ENGLISH);
            }
            if (premiumValidUntil < System.currentTimeMillis()) {
                // ai.setExpired(true);
                throw new AccountInvalidException("Free accounts are not supported");
            }
            if (quota_limit_download_daily_byte != null) {
                /* 2024-06-26: API does not return traffic left so we set traffic max as traffic left. */
                ai.setTrafficLeft(quota_limit_download_daily_byte.longValue());
                ai.setTrafficMax(quota_limit_download_daily_byte.longValue());
            }
            account.setType(AccountType.PREMIUM);
        }
        return ai;
    }

    /** https://4share.vn/documents/api/#api-AccessToken-AccessToken */
    private Object loginAPI(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            /* Load cookies */
            this.setBrowserExclusive();
            final String storedToken = account.getStringProperty(PROPERTY_ACCOUNT_TOKEN);
            if (storedToken != null) {
                /* Work with existing token */
                if (!force) {
                    /* Do not check existing token. */
                    return null;
                }
                logger.info("Checking token validity");
                try {
                    final PostRequest req = br.createPostRequest(API_BASE + "?cmd=check_token", new UrlQuery());
                    final Object responseO = this.callAPI(req, account, null);
                    logger.info("Token is valid");
                    return responseO;
                } catch (final PluginException e) {
                    logger.log(e);
                    logger.info("Token login failed");
                }
            }
            /* Obtain new token. */
            account.removeProperty(PROPERTY_ACCOUNT_TOKEN);
            final UrlQuery query = new UrlQuery();
            query.add("username", Encoding.urlEncode(account.getUser()));
            query.add("password", Encoding.urlEncode(account.getPass()));
            final PostRequest req = br.createPostRequest(API_BASE + "?cmd=get_token", query);
            final Object response = this.callAPI(req, account, null);
            final String newtoken = response.toString();
            if (StringUtils.isEmpty(newtoken)) {
                /* This should never happen. */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            account.setProperty(PROPERTY_ACCOUNT_TOKEN, newtoken);
            return response;
        }
    }

    @Override
    public String getAGBLink() {
        return "https://" + getHost() + "/?act=terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 5;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        String dllink = null;
        // Can be skipped
        // int wait = 60;
        // final String waittime = br.getRegex("var counter=(\\d+);").getMatch(0);
        // if (waittime != null) {
        // wait = Integer.parseInt(waittime);
        // }
        // sleep(wait * 1001l, downloadLink);
        if (br.containsHTML("chứa file này đang bảo dưỡng")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server is under maintenance!");
        }
        for (int i = 0; i <= 3; i++) {
            Form captchaform = br.getFormbyKey("free_download");
            if (captchaform == null) {
                captchaform = new Form();
                captchaform.setMethod(MethodType.POST);
            }
            final InputField ifield = captchaform.getInputField("free_download");
            if (ifield == null || ifield.getValue() == null) {
                captchaform.put("free_download", "");
            }
            final CaptchaHelperHostPluginRecaptchaV2 rc2 = new CaptchaHelperHostPluginRecaptchaV2(this, br);
            final String recaptchaV2Response = rc2.getToken();
            if (recaptchaV2Response == null) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            captchaform.put("g-recaptcha-response", recaptchaV2Response);
            br.submitForm(captchaform);
            dllink = br.getRedirectLocation();
            if (dllink == null && br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                continue;
            }
            break;
        }
        if (dllink == null && br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        } else if (dllink == null) {
            handleErrorsWebsite(br);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, this.isResumeable(link, null), this.getMaxChunks(link, null));
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection(true);
            handleErrorsWebsite(br);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        /* 2024-06-26: psp: The API call for downloading is broken. I've reported this bug to the 4share.vn support. */
        final boolean apiDownloadBroken = true;
        if (USE_API_FOR_ACCOUNT_ACTIONS && !apiDownloadBroken) {
            handlePremiumAPI(link, account);
        } else {
            handlePremiumWebsite(link, account);
        }
    }

    private void handlePremiumWebsite(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        loginWebsite(account, false);
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, this.getContentURL(link), this.isResumeable(link, account), this.getMaxChunks(link, account));
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            /* Maybe no direct download */
            br.followConnection(true);
            dl = null;
            String dllink = br.getRegex("class=''> <a href\\s*=\\s*'(https?://.*?)'").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("('|\")(https?://sv\\d+\\.4share\\.vn(:\\d+)?/[^<>\"]*?)\\1").getMatch(1);
                if (dllink == null) {
                    handleErrorsWebsite(br);
                    logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, this.isResumeable(link, account), this.getMaxChunks(link, account));
        }
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection(true);
            handleErrorsWebsite(br);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void handlePremiumAPI(final DownloadLink link, final Account account) throws Exception {
        requestFileInformationAPI(link, account);
        final String fid = this.getFID(link);
        final UrlQuery query = new UrlQuery();
        query.add("file_id", Encoding.urlEncode(fid));
        final PostRequest req = br.createPostRequest(API_BASE + "?cmd=get_download_link", query);
        final Map<String, Object> payload = (Map<String, Object>) this.callAPI(req, account, null);
        final String directurl = payload.get("download_link").toString();
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, directurl, this.isResumeable(link, account), this.getMaxChunks(link, account));
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection(true);
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server did not respond with file content");
        }
        dl.startDownload();
    }

    private void loginWebsite(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            /* Load cookies */
            this.setBrowserExclusive();
            boolean refresh = true;
            final Cookies cookies = account.loadCookies("");
            if (cookies != null) {
                br.setCookies(cookies);
                if (!force) {
                    /* Do not validate cookies */
                    return;
                }
                br.getPage("https://" + getHost() + "/member");
                if (this.isLoggedINWebsite(br)) {
                    logger.info("Cookie login successful");
                    refresh = false;
                } else {
                    logger.info("Cookie login failed");
                    refresh = true;
                    account.clearCookies("");
                }
            }
            if (refresh) {
                br.getPage("https://4share.vn/");
                br.getPage("/default/login");
                final CaptchaHelperHostPluginRecaptchaV2 rc2 = new CaptchaHelperHostPluginRecaptchaV2(this, br, "6Lfnb8YUAAAAAElE9DwwEWA881UX3-chISAQZApu") {
                    @Override
                    public org.jdownloader.captcha.v2.challenge.recaptcha.v2.AbstractRecaptchaV2.TYPE getType() {
                        return TYPE.INVISIBLE;
                    }
                };
                br.getHeaders().put("Accept", "application/json, text/plain, */*");
                br.postPage("/a_p_i/public-common/login", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&captcha=" + Encoding.urlEncode(rc2.getToken()));
                if (br.getCookie(br.getHost(), "currentUser", Cookies.NOTDELETEDPATTERN) == null) {
                    throw new AccountInvalidException();
                }
                br.getPage("/member");
                if (!this.isLoggedINWebsite(br)) {
                    throw new AccountInvalidException();
                }
            }
            account.saveCookies(br.getCookies(br.getHost()), "");
        }
    }

    private boolean isLoggedINWebsite(final Browser br) {
        if (br.getCookie(br.getHost(), "currentUser", Cookies.NOTDELETEDPATTERN) != null && br.containsHTML("/login/logout")) {
            return true;
        } else {
            return false;
        }
    }

    /* API docs: https://4share.vn/documents/api/ */
    private Object callAPI(final Request req, final Account account, final DownloadLink link) throws IOException, PluginException, InterruptedException {
        final String token = account.getStringProperty(PROPERTY_ACCOUNT_TOKEN);
        if (token != null) {
            req.getHeaders().put("accesstoken01", token);
        }
        br.getPage(req);
        return checkErrors(account, link);
    }

    private Object checkErrors(final Account account, final DownloadLink link) throws PluginException, InterruptedException {
        try {
            final Object jsonO = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.OBJECT);
            // if (jsonO == null || !(jsonO instanceof Map)) {
            // return jsonO;
            // }
            final Map<String, Object> entries = (Map<String, Object>) jsonO;
            final Number errorNumberO = (Number) entries.get("errorNumber");
            final Object payload = entries.get("payload");
            if (errorNumberO == null || errorNumberO.intValue() == 0) {
                /* No error -> Return payload */
                return payload;
            }
            /* Looks like on error, they always return code -100. */
            // final int errorNumber = errorNumberO.intValue();
            final String msg;
            if (payload instanceof String) {
                /* On error, payload should contain an error message as string */
                msg = payload.toString();
            } else {
                msg = "API error " + errorNumberO;
            }
            if (msg.matches("(?i).*Please input valid access token.*")) {
                /* Invalid access token */
                throw new AccountUnavailableException(msg, 1 * 60 * 1000l);
            } else if (msg.matches("(?i).*Not valid file_id.*")) {
                /* File offline */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, msg);
            } else {
                /* Unknown error */
                if (link != null) {
                    /* E.g. {"success":false,"detail":"Failed to request web download. Please try again later.","data":null} */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, msg);
                } else {
                    throw new AccountInvalidException(msg);
                }
            }
        } catch (final JSonMapperException jme) {
            final String errortext = "Bad API response";
            if (link != null) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errortext);
            } else {
                throw new AccountUnavailableException(errortext, 1 * 60 * 1000l);
            }
        }
    }

    private void handleErrorsWebsite(final Browser br) throws PluginException {
        if (this.br.containsHTML("(?i)File này tạm dừng Download do yêu cầu của Người upload|Thông báo với Administrator\\!")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 30 * 60 * 1000l);
        } else if (br.containsHTML("chứa file này đang bảo dưỡng")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server is under maintenance!");
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc != null && AccountType.PREMIUM.equals(acc.getType())) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public boolean hasAutoCaptcha() {
        // recaptcha
        return false;
    }
}
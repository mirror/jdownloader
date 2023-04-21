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

import java.util.Locale;

import org.appwork.utils.Regex;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.parser.html.InputField;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
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

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("up.4share.vn/", "4share.vn/"));
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

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        correctDownloadLink(link);
        prepBR(br);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        getPage(link.getPluginPatternMatcher());
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
                filesize = br.getRegex("/strong>\\s*</h1>\\s*(\\d+[^<>\"]+)<br/>").getMatch(0);
            }
        }
        if (filename != null) {
            link.setName(Encoding.htmlDecode(filename).trim());
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        final String md5 = br.getRegex("MD5\\s*:?\\s*([a-f0-9]{32})").getMatch(0);
        if (md5 != null) {
            link.setMD5Hash(md5);
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

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        synchronized (account) {
            login(account, true);
            getPage("/member");
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
            final String expire = br.getRegex("Ngày hết hạn: <b>(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})").getMatch(0);
            if (expire == null) {
                ai.setExpired(true);
                return ai;
            } else {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd hh:mm:ss", Locale.ENGLISH));
            }
            if (br.containsHTML("Gold TKC")) {
                ai.setStatus("Premium (Gold) User");
            } else {
                ai.setStatus("Premium (VIP) User");
            }
        }
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://up.4share.vn/?act=terms";
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
        br.setFollowRedirects(false);
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
            handleErrorsGeneral();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, true, 1);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection(true);
            handleErrorsGeneral();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        getPage(link.getDownloadURL());
        if (br.containsHTML("chứa file này đang bảo dưỡng")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server is under maintenance!");
        }
        String dllink = br.getRedirectLocation();
        if (dllink == null) {
            dllink = br.getRegex("class=''> <a href\\s*=\\s*'(https?://.*?)'").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("('|\")(https?://sv\\d+\\.4share\\.vn/[^<>\"]*?)\\1").getMatch(1);
                if (dllink == null) {
                    handleErrorsGeneral();
                    logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, true, 0);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection(true);
            handleErrorsGeneral();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            boolean redirect = this.br.isFollowingRedirects();
            try {
                /* Load cookies */
                this.setBrowserExclusive();
                prepBR(br);
                boolean refresh = true;
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    br.setCookies(account.getHoster(), cookies);
                    if (!force) {
                        refresh = false;
                    } else {
                        getPage("https://4share.vn/");
                        if (!br.containsHTML("TK <b>VIP") && !br.containsHTML("Hạn sử dụng VIP: \\d{2,4}-\\d{2}-\\d{2,4}") && !br.containsHTML("Còn hạn sử dụng VIP đến: \\d{2,4}-\\d{2}-\\d{2,4}") && !br.containsHTML("Hạn dùng: \\d{2,4}-\\d{2}-\\d{2,4}")) {
                            refresh = true;
                        } else {
                            refresh = false;
                        }
                    }
                }
                if (refresh) {
                    br.setFollowRedirects(true);
                    getPage("https://4share.vn/");
                    getPage("https://4share.vn/default/login");
                    final CaptchaHelperHostPluginRecaptchaV2 rc2 = new CaptchaHelperHostPluginRecaptchaV2(this, br, "6Lfnb8YUAAAAAElE9DwwEWA881UX3-chISAQZApu") {
                        @Override
                        public org.jdownloader.captcha.v2.challenge.recaptcha.v2.AbstractRecaptchaV2.TYPE getType() {
                            return TYPE.INVISIBLE;
                        }
                    };
                    br.getHeaders().put("Accept", "application/json, text/plain, */*");
                    postPage("https://4share.vn/a_p_i/public-common/login", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&captcha=" + Encoding.urlEncode(rc2.getToken()));
                    final String lang = System.getProperty("user.language");
                    if (br.getCookie(br.getHost(), "currentUser", Cookies.NOTDELETEDPATTERN) == null) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    getPage("https://4share.vn/");
                    if (!br.containsHTML("TK <b>VIP") && !br.containsHTML("Hạn sử dụng VIP: \\d{2,4}-\\d{2}-\\d{2,4}") && !br.containsHTML("Còn hạn sử dụng VIP đến: \\d{2,4}-\\d{2}-\\d{2,4}") && !br.containsHTML("Hạn dùng: \\d{2,4}-\\d{2}-\\d{2,4}")) {
                        if ("de".equalsIgnoreCase(lang)) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Account Typ!\r\nDas ist ein kostenloser Account.\r\nJDownloader unterstützt keine kostenlosen Accounts für diesen Hoster!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid account type!\r\nThis is a free account. JDownloader only supports premium (VIP) accounts for this host!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                    }
                }
                account.saveCookies(br.getCookies(br.getHost()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            } finally {
                this.br.setFollowRedirects(redirect);
            }
        }
    }

    private void getPage(final String string) throws Exception {
        if (string == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (int i = 0; i != 4; i++) {
            try {
                br.getPage(string);
            } catch (final BrowserException e) {
                if (e.getCause() != null && e.getCause().toString().contains("ConnectException")) {
                    if (i + 1 == 4) {
                        throw e;
                    } else {
                        continue;
                    }
                } else {
                    throw e;
                }
            }
            break;
        }
    }

    private void postPage(String string, String string2) throws Exception {
        if (string == null || string2 == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (int i = 0; i != 4; i++) {
            try {
                br.postPage(string, string2);
            } catch (final BrowserException e) {
                if (e.getCause() != null && e.getCause().toString().contains("ConnectException")) {
                    continue;
                } else {
                    throw e;
                }
            }
            break;
        }
    }

    private Browser prepBR(final Browser br) {
        br.setReadTimeout(2 * 60 * 1000);
        br.setConnectTimeout(2 * 60 * 1000);
        return br;
    }

    private void handleErrorsGeneral() throws PluginException {
        if (this.br.containsHTML("(?i)File này tạm dừng Download do yêu cầu của Người upload|Thông báo với Administrator\\!")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 30 * 60 * 1000l);
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
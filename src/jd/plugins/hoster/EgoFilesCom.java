//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "egofiles.com" }, urls = { "https?://(www\\.)?egofiles\\.com/[A-Za-z0-9]+" }, flags = { 2 })
public class EgoFilesCom extends PluginForHost {

    public EgoFilesCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://egofiles.com/premium");
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://egofiles.com/tos";
    }

    private static final String MAINPAGE       = "http://egofiles.com";
    private static Object       LOCK           = new Object();
    public static final String  USETHTTPSLOGIN = "USETHTTPSLOGIN";
    public static final String  USEALWAYSHTTP  = "USEALWAYSHTTP";
    private static final String INVALIDLINKS   = "https?://(www\\.)?egofiles\\.com/(checker|dmca|files|help|logout|policy|premium|remote|settings|style|tos|voucher|register|password|cache)";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        if (link.getDownloadURL().matches(INVALIDLINKS)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final boolean alwaysHttp = this.getPluginConfig().getBooleanProperty(jd.plugins.hoster.EgoFilesCom.USEALWAYSHTTP, false);
        if (alwaysHttp) {
            link.setUrlDownload(link.getDownloadURL().replace("https", "http"));
        }
        this.setBrowserExclusive();
        br.setCookie(MAINPAGE, "lang", "en");
        br.getPage(link.getDownloadURL());
        // (Invalid link | Link offline, this also means country block!)
        if (br.containsHTML("(>404 \\- Not Found<|>404 File not found<)") || br.containsHTML("Ten link jest niepoprawny, sprawdź czy nie ma jakiegoś błędu\\.")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String filename = br.getRegex("<div class=\"down\\-file\">([^<>\"]*?)<div").getMatch(0);
        final String filesize = br.getRegex("File size: ([^<>\"]*?) \\|").getMatch(0);
        if (filename == null || filesize == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "filename or filesize not recognized"); }
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {

        // final boolean alwaysHttp = this.getPluginConfig().getBooleanProperty(jd.plugins.hoster.EgoFilesCom.USEALWAYSHTTP, false);
        final String connectionType = (downloadLink.getDownloadURL().contains("https") ? "https" : "http");
        requestFileInformation(downloadLink);

        Regex limitReached = br.getRegex(">For next free download you have to wait <strong>(\\d+)m (\\d+)s</strong>");
        if (limitReached.getMatches().length > 0) {
            logger.warning("Free download limit reached, waittime is applied.");
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, (Integer.parseInt(limitReached.getMatch(0)) * 60 + Integer.parseInt(limitReached.getMatch(1))) * 1001l);
        }
        if (br.containsHTML("Your IP have reached daily <strong>downloading&nbsp;limit")) {
            logger.warning("Free daily download limit reached!");
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
        }
        final String rcID = br.getRegex("google\\.com/recaptcha/api/challenge\\?k=([^<>\"]*?)\"").getMatch(0);
        if (rcID == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "reCaptcha ID not recognized"); }
        final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        rc.setId(rcID);
        for (int i = 0; i <= 5; i++) {
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, downloadLink);
            br.postPage(downloadLink.getDownloadURL(), "recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + c);
            if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                rc.reload();
                continue;
            }
            break;
        }
        if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
            logger.info("6 reCaptcha tryouts for <" + downloadLink.getDownloadURL() + "> were incorrect");
            throw new PluginException(LinkStatus.ERROR_CAPTCHA, "reCaptcha error");
        }
        br.setFollowRedirects(false);
        String dllink = br.getRegex("<h2 class=\"grey\\-brake\"><a href=\"(" + connectionType + "://[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(" + connectionType + "://s\\d+\\.egofiles\\.com:\\d+/dl/[a-z0-9]+/[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("<h2 class=\"grey\\-brake\">[ \t\n\r\f]+<a href=\"(" + connectionType + "://[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            limitReached = br.getRegex(">For next free download you have to wait <strong>(\\d+)m (\\d+)s</strong>");
            if (limitReached.getMatches().length > 0) {
                logger.warning("Free download limit reached, waittime is applied.");
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, (Integer.parseInt(limitReached.getMatch(0)) * 60 + Integer.parseInt(limitReached.getMatch(1))) * 1001l);
            } else {
                if (br.containsHTML("socket error")) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Https socket error", 10 * 1000l); }
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!\n" + br.getRequest().getHtmlCode());
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "final dllink is null");
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!\nGot response:\n" + dl.getConnection().getResponseMessage());
            br.followConnection();
            logger.info("browser code: " + br.getRequest().getHtmlCode());
            if (br.containsHTML("Download link has expired.") || br.containsHTML("Download link has expired or you have reached the limit.")) {
                throw new PluginException(LinkStatus.ERROR_RETRY, "Download link has expired or limit reached", 5 * 60 * 1000l);
            } else
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "getConnection returns html");
        }
        dl.startDownload();
    }

    @SuppressWarnings("unchecked")
    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                br.setAcceptLanguage("en, pl;q=0.8");
                br.setCookie(MAINPAGE, "lang", "en");
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(false);
                final boolean httpsLogin = this.getPluginConfig().getBooleanProperty(jd.plugins.hoster.EgoFilesCom.USETHTTPSLOGIN, false);
                String loginType = "http";
                if (httpsLogin) loginType = "https";
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.postPage(loginType + "://egofiles.com/ajax/register.php", "log=1&loginV=" + Encoding.urlEncode(account.getUser()) + "&passV=" + Encoding.urlEncode(account.getPass()));
                br.getHeaders().put("X-Requested-With", null);
                if (br.getCookie(MAINPAGE, "p") == null || br.getCookie(MAINPAGE, "h") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(MAINPAGE);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            ai.setStatus("Login failed");
            String errorMsg = "Please check your Username and Password!";
            if (br.getRequest().getHtmlCode().contains("error")) {
                errorMsg = getJson("error", br);
            }
            UserIO.getInstance().requestMessageDialog(0, "EgoFiles.com Premium Error!", errorMsg);
            account.setValid(false);
            return ai;
        }
        br.getPage(MAINPAGE);
        if (br.containsHTML("Traffic left:")) {
            String trafficLeft = br.getRegex("Traffic left: (.*?)[\t\n\r\f]").getMatch(0);
            ai.setTrafficLeft(SizeFormatter.getSize(trafficLeft));
        } else
            ai.setUnlimitedTraffic();

        final String expire = br.getRegex("<br/> Premium: (\\d{4}\\-\\d{2}\\-\\d{2} \\d{2}:\\d{2}:\\d{2})").getMatch(0);
        if (expire == null) {
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd hh:mm:ss", null));
        }
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {

        final String connectionType = (link.getDownloadURL().contains("https") ? "https" : "http");

        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        String dllink = br.getRedirectLocation();

        if (dllink == null) dllink = br.getRegex("<h2 class=\"grey\\-brake\">[ \t\n\r\f]+<a href=\"(" + connectionType + "://[^<>\"]*?)\"").getMatch(0);

        if (dllink == null) {
            // based on the logs only, need real Premium to check
            final String limitMsg = br.getRegex("<h2 class=\"grey\\-brake\">(.*?)</h2>").getMatch(0);
            if (limitMsg != null) {
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!\nMessage: " + limitMsg);
                UserIO.getInstance().requestMessageDialog(0, "EgoFiles.com Premium Error", limitMsg + "\r\nPremium disabled, will continue downloads as Free User");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            } else {
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!\n" + br.getRequest().getHtmlCode());
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "final dllink is null");
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink), true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!\nGot response:\n" + dl.getConnection().getResponseMessage());
            br.followConnection();
            logger.info("browser code: " + br.getRequest().getHtmlCode());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "getConnection returns html");
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), EgoFilesCom.USETHTTPSLOGIN, JDL.L("plugins.hoster.egofilescom.usehttpslogin", "Use HTTPS for login")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), EgoFilesCom.USEALWAYSHTTP, JDL.L("plugins.hoster.egofilescom.usealwayshttp", "Use always http connection")).setDefaultValue(false));
    }

    public static String getJson(final String parameter, final Browser br2) {
        String result = br2.getRegex("\"" + parameter + "\":\"(.*?)\"").getMatch(0);
        return result;
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }
}
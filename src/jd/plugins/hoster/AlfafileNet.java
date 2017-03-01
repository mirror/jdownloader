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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
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
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.JDUtilities;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "alfafile.net" }, urls = { "https?://(www\\.)?alfafile\\.net/file/[A-Za-z0-9]+" })
public class AlfafileNet extends PluginForHost {

    public AlfafileNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://alfafile.net/premium");
    }

    @Override
    public String getAGBLink() {
        return "http://alfafile.net/terms";
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("http://", "https://"));
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME                  = false;
    private static final int     FREE_MAXCHUNKS               = 1;
    private static final int     FREE_MAXDOWNLOADS            = 1;
    private static final boolean ACCOUNT_FREE_RESUME          = false;
    private static final int     ACCOUNT_FREE_MAXCHUNKS       = 1;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS    = 1;
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = -5;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;

    /* don't touch the following! */
    private static AtomicInteger maxPrem                      = new AtomicInteger(1);

    private boolean              isDirecturl                  = false;

    /*
     * TODO: Use API for linkchecking whenever an account is added to JD. This will ensure that the plugin will always work, at least for
     * premium users. Status 2015-08-03: Filecheck API does not seem to work --> Disabled it - reported API issues to jiaz.
     */
    private static final boolean prefer_api_linkcheck         = false;

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        isDirecturl = false;

        this.setBrowserExclusive();
        prepBR();
        String filename = null;
        String filesize = null;
        String md5 = null;
        boolean api_works = false;
        Account aa = AccountController.getInstance().getValidAccount(this);
        String api_token = null;
        if (aa != null) {
            api_token = getLoginToken(aa);
        }
        if (api_token != null && prefer_api_linkcheck) {
            this.br.getPage("https://alfafile.net/api/v1/file/info?file_id=" + getFileID(link) + "&token=" + api_token);
            final String status = PluginJSonUtils.getJsonValue(br, "status");
            if (!"401".equals(status)) {
                api_works = true;
            }
        }

        if (api_works) {
            final String status = PluginJSonUtils.getJsonValue(br, "status");
            if (!"200".equals(status)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = PluginJSonUtils.getJsonValue(br, "name");
            filesize = PluginJSonUtils.getJsonValue(br, "size");
            md5 = PluginJSonUtils.getJsonValue(br, "hash");
        } else {
            URLConnectionAdapter con = null;
            try {
                con = br.openGetConnection(link.getDownloadURL());
                if (!con.getContentType().contains("html")) {
                    logger.info("This url is a directurl");
                    link.setDownloadSize(con.getLongContentLength());
                    link.setFinalFileName(getFileNameFromHeader(con));
                    isDirecturl = true;
                    return AvailableStatus.TRUE;
                } else {
                    br.followConnection();
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
            if (this.br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("id=\"st_file_name\" title=\"([^<>\"]*?)\"").getMatch(0);
            filesize = br.getRegex("<span class=\"size\">([^<>\"]*?)</span>").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        if (StringUtils.isNotEmpty(filesize)) {
            link.setDownloadSize(SizeFormatter.getSize(filesize.contains(".") && filesize.contains(",") ? filesize.replace(",", "") : filesize));
        }
        if (md5 != null) {
            /* TODO: Check if their API actually returns valid md5 hashes */
            link.setMD5Hash(md5);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        if (checkShowFreeDialog(getHost())) {
            showFreeDialog(getHost());
        }
        String dllink = checkDirectLink(downloadLink, directlinkproperty);
        if (dllink == null) {
            if (isDirecturl) {
                dllink = downloadLink.getDownloadURL();
            } else {
                final String fid = getFileID(downloadLink);
                br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.getPage("/download/start_timer/" + fid);
                final String reconnect_wait = br.getRegex("Try again in (\\d+) minutes").getMatch(0);
                if (br.containsHTML(">This file can be downloaded by premium users only|>You can download files up to")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
                } else if (reconnect_wait != null) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Long.parseLong(reconnect_wait) * 60 * 1001l);
                } else if (br.containsHTML("You can't download not more than \\d+ file at a time")) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many max sim dls", 20 * 60 * 1000l);
                } else if (br.containsHTML("You have reached your daily downloads limit. Please try again later\\.")) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "You have reached your daily download limit.", 3 * 60 * 60 * 1000l);
                }
                int wait = 45;
                String wait_str = br.getRegex(">(\\d+) <span>s<").getMatch(0);
                if (wait_str != null) {
                    wait = Integer.parseInt(wait_str);
                }
                String redirect_url = PluginJSonUtils.getJsonValue(br, "redirect_url");
                if (redirect_url == null) {
                    redirect_url = "/file/" + fid + "/captcha";
                }
                this.sleep(wait * 1001l, downloadLink);
                br.getPage(redirect_url);
                if (br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 5 * 60 * 1000l);
                }
                this.br.setFollowRedirects(true);
                boolean success = false;
                for (int i = 0; i <= 3; i++) {
                    final PluginForDecrypt solveplug = JDUtilities.getPluginForDecrypt("linkcrypt.ws");
                    final SolveMedia sm = new SolveMedia(br);
                    sm.setSecure(true);
                    File cf = null;
                    try {
                        cf = sm.downloadCaptcha(getLocalCaptchaFile());
                    } catch (final Exception e) {
                        if (SolveMedia.FAIL_CAUSE_CKEY_MISSING.equals(e.getMessage())) {
                            throw new PluginException(LinkStatus.ERROR_FATAL, "Host side solvemedia.com captcha error - please contact the " + this.getHost() + " support");
                        }
                        throw e;
                    }
                    final String code = getCaptchaCode("solvemedia", cf, downloadLink);
                    final String chid = sm.getChallenge(code);
                    this.br.postPage(this.br.getURL(), "send=Send&adcopy_response=" + Encoding.urlEncode(code) + "&adcopy_challenge=" + Encoding.urlEncode(chid));
                    if (br.containsHTML("solvemedia\\.com/papi/")) {
                        continue;
                    }
                    success = true;
                    break;
                }
                if (!success) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
                dllink = br.getRegex("href=\"(https://[^<>\"]*?)\" class=\"big_button\"><span>Download</span>").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("\"(https?://[a-z0-9\\-]+\\.alfafile\\.net/dl/[^<>\"]*?)\"").getMatch(0);
                }
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(directlinkproperty, dllink);
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openGetConnection(dllink);
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

    private void prepBR() {
        this.br.setCookie(this.getHost(), "lang", "en");
        this.br.setFollowRedirects(true);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    private static final String MAINPAGE = "http://alfafile.net";
    private static Object       LOCK     = new Object();

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                prepBR();
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(false);
                br.getPage("https://alfafile.net/api/v1/user/login?login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                final String token = PluginJSonUtils.getJsonValue(br, "token");
                if (token == null || !"200".equals(PluginJSonUtils.getJsonValue(br, "status"))) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(MAINPAGE);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
                account.setProperty("token", token);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        final long traffic_total = JavaScriptEngineFactory.toLong(JavaScriptEngineFactory.walkJson(entries, "response/user/traffic/total"), -1);
        final long traffic_left = JavaScriptEngineFactory.toLong(JavaScriptEngineFactory.walkJson(entries, "response/user/traffic/left"), -1);
        final String ispremium = PluginJSonUtils.getJsonValue(br, "is_premium");
        if ("true".equals(ispremium)) {
            final String expire = PluginJSonUtils.getJsonValue(br, "premium_end_time");
            ai.setValidUntil(Long.parseLong(expire) * 1000);
            maxPrem.set(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(maxPrem.get());
            account.setConcurrentUsePossible(true);
            ai.setStatus("Premium account");
        } else {
            maxPrem.set(ACCOUNT_FREE_MAXDOWNLOADS);
            account.setType(AccountType.FREE);
            /* free accounts can still have captcha */
            account.setMaxSimultanDownloads(maxPrem.get());
            account.setConcurrentUsePossible(false);
            ai.setStatus("Registered (free) user");
        }
        ai.setTrafficLeft(traffic_left);
        ai.setTrafficMax(traffic_total);
        account.setValid(true);
        return ai;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        if (account.getType() == AccountType.FREE) {
            /*
             * No API --> We're actually not downloading via free account but it doesnt matter as there are no known free account advantages
             * compared to unregistered mode.
             */
            br.getPage(link.getDownloadURL());
            doFree(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
        } else {
            String dllink = this.checkDirectLink(link, "premium_directlink");
            if (dllink == null) {
                final String fid = getFileID(link);
                this.br.getPage("/api/v1/file/download?file_id=" + fid + "&token=" + getLoginToken(account));
                handleErrorsGeneral();
                dllink = PluginJSonUtils.getJsonValue(br, "download_url");
                if (dllink == null) {
                    logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
            if (dl.getConnection().getContentType().contains("html")) {
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                }
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty("premium_directlink", dllink);
            dl.startDownload();
        }
    }

    private void handleErrorsGeneral() throws PluginException {
        final String errorcode = PluginJSonUtils.getJsonValue(br, "status");
        String errormessage = PluginJSonUtils.getJsonValue(br, "details");
        if (errorcode != null) {
            if (errorcode.equals("401")) {
                /* This can sometimes happen in premium mode */
                /* {"response":null,"status":401,"details":"Unauthorized. Token doesn't exist"} */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 401 - unauthorized", 5 * 60 * 1000l);
            } else if (errorcode.equals("404")) {
                /*
                 * E.g. detailed errormessages: "details":"File with file_id: '1234567' doesn't exist"
                 */
                if (errormessage == null) {
                    errormessage = "File does not exist according to API";
                }
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errormessage, 30 * 60 * 1000l);
            } else if (errorcode.equals("409")) {
                /*
                 * E.g. detailed errormessages:
                 * 
                 * Conflict. Delay between downloads must be not less than 60 minutes. Try again in 51 minutes.
                 * 
                 * Conflict. DOWNLOAD::ERROR::You can't download not more than 1 file at a time in free mode.
                 */
                String minutes_regexed = null;
                int minutes = 60;
                if (errormessage != null) {
                    minutes_regexed = new Regex(errormessage, "again in (\\d+) minutes?").getMatch(0);
                    if (minutes_regexed != null) {
                        minutes = Integer.parseInt(minutes_regexed);
                    }
                }
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, minutes * 60 * 1001l);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private String getFileID(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
    }

    private String getLoginToken(final Account acc) {
        return acc.getStringProperty("token", null);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /* workaround for free/premium issue on stable 09581 */
        return maxPrem.get();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
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
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.SiteType.SiteTemplate;
import jd.utils.JDUtilities;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;

/* ChomikujPlScript */
@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "minhateca.com.br" }, urls = { "http://minhatecadecrypted\\.com\\.br/\\d+" })
public class MinhatecaComBr extends PluginForHost {
    @Override
    public String buildExternalDownloadURL(DownloadLink downloadLink, PluginForHost buildForThisPlugin) {
        if (StringUtils.equals(getHost(), buildForThisPlugin.getHost())) {
            return super.buildExternalDownloadURL(downloadLink, buildForThisPlugin);
        } else {
            final String contentURL = downloadLink.getContentUrl();
            if (contentURL != null) {
                return contentURL;
            }
            return downloadLink.getStringProperty("mainlink", null);
        }
    }

    public MinhatecaComBr(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(MAINPAGE);
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://minhateca.com.br/termosecondicoes.aspx";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME                  = true;
    private static final int     FREE_MAXCHUNKS               = 0;
    private static final int     FREE_MAXDOWNLOADS            = 20;
    private static final boolean ACCOUNT_FREE_RESUME          = true;
    private static final int     ACCOUNT_FREE_MAXCHUNKS       = 0;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS    = 20;
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    /* note: CAN NOT be negative or zero! (ie. -1 or 0) Otherwise math sections fail. .:. use [1-20] */
    private static AtomicInteger totalMaxSimultanFreeDownload = new AtomicInteger(FREE_MAXDOWNLOADS);
    /* don't touch the following! */
    private static AtomicInteger maxPrem                      = new AtomicInteger(1);
    private static boolean       pluginloaded                 = false;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (link.getBooleanProperty("offline", false)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.setBrowserExclusive();
        br.setAllowedResponseCodes(new int[] { 401 });
        br.setFollowRedirects(true);
        final String url = link.getStringProperty("mainlink", null);
        if (url == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.getPage(url);
        if (br.containsHTML("class=\"noFile\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = link.getStringProperty("plain_filename", null);
        final String filesize = link.getStringProperty("plain_filesize", null);
        link.setFinalFileName(filename);
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    public void doFree(final DownloadLink downloadLink, boolean resumable, int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        handlePWProtected(downloadLink);
        final String requestVerificationToken = br.getRegex("name=\"__RequestVerificationToken\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
        final String fid = downloadLink.getStringProperty("plain_fid", null);
        String dllink = checkDirectLink(downloadLink, "directlink");
        if (dllink == null && downloadLink.getFinalFileName().contains(".mp3") && this.getPluginConfig().getBooleanProperty("ENABLE_MP3_STREAM_DOWNLOAD", true)) {
            /*
             * 2017-05-09: Captcha can be bypassed by downloading the mp4 stream. Only disadvantage I have noticed: Cover inside audio file
             * is missing (quality == original-download-quality)
             */
            try {
                final Browser br2 = this.br.cloneBrowser();
                br2.setFollowRedirects(false);
                br2.getPage("http://minhateca.com.br/Audio.ashx?id=" + fid + "&type=2&tp=mp3");
                dllink = br2.getRedirectLocation();
                if (!StringUtils.isEmpty(dllink)) {
                    maxchunks = 1;
                    resumable = true;
                }
            } catch (final Throwable e) {
            }
        }
        if (dllink == null && this.br.containsHTML("class=\"videoWrapper\"") && this.getPluginConfig().getBooleanProperty("ENABLE_MP4_STREAM_DOWNLOAD", true)) {
            /*
             * 2017-05-09: Captcha can be bypassed by downloading the mp4 stream. Quality is usually much worse than the
             * original-file-download. This can not only skip the captcha but also it makes it possible to download premiumonly video files
             * as freeuser.
             */
            String video_redirecturl = this.br.getRegex("(https?://[^/]+/Video\\.ashx[^<>\"\\'/]+)").getMatch(0);
            if (video_redirecturl != null) {
                video_redirecturl = Encoding.htmlDecode(video_redirecturl);
                try {
                    final Browser br2 = this.br.cloneBrowser();
                    br2.setFollowRedirects(false);
                    br2.getPage(video_redirecturl);
                    dllink = br2.getRedirectLocation();
                    if (!StringUtils.isEmpty(dllink)) {
                        maxchunks = 1;
                        resumable = false;
                    }
                } catch (final Throwable e) {
                }
            }
        }
        if (dllink == null) {
            if (requestVerificationToken == null) {
                logger.warning("req_token is null");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getHeaders().put("Accept", "*/*");
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.postPage("http://" + this.getHost() + "/action/License/Download", "fileId=" + fid + "&__RequestVerificationToken=" + Encoding.urlEncode(requestVerificationToken));
            /* 2017-05-09: Added captcha support */
            String captchaurl = getCaptchaURL();
            if (captchaurl != null) {
                final String unescapedBR = unescape(br.toString());
                final String serializedUserSelection = new Regex(unescapedBR, "name=\"SerializedUserSelection\" type=\"hidden\" value=\"([^<>\"]+)\"").getMatch(0);
                final String serializedOrgFile = new Regex(unescapedBR, "name=\"SerializedOrgFile\" type=\"hidden\" value=\"([^<>\"]+)\"").getMatch(0);
                if (serializedUserSelection == null || serializedOrgFile == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                int counter = 0;
                do {
                    final String captcha = this.getCaptchaCode(captchaurl, downloadLink);
                    /*
                     * This request can contain one more parameter (optional): + "&FileName=" +
                     * Encoding.urlEncode(downloadLink.getFinalFileName())
                     */
                    br.postPage("/action/License/DownloadNotLoggedCaptchaEntered", "FileId=" + fid + "&__RequestVerificationToken=" + Encoding.urlEncode(requestVerificationToken) + "&SerializedUserSelection=" + Encoding.urlEncode(serializedUserSelection) + "&SerializedOrgFile=" + Encoding.urlEncode(serializedOrgFile) + "&recaptcha_response_field=" + Encoding.urlEncode(captcha));
                    captchaurl = getCaptchaURL();
                    counter++;
                } while (captchaurl != null && counter <= 4);
                if (captchaurl != null) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
            }
            dllink = PluginJSonUtils.getJson(this.br, "redirectUrl");
            if (StringUtils.isEmpty(dllink)) {
                if (br.containsHTML("payment_window")) {
                    /* User needs to use an account and/or buy traffic for his existing account to download this file. */
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = unescape(dllink);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html") && dl.getConnection().getResponseCode() != 206) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("directlink", dllink);
        dl.startDownload();
    }

    private String getCaptchaURL() {
        return this.br.getRegex("(/captcha\\.axd\\?ts=\\d+)").getMatch(0);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    private static final String MAINPAGE = "http://minhateca.com.br";
    private static Object       LOCK     = new Object();

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
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
                br.getPage(MAINPAGE + "/");
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                String req_token = br.getRegex("name=\"__RequestVerificationToken\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
                if (req_token == null) {
                    req_token = "undefined";
                }
                br.postPage("http://minhateca.com.br/action/login/loginWindow", "Redirect=true&__RequestVerificationToken=" + req_token);
                br.postPageRaw("/action/login/login", "RememberMe=true&RememberMe=false&__RequestVerificationToken=" + req_token + "&RedirectUrl=&Redirect=True&FileId=0&Login=" + Encoding.urlEncode(account.getUser()) + "&Password=" + Encoding.urlEncode(account.getPass()));
                if (br.getCookie(MAINPAGE, "RememberMe") == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                /* Only premium accounts are supported so far */
                account.setProperty("free", false);
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(MAINPAGE);
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

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        /* reset maxPrem workaround on every fetchaccount info */
        maxPrem.set(1);
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        ai.setUnlimitedTraffic();
        this.br.getPage("/action/payments");
        /* Only premium accounts are supported so far */
        /* Browser contains message like "If you want to download files bigger than X MB buy premium" --> We have a free account */
        if (this.br.containsHTML("você vai precisar de créditos de download")) {
            maxPrem.set(ACCOUNT_FREE_MAXDOWNLOADS);
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(maxPrem.get());
            account.setConcurrentUsePossible(false);
            ai.setStatus("Registered (free) user");
        } else {
            maxPrem.set(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(maxPrem.get());
            account.setConcurrentUsePossible(true);
            ai.setStatus("Premium User");
        }
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.getPage(link.getStringProperty("mainlink", null));
        if (this.br.getURL().contains("/action/Tutorial")) {
            /* No traffic left and/or user tries to download oversized file via free account! */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\t\nNo traffic left", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
        if (account.getType() == AccountType.FREE) {
            doFree(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
        } else {
            doFree(link, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS, "account_premium_directlink");
        }
    }

    private void handlePWProtected(final DownloadLink dl) throws PluginException, IOException {
        String passCode = dl.getStringProperty("pass", null);
        if (br.containsHTML("class=\"LoginToFolderForm\"")) {
            final String reqtoken = br.getRegex("name=\"__RequestVerificationToken\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
            final String chomikid = br.getRegex("type=\"hidden\" name=\"ChomikId\" value=\"(\\d+)\"").getMatch(0);
            final String folderid = br.getRegex("name=\"FolderId\" type=\"hidden\" value=\"(\\d+)\"").getMatch(0);
            final String foldername = br.getRegex("id=\"FolderName\" name=\"FolderName\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
            if (reqtoken == null || chomikid == null || folderid == null || foldername == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            boolean success = false;
            for (int i = 1; i <= 3; i++) {
                if (passCode == null) {
                    passCode = Plugin.getUserInput("Password?", dl);
                }
                br.postPageRaw("http://" + this.getHost() + "/action/Files/LoginToFolder", "Remember=true&Remember=false&ChomikId=" + chomikid + "&FolderId=" + folderid + "&FolderName=" + Encoding.urlEncode(foldername) + "&Password=" + Encoding.urlEncode(passCode) + "&__RequestVerificationToken=" + Encoding.urlEncode(reqtoken));
                if (br.containsHTML("\"IsSuccess\":false")) {
                    passCode = null;
                    dl.setProperty("pass", Property.NULL);
                    continue;
                }
                success = true;
                break;
            }
            if (!success) {
                throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
            }
            /* We don't want to work with the encoded json bla html response */
            br.getPage(dl.getStringProperty("mainlink", null));
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /* workaround for free/premium issue on stable 09581 */
        return maxPrem.get();
    }

    private static synchronized String unescape(final String s) {
        /* we have to make sure the youtube plugin is loaded */
        if (pluginloaded == false) {
            final PluginForHost plugin = JDUtilities.getPluginForHost("youtube.com");
            if (plugin == null) {
                throw new IllegalStateException("youtube plugin not found!");
            }
            pluginloaded = true;
        }
        return jd.nutils.encoding.Encoding.unescapeYoutube(s);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
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
        if (Boolean.TRUE.equals(acc.getBooleanProperty("nopremium"))) {
            /* free accounts also have captchas */
            return true;
        }
        if (acc.getStringProperty("session_type") != null && !"premium".equalsIgnoreCase(acc.getStringProperty("session_type"))) {
            return true;
        }
        return false;
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "ENABLE_MP3_STREAM_DOWNLOAD", "Enable mp3 stream download?\r\nThis may skip captcha for audio files but the quality can be worse than via official download and cover inside the audio file is missing.").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "ENABLE_MP4_STREAM_DOWNLOAD", "Enable mp4 stream download?\r\nThis may skip captcha for video files but the quality is usually worse than via official download.").setDefaultValue(true));
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.ChomikujPlScript;
    }
}
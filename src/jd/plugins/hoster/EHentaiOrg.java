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
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.http.requests.HeadRequest;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.UserAgents;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "e-hentai.org" }, urls = { "^https?://(?:www\\.)?(?:(?:g\\.)?e-hentai\\.org|exhentai\\.org)/s/[a-f0-9]{10}/(\\d+)-(\\d+)$" })
public class EHentaiOrg extends PluginForHost {

    @Override
    public String rewriteHost(String host) {
        if (host == null || "exhentai.org".equals(host) || "e-hentai.org".equals(host)) {
            return "e-hentai.org";
        }
        return super.rewriteHost(host);
    }

    public EHentaiOrg(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://exhentai.org/");
        setConfigElements();
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:

    /* Connection stuff */
    private static final boolean        free_resume             = true;
    /* Limit chunks to 1 as we only download small files */
    private static final int            free_maxchunks          = 1;
    private static final int            free_maxdownloads       = -1;

    private static final long           minimal_filesize        = 1000;

    private String                      dllink                  = null;
    private boolean                     server_issues           = false;
    private final boolean               ENABLE_RANDOM_UA        = true;
    private static final String         PREFER_ORIGINAL_QUALITY = "PREFER_ORIGINAL_QUALITY";

    private static final String         TYPE_EXHENTAI           = "exhentai\\.org";
    private final LinkedHashSet<String> dupe                    = new LinkedHashSet<String>();

    private String                      uid_chapter             = null;
    private String                      uid_page                = null;

    @Override
    public String getAGBLink() {
        return "http://g.e-hentai.org/tos.php";
    }

    @Override
    public boolean canHandle(final DownloadLink downloadLink, final Account account) throws Exception {
        if (account == null && new Regex(downloadLink.getDownloadURL(), TYPE_EXHENTAI).matches()) {
            return false;
        }
        return super.canHandle(downloadLink, account);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        return requestFileInformation(downloadLink, null);
    }

    /**
     * take account from download candidate!
     *
     * @param downloadLink
     * @param account
     * @return
     * @throws Exception
     */
    private AvailableStatus requestFileInformation(final DownloadLink downloadLink, final Account account) throws Exception {
        /* from manual 'online check', we don't want to 'try' as it uses up quota... */
        if (account == null && new Regex(downloadLink.getDownloadURL(), TYPE_EXHENTAI).matches()) {
            return AvailableStatus.UNCHECKABLE;
        }
        // nullfication
        dupe.clear();
        dllink = null;
        String dllink_fullsize = null;
        boolean loggedin = false;
        // uids
        uid_chapter = new Regex(downloadLink.getDownloadURL(), this.getSupportedLinks()).getMatch(0);
        uid_page = new Regex(downloadLink.getDownloadURL(), this.getSupportedLinks()).getMatch(1);

        final String mainlink = getMainlink(downloadLink);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        if (account != null) {
            try {
                login(this.br, account, false);
                loggedin = true;
            } catch (final Throwable e) {
                loggedin = false;
            }
        }
        if (ENABLE_RANDOM_UA) {
            /*
             * Using a different UA for every download might be a bit obvious but at the moment, this fixed the error-server responses as it
             * tricks it into thinking that we re a lot of users and not only one.
             */
            br.getHeaders().put("User-Agent", UserAgents.stringUserAgent());
        }
        br.getPage(mainlink);
        if (br.toString().matches("Your IP address has been temporarily banned for excessive pageloads.+")) {
            if (account == null) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Your IP address has been temporarily banned for excessive pageloads");
            }
            String tmpYears = new Regex(br, "(\\d+)\\s+years?").getMatch(0);
            String tmpdays = new Regex(br, "(\\d+)\\s+days?").getMatch(0);
            String tmphrs = new Regex(br, "(\\d+)\\s+hours?").getMatch(0);
            String tmpmin = new Regex(br, "(\\d+)\\s+minutes?").getMatch(0);
            String tmpsec = new Regex(br, "(\\d+)\\s+seconds?").getMatch(0);
            long years = 0, days = 0, hours = 0, minutes = 0, seconds = 0;
            if (StringUtils.isEmpty(tmpYears)) {
                years = Integer.parseInt(tmpYears);
            }
            if (StringUtils.isEmpty(tmpdays)) {
                days = Integer.parseInt(tmpdays);
            }
            if (StringUtils.isEmpty(tmphrs)) {
                hours = Integer.parseInt(tmphrs);
            }
            if (StringUtils.isEmpty(tmpmin)) {
                minutes = Integer.parseInt(tmpmin);
            }
            if (StringUtils.isEmpty(tmpsec)) {
                seconds = Integer.parseInt(tmpsec);
            }
            long expireS = ((years * 86400000 * 365) + (days * 86400000) + (hours * 3600000) + (minutes * 60000) + (seconds * 1000)) + System.currentTimeMillis();
            throw new PluginException(PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE, "Your IP address has been temporarily banned for excessive pageloads", expireS);
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String namepart = getNamePart(downloadLink);
        if (loggedin && this.getPluginConfig().getBooleanProperty(PREFER_ORIGINAL_QUALITY, default_PREFER_ORIGINAL_QUALITY)) {
            /* Try to get fullsize (original) image. */
            final Regex fulllinkinfo = br.getRegex("href=\"(https?://(?:(?:g\\.)?e\\-hentai|exhentai)\\.org/fullimg\\.php[^<>\"]*?)\">Download original \\d+ x \\d+ ([^<>\"]*?) source</a>");
            dllink_fullsize = fulllinkinfo.getMatch(0);
            final String html_filesize = fulllinkinfo.getMatch(1);
            if (dllink_fullsize != null && html_filesize != null) {
                downloadLink.setDownloadSize(SizeFormatter.getSize(html_filesize));
            }
        }
        getDllink(account);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String ext = getFileNameExtensionFromString(dllink, ".png");
        // package customiser altered, or user altered value, we need to update this value.
        if (downloadLink.getForcedFileName() != null) {
            downloadLink.setForcedFileName(namepart + ext);
        } else {
            // decrypter doesn't set file extension.
            downloadLink.setFinalFileName(namepart + ext);
        }

        if (dllink_fullsize != null) {
            dllink_fullsize = Encoding.htmlDecode(dllink_fullsize);
            /* Filesize is already set via html_filesize, we have our full (original) resolution downloadlink and our file extension! */
            dllink = dllink_fullsize;
            return AvailableStatus.TRUE;
        }
        if (dllink != null) {
            while (true) {
                if (!dupe.add(dllink)) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                final Browser br2 = br.cloneBrowser();
                // In case the link redirects to the finallink
                br2.setFollowRedirects(true);
                URLConnectionAdapter con = null;
                try {
                    try {
                        con = br2.openHeadConnection(dllink);
                    } catch (final BrowserException ebr) {
                        // socket issues, lets try another mirror also.
                        final String[] failed = br.getRegex("onclick=\"return ([a-z]+)\\(\\'(\\d+-\\d+)\\'\\)\">Click here if the image fails loading</a>").getRow(0);
                        if (failed != null && failed.length == 2) {
                            br.getPage(br.getURL() + "?" + failed[0] + "=" + failed[1]);
                            getDllink(account);
                            if (dllink != null) {
                                continue;
                            } else {
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            }
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        /* Whatever happens - its most likely a server problem for this host! */
                        // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
                    }
                    if (con.getResponseCode() == 404) {
                        // we can try another mirror
                        final String[] failed = br.getRegex("onclick=\"return ([a-z]+)\\('(\\d+-\\d+)'\\)\">Click here if the image fails loading</a>").getRow(0);
                        if (failed != null && failed.length == 2) {
                            br.getPage(br.getURL() + "?" + failed[0] + "=" + failed[1]);
                            getDllink(account);
                            if (dllink != null) {
                                continue;
                            } else {
                                /* Failed */
                                break;
                            }
                        } else {
                            /* Failed */
                            break;
                        }
                    }
                    final long conlength = con.getLongContentLength();
                    if (!con.getContentType().contains("html") && conlength > minimal_filesize) {
                        downloadLink.setDownloadSize(conlength);
                        downloadLink.setProperty("directlink", dllink);
                        return AvailableStatus.TRUE;
                    } else {
                        return AvailableStatus.UNCHECKABLE;
                    }
                } finally {
                    if (con != null) {
                        if (con.getRequest() instanceof HeadRequest) {
                            br2.loadConnection(con);
                        } else {
                            try {
                                con.disconnect();
                            } catch (final Throwable e) {
                            }
                        }
                    }
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    private void getDllink(final Account account) throws Exception {
        // g.e-hentai.org = free non account
        // error
        // <div id="i3"><a onclick="return load_image(94, '00ea7fd4e0')" href="http://g.e-hentai.org/s/00ea7fd4e0/348501-94"><img id="img"
        // src="http://ehgt.org/g/509.gif" style="margin:20px auto" /></a></div>
        // working
        // <div id="i3"><a onclick="return load_image(94, '00ea7fd4e0')" href="http://g.e-hentai.org/s/00ea7fd4e0/348501-94"><img id="img"
        // src="http://153.149.98.104:65000/h/40e8a3da0fac1b0ec40b5c58489f7b8d46b1a2a2-436260-1200-1600-jpg/keystamp=1469074200-e1ec68e0ef/093.jpg"
        // style="height:1600px;width:1200px" /></a></div>
        // error (no div id=i3, no a onclick either...) Link; 0957971887641.log; 57438449; jdlog://0957971887641
        // <a href="http://g.e-hentai.org/s/4bf901e9e6/957224-513"><img src="http://ehgt.org/g/509.gif" style="margin:20px auto" /></a>
        // working
        // ...

        // exhentai.org = account
        // error
        // <div id="i3"><a onclick="return load_image(26, '2fb043446a')" href="http://exhentai.org/s/2fb043446a/706165-26"><img id="img"
        // src="http://exhentai.org/img/509.gif" style="margin:20px auto" /></a></div>
        // working
        // <div id="i3"><a onclick="return load_image(54, 'cd7295ee9c')" href="http://exhentai.org/s/cd7295ee9c/940613-54"><img id="img"
        // src="http://130.234.205.178:25565/h/f21818f4e9d04169de22f31407df68da84f30719-935516-1273-1800-jpg/keystamp=1468656900-b9873b14ab/ow_013.jpg"
        // style="height:1800px;width:1273px" /></a></div>

        // best solution is to apply cleanup?
        final String b = br.toString();
        String cleanup = new Regex(b, "<iframe[^>]*>(.*?)<iframe").getMatch(0);
        if (cleanup == null) {
            cleanup = new Regex(b, "<div id=\"i3\">(.*?)</div").getMatch(0);
        }

        dllink = new Regex(cleanup, "<img [^>]*src=(\"|')([^\"\\'<>]*?)\\1").getMatch(1);
        if (dllink == null) {
            /* 2017-01-30: Until now only jp(e)g was allowed, now also png. */
            dllink = new Regex(b, "<img [^>]*src=(\"|')([^\"\\'<>]{30,}(?:\\.jpe?g|png|gif))\\1").getMatch(1);
        }
        // ok so we want to make sure it isn't 509.gif
        final String filename = extractFileNameFromURL(dllink);
        if (filename != null && filename.equals("509.gif")) {
            if (account == null) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 2 * 60 * 60 * 1000l);
            } else {
                br.getPage("http://exhentai.org/home.php");
                if (account != null) { // todo: ensure this works?
                    saveCookies(br, account);
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "509.gif", 2 * 60 * 1000l);
                }
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink, null);
        doFree(downloadLink, null);
    }

    @SuppressWarnings("deprecation")
    private void doFree(final DownloadLink downloadLink, final Account account) throws Exception {
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (downloadLink.getDownloadSize() < minimal_filesize) {
            /* E.h. "403 picture" is smaller than 1 KB */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error - file is too small", 2 * 60 * 1000l);
        } else if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        }
        try {
            dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, free_resume, free_maxchunks);
        } catch (final BrowserException ebr) {
            /* Whatever happens - its most likely a server problem for this host! */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            if (br.containsHTML("¿You have exceeded your image viewing limits\\. Note that you can reset these limits by going")) {
                br.getPage("http://exhentai.org/home.php");
                if (account != null) { // todo: ensure this works?
                    saveCookies(br, account);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                } else {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 2 * 60 * 60 * 1000l);
                }
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private static final String MAINPAGE = "http://e-hentai.org";
    private static Object       LOCK     = new Object();

    @SuppressWarnings("unchecked")
    public void login(final Browser br, final Account account, final boolean force) throws Exception {
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
                        for (final Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(MAINPAGE, key, value);
                            /* Important! */
                            br.setCookie("http://exhentai.org/", key, value);
                        }
                        return;
                    }
                }
                boolean failed = true;
                br.setFollowRedirects(true);
                br.getPage("https://forums.e-hentai.org/index.php?act=Login&CODE=01");
                for (int i = 0; i <= 3; i++) {
                    final Form loginform = br.getFormbyKey("CookieDate");
                    if (loginform == null) {
                        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                    }
                    loginform.put("UserName", account.getUser());
                    loginform.put("PassWord", account.getPass());
                    if (i > 0 && br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                        /* First login try failed and we get a captcha --> Does not necessarily mean that user entered wrong logindata! */
                        final Recaptcha rc = new Recaptcha(br, this);
                        rc.findID();
                        rc.load();
                        final DownloadLink dummyLink = new DownloadLink(this, "Account", "e-hentai.org", "http://e-hentai.org", true);
                        final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                        final String c = getCaptchaCode("recaptcha", cf, dummyLink);
                        loginform.put("recaptcha_challenge_field", rc.getChallenge());
                        loginform.put("recaptcha_response_field", c);
                    }
                    br.submitForm(loginform);
                    failed = br.getCookie(MAINPAGE, "ipb_pass_hash") == null;
                    if (!failed) {
                        break;
                    }
                }
                if (failed) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                br.getPage("http://exhentai.org/");
                saveCookies(br, account);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    private void saveCookies(final Browser br, final Account account) {
        synchronized (LOCK) {
            // Save cookies
            final HashMap<String, String> cookies = new HashMap<String, String>();
            final Cookies add = br.getCookies(MAINPAGE);
            for (final Cookie c : add.getCookies()) {
                cookies.put(c.getKey(), c.getValue());
            }
            account.setProperty("name", Encoding.urlEncode(account.getUser()));
            account.setProperty("pass", Encoding.urlEncode(account.getPass()));
            account.setProperty("cookies", cookies);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(this.br, account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        ai.setUnlimitedTraffic();
        account.setType(AccountType.PREMIUM);
        account.setConcurrentUsePossible(true);
        ai.setStatus("Premium Account");
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, account);
        /* No need to login here as we already logged in in availablecheck */
        doFree(link, account);
    }

    private String getMainlink(final DownloadLink dl) {
        final String link = dl.getStringProperty("individual_link", null);
        if (link != null) {
            return link;
        } else {
            return dl.getDownloadURL();
        }
    }

    private String getNamePart(DownloadLink downloadLink) throws PluginException {
        // package customiser sets filename to this value
        final String userFilename = downloadLink.getForcedFileName();
        if (userFilename != null) {
            // make sure you remove the existing extension!
            final String ext = getFileNameExtensionFromString(userFilename, null);
            return userFilename != null ? userFilename.replaceFirst(Pattern.quote(ext) + "$", "") : userFilename;
        }
        final String namelink = downloadLink.getStringProperty("namepart", null);
        if (namelink != null) {
            // return what's from decrypter as gospel.
            return namelink;
        }
        // link has added in a single manner outside of decrypter, so we need to construct!
        final DecimalFormat df = new DecimalFormat("0000");
        // we can do that based on image part
        final String[] uidPart = new Regex(downloadLink.getDownloadURL(), "/(\\d+)-(\\d+)$").getRow(0);

        final String fpName = getTitle(br);
        if (fpName == null || uidPart == null || uidPart.length != 2) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String title = HTMLEntities.unhtmlentities(fpName) + "_" + uidPart[0] + "-" + df.format(Integer.parseInt(uidPart[1]));
        return title;
    }

    public String getTitle(final Browser br) {
        final String fpName = br.getRegex("<title>([^<>\"]*?)(?:\\s*-\\s*E-Hentai Galleries|\\s*-\\s*ExHentai\\.org)?</title>").getMatch(0);
        return fpName;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return free_maxdownloads;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
    }

    private final boolean default_PREFER_ORIGINAL_QUALITY = true;

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PREFER_ORIGINAL_QUALITY, JDL.L("plugins.hoster.EHentaiOrg.DownloadZip", "Account only: Prefer original quality (bigger filesize, higher resolution)?")).setDefaultValue(default_PREFER_ORIGINAL_QUALITY));
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}

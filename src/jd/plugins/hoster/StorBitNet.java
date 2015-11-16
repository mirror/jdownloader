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
import java.util.Locale;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
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
import jd.plugins.PluginForHost;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "storbit.net" }, urls = { "https?://(www\\.)?(?:uploads\\.xxx|streambit\\.tv|storbit\\.net)/(?:video|file)/[A-Za-z0-9\\-_]+/" }, flags = { 2 })
public class StorBitNet extends PluginForHost {

    public StorBitNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://storbit.net/premium/");
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("https?://(www\\.)?[^/]+/(?:video|file)/", "http://storbit.net/file/"));
    }

    @Override
    public String getAGBLink() {
        return "http://storbit.net/rules/";
    }

    private static final String  API_SERVER                      = "http://storbit.net/api";

    /* Connection stuff */
    private static final boolean FREE_RESUME                     = false;
    private static final int     FREE_MAXCHUNKS                  = 1;
    private static final int     FREE_MAXDOWNLOADS               = 1;
    private static final boolean ACCOUNT_FREE_RESUME             = false;
    private static final int     ACCOUNT_FREE_MAXCHUNKS          = 1;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS       = 1;
    private static final boolean ACCOUNT_PREMIUM_RESUME          = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS       = 0;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS    = 20;

    /* Properties */
    private static final String  PROPERTY_DLLINK_FREE            = "freelink";
    private static final String  PROPERTY_DLLINK_ACCOUNT_FREE    = "freelink2";
    private static final String  PROPERTY_DLLINK_ACCOUNT_PREMIUM = "premlink";

    private static final boolean USE_API_SINGLE_FILECHECK        = true;

    private void prepBR_API(final Browser br) {
        br.getHeaders().put("User-Agent", "JDownloader");
    }

    private void prepBR_Website(final Browser br) {
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:40.0) Gecko/20100101 Firefox/40.0");
        br.setCookie(this.getHost(), "xxx_lang", "en");
    }

    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) {
            return false;
        }
        try {
            final Browser br = new Browser();
            prepBR_API(br);
            br.setCookiesExclusive(true);
            final StringBuilder sb = new StringBuilder();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    /* we test 50 links at once (50 tested, more might be possible) */
                    if (index == urls.length || links.size() > 50) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                sb.delete(0, sb.capacity());
                sb.append("id=");
                for (final DownloadLink dl : links) {
                    final String tmp_fid = this.getFID(dl);
                    dl.setLinkID(tmp_fid);
                    sb.append(tmp_fid);
                    sb.append("%2C");
                }
                br.postPage(API_SERVER + "/getFileDetails/", sb.toString());
                for (final DownloadLink dllink : links) {
                    final String fid = getFID(dllink);
                    final String linkdata = br.getRegex("(\\{\"fileStatus\":\\d+,\"fileId\":\"" + fid + "\"[^\\}]*\\})").getMatch(0);
                    if (linkdata == null) {
                        /* Should never happen */
                        dllink.setName(fid);
                        dllink.setAvailable(false);
                    } else {
                        final String ftitle = getJson(linkdata, "fileName");
                        final String fsize = getJson(linkdata, "fileSize");
                        if (ftitle == null || fsize == null) {
                            dllink.setName(fid);
                            logger.warning("Linkchecker broken for " + this.getHost());
                            return false;
                        }
                        dllink.setAvailable(true);
                        dllink.setFinalFileName(ftitle);
                        dllink.setDownloadSize(Long.parseLong(fsize));
                    }
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            return false;
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        if (USE_API_SINGLE_FILECHECK) {
            checkLinks(new DownloadLink[] { downloadLink });
            if (!downloadLink.isAvailabilityStatusChecked()) {
                return AvailableStatus.UNCHECKED;
            }
            if (downloadLink.isAvailabilityStatusChecked() && !downloadLink.isAvailable()) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } else {
            this.setBrowserExclusive();
            prepBR_Website(this.br);
            br.getPage(downloadLink.getDownloadURL());
            if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">404 - File not found|>Sorry, but the specified file may have been deleted")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String filename = br.getRegex("h1 title=\"([^<>\"]*?)\"").getMatch(0);
            if (filename == null) {
                String format = br.getRegex(">Format: <b>([^<>\"]*?)</b>").getMatch(0);
                filename = br.getRegex("class=\"title\">[\t\n\r ]+<h\\d+ title=\"([^<>\"]*?)\"").getMatch(0);
                if (filename == null || format == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                filename += "." + format;
            }
            String filesize = br.getRegex(">Size: <b>([^<>\"]+)<").getMatch(0);
            if (filesize == null) {
                filesize = br.getRegex("class=\"size\">([^<>\"]*?)<").getMatch(0);
            }
            if (filename == null || filesize == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            downloadLink.setFinalFileName(Encoding.htmlDecode(filename.trim()));
            downloadLink.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFreeWebsite(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    @SuppressWarnings("static-access")
    private void doFreeWebsite(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        this.prepBR_Website(this.br);
        String dllink = checkDirectLink(downloadLink, directlinkproperty);
        if (dllink == null) {
            final String fid = this.getFID(downloadLink);
            br.getPage(MAINPAGE + "/play/" + fid + "/");
            final String streamlink = br.getRegex("file: \\'(http://[^<>\"\\']*?)\\'").getMatch(0);
            /* video/xxx.mp4 = conversion in progress, video/lock.mp4 = IP_blocked */
            if (streamlink == null || streamlink.contains("video/xxx.mp4") || streamlink.contains("video/lock.mp4")) {
                // if (br.containsHTML("/img/streaming\\.jpg")) {
                // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Converting video in progress ...");
                // }
                this.br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                this.br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                this.br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.postPage("/ajax.php?a=getDownloadForFree", "id=" + fid + "&_go=");
                if (br.containsHTML("\"message\":\"error\"")) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
                }
                final Recaptcha rc = new Recaptcha(br, this);
                rc.setId("6Lc4YwgTAAAAAPoZXXByh65cUKulPwDN31HlV1Wp");
                for (int i = 0; i < 5; i++) {
                    rc.load();
                    final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    final String c = getCaptchaCode("recaptcha", cf, downloadLink);
                    br.postPage("/ajax.php?a=getDownloadLink", "captcha1=" + Encoding.urlEncode(rc.getChallenge()) + "&captcha2=" + Encoding.urlEncode(c) + "&id=" + fid + "&_go=");
                    if (br.containsHTML("\"message\":\"error\"")) {
                        rc.reload();
                        continue;
                        // throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                    break;
                }
                dllink = getJson("location");
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Wrong Captcha code in 5 trials OR final download link not found!", 1 * 60 * 1000l);
                }
                if (dllink.contains("http://.streambit.tv/")) {
                    /* We get crippled downloadlinks if the user enters "" as captcha response */
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
            } else {
                /* Prefer streams as we can avoid the captcha though the quality does not match the originally uploaded content. */
                dllink = streamlink;
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
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

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    private static final String MAINPAGE = "http://storbit.net";
    private static Object       LOCK     = new Object();

    /* Keep this - might be needed in the future e.g. for free accounts. */
    @SuppressWarnings("unused")
    private void loginWebsite(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                br.setFollowRedirects(false);
                if (cookies != null && !force) {
                    this.br.setCookies(this.getHost(), cookies);
                    return;
                }
                br.postPage(MAINPAGE + "/ajax.php?a=getUserLogin", "login=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&remember=0&_go=");

                String success = getJson("message");
                if (!"success".equals(success)) {
                    logger.warning("Couldn't determine premium status or account is Free not Premium!");
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    private void loginAPI(final Account acc) throws IOException, PluginException {
        this.accessAPI(API_SERVER + "/getAccountDetails/", "login=" + Encoding.urlEncode(acc.getUser()) + "&password=" + Encoding.urlEncode(acc.getPass()));
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            loginAPI(account);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        final boolean isPremium = "1".equals(getJson("userPremium"));
        if (isPremium) {
            final String expiredate = getJson("userPremiumDateEnd");
            account.setType(AccountType.PREMIUM);
            /* Do not set any specified max simultan downloads num on premium accounts */
            // account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            /* Simultaneous usage of multiple premium accounts should be possible! */
            account.setConcurrentUsePossible(true);
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expiredate, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH));
            ai.setStatus("Premium account");
        } else {
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
            /* Free accounts have more likely strict limits and can have captchas */
            account.setConcurrentUsePossible(false);
            ai.setStatus("Registered (free) user");
        }
        ai.setUnlimitedTraffic();
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        /* Make sure to use the API headers in case we use the website-availablecheck */
        prepBR_API(this.br);
        String dllink;
        String directlinkproperty;
        boolean resumable;
        int maxchunks;
        final boolean isPremium = account.getType() == AccountType.PREMIUM;
        if (isPremium) {
            directlinkproperty = PROPERTY_DLLINK_ACCOUNT_PREMIUM;
            resumable = ACCOUNT_PREMIUM_RESUME;
            maxchunks = ACCOUNT_PREMIUM_MAXCHUNKS;
        } else {
            directlinkproperty = PROPERTY_DLLINK_ACCOUNT_FREE;
            resumable = ACCOUNT_FREE_RESUME;
            maxchunks = ACCOUNT_FREE_MAXCHUNKS;
        }
        dllink = this.checkDirectLink(link, directlinkproperty);
        if (dllink == null) {
            this.accessAPI(API_SERVER + "/getDownloadFile/", "login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&id=" + this.getFID(link));
            dllink = getJson("fileLocation");
            if (dllink == null) {
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
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

    private void accessAPI(final String url, final String postdata) throws IOException, PluginException {
        this.br.postPage(url, postdata);
        if (this.br.containsHTML("errorEmptyLoginOrPassword|errorAccountNotFound")) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername/Passwort oder nicht unterstützter Account Typ!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password or unsupported account type!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        } else if (this.br.containsHTML("errorEmptyFileId")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "FATAL API failure");
        } else if (this.br.containsHTML("errorFileNotFound")) {
            /* File not found on (premium) download attempt */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (this.br.containsHTML("errorDateNextDownload")) {
            /* Free download limit reached TODO: implement */
            long wait = 0;
            final String wait_until_date = getJson("errorDateNextDownload");
            if (wait_until_date != null) {
                // 2015-09-21 22:40:20
                final long wait_until_date_long = TimeFormatter.getMilliSeconds(wait_until_date, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
                if (wait_until_date_long > System.currentTimeMillis()) {
                    wait = wait_until_date_long - System.currentTimeMillis();
                }
            }
            if (wait == 0) {
                /* Simple fallback to default free (account) 'limit reached'-waittime - 3 hours. */
                wait = 3 * 60 * 60 * 1001l;
            }
            /* 2015-09-23: Yes, IP changes will actually also remove this host side free account limit */
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wait);
        }
    }

    @SuppressWarnings("deprecation")
    private String getFID(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "/([^/]+)/$").getMatch(0);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_PREMIUM_MAXDOWNLOADS;
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from String source.
     *
     * @author raztoki
     * */
    private String getJson(final String source, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(source, key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     * */
    private String getJson(final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(br.toString(), key);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
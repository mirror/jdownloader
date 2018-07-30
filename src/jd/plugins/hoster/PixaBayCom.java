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
import java.util.HashMap;
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
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pixabay.com" }, urls = { "https?://(www\\.)?pixabay\\.com/en/[a-z0-9\\-]+\\-\\d+/" })
public class PixaBayCom extends PluginForHost {
    public PixaBayCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://pixabay.com/en/accounts/register/");
    }

    @Override
    public String getAGBLink() {
        return "https://pixabay.com/en/service/terms/";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME               = false;
    private static final int     FREE_MAXCHUNKS            = 1;
    private static final int     FREE_MAXDOWNLOADS         = 20;
    private static final boolean ACCOUNT_FREE_RESUME       = false;
    private static final int     ACCOUNT_FREE_MAXCHUNKS    = 1;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS = 20;
    /* TODO: Maybe add support for SVG format(s) */
    private final String[]       qualities                 = { "Original", "O", "XXL", "XL", "L", "M", "S" };
    private String               quality_max               = null;
    private String               quality_download_id       = null;
    /* don't touch the following! */
    private static AtomicInteger maxPrem                   = new AtomicInteger(1);

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("http://", "https://"));
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa != null) {
            this.login(aa, false);
        }
        br.getPage(link.getDownloadURL());
        if (this.br.getHttpConnection().getResponseCode() == 404 || this.br.getURL().contains("?")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<title>Free photo: ([^<>]*?)</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>([^<>]*?)</title>").getMatch(0);
        }
        /* Find filesize based on whether user has an account or not. */
        String filesize = null;
        boolean done = false;
        final String[] qualityInfo = br.getRegex("(<td><input type=\"radio\" name=\"download\".*?/td></tr>)").getColumn(0);
        for (final String possiblequality : qualities) {
            for (final String quality : qualityInfo) {
                // logger.info("quality: " + quality);
                final String quality_name = new Regex(quality, "(ORIGINAL|O|S|M|L|XXL|XL|SVG)</td>").getMatch(0);
                if (quality_name == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final boolean accountQualityPossible = aa != null && (quality_name.equalsIgnoreCase("XXL") || quality_name.equalsIgnoreCase("XL") || quality_name.equalsIgnoreCase("O") || quality_name.equalsIgnoreCase("Original"));
                final boolean isNoAccountQuality = !quality_name.equalsIgnoreCase("XXL") && !quality_name.equalsIgnoreCase("XL") && !quality_name.equalsIgnoreCase("O") && !quality_name.equalsIgnoreCase("Original");
                if (quality_name.equals(possiblequality) && (accountQualityPossible || isNoAccountQuality)) {
                    done = true;
                    filesize = new Regex(quality, "class=\"hide-xs hide-md\">([^<>\"]*?)<").getMatch(0);
                    if (filesize == null) {
                        filesize = new Regex(quality, ">(\\d+(?:\\.\\d+)? (?:kB|mB|gB))<").getMatch(0);
                    }
                    quality_max = new Regex(quality, ">(\\d+)\\s*(?:×|x)\\s*\\d+<").getMatch(0);
                    quality_download_id = new Regex(quality, "([^<>\"/]*?\\.jpg)").getMatch(0);
                    if (quality_download_id == null) {
                        quality_download_id = new Regex(quality, "name=\"download\" value=\"([^<>\"]*?)\"").getMatch(0);
                    }
                    break;
                }
            }
            if (done) {
                break;
            }
        }
        if (filesize == null || quality_download_id == null) {
            String dllink = br.getRegex("(https://cdn[^<>\"\\s]+) 1.333x").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("(https://cdn[^<>\"\\s]+) 1x").getMatch(0);
            }
            if (dllink != null) {
                link.setProperty("free_directlink", dllink);
                filename = Encoding.htmlDecode(filename.trim());
                filename = encodeUnicode(filename) + ".jpg";
                link.setFinalFileName(filename);
                return AvailableStatus.TRUE;
            }
        }
        if (filename == null || filesize == null || quality_download_id == null) {
            logger.info("filename: " + filename + ", filesize: " + filesize + ", quality_download_id: " + quality_download_id);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (quality_max == null) {
            quality_max = "10000";
        }
        filename = Encoding.htmlDecode(filename.trim());
        filename = encodeUnicode(filename) + ".jpg";
        link.setFinalFileName(filename);
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    @SuppressWarnings("static-access")
    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        String dllink = checkDirectLink(downloadLink, directlinkproperty);
        if (dllink == null) {
            final String post_url = "https://pixabay.com/en/photos/download/" + quality_download_id + "?attachment&modal";
            this.br.getPage(post_url);
            final String timestamp = this.br.getRegex("name=\"timestamp\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
            final String security_hash = this.br.getRegex("name=\"security_hash\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
            if (timestamp == null || security_hash == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            this.br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            final Recaptcha rc = new Recaptcha(br, this);
            /* Last updated: 2015-08-17 */
            rc.setId("6Ld8hL8SAAAAAKbydL06Ir20hG_u2SfRkBbfpTNf");
            rc.load();
            final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            final String c = getCaptchaCode("recaptcha", cf, downloadLink);
            this.br.postPage(post_url, "topyenoh=&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c) + "&timestamp=" + timestamp + "&security_hash=" + security_hash + "&attachment=1");
            dllink = br.getRegex("window\\.location=\"(/[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = "https://pixabay.com" + dllink;
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
                if (isJDStable()) {
                    con = br2.openGetConnection(dllink);
                } else {
                    con = br2.openHeadConnection(dllink);
                }
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

    private boolean isJDStable() {
        return System.getProperty("jd.revision.jdownloaderrevision") == null;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    private static final String MAINPAGE = "http://pixabay.com";
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
                br.setFollowRedirects(true);
                br.postPage("https://pixabay.com/en/accounts/login/", "next=%2Fen%2Faccounts%2Fmedia%2F&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                if (!this.br.containsHTML("/accounts/logout/\"")) {
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
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        ai.setUnlimitedTraffic();
        maxPrem.set(ACCOUNT_FREE_MAXDOWNLOADS);
        try {
            account.setType(AccountType.FREE);
            /* Free accounts cannot have captchas */
            account.setMaxSimultanDownloads(maxPrem.get());
            account.setConcurrentUsePossible(true);
        } catch (final Throwable e) {
            /* not available in old Stable 0.9.581 */
        }
        ai.setStatus("Registered (free) user");
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        /* Login not needed here as we already logged in above. */
        String dllink = this.br.getRegex("name=\"download\" value=\"([^<>\"]*?)\" data-perm=\"auth\"").getMatch(0);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = "https://pixabay.com/en/photos/download/" + dllink + "?attachment";
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else if (this.br.containsHTML("\"fdiv_recaptcha\"|google\\.com/recaptcha/help")) {
                /* If a user downloads too much, he might get asked to enter captchas in premium mode --> Wait to get around this problem. */
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Captcha needed - wait some time until you can download again", 10 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty("premium_directlink", dllink);
        dl.startDownload();
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
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

import java.io.File;
import java.io.IOException;
import java.util.Random;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "datpiff.com" }, urls = { "http://(www\\.)?datpiff\\.com/([^<>\"% ]*?\\-download(\\-track)?\\.php\\?id=[a-z0-9]+|mixtapes\\-detail\\.php\\?id=\\d+|.*?\\-mixtape\\.\\d+\\.html)" })
public class DatPiffCom extends PluginForHost {
    private static final String PREMIUMONLY              = ">you must be logged in to download mixtapes<";
    private static final String ONLYREGISTEREDUSERTEXT   = "Only downloadable for registered users";
    private static final String CURRENTLYUNAVAILABLE     = ">This is most likely because the uploader is currently making changes";
    private static final String CURRENTLYUNAVAILABLETEXT = "Currently unavailable";
    private static final String MAINPAGE                 = "http://www.datpiff.com/";

    public DatPiffCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.datpiff.com/register");
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) throws IOException {
        if (link.getDownloadURL().matches("http://(www\\.)?datpiff\\.com/mixtapes\\-detail\\.php\\?id=\\d+")) {
            link.setUrlDownload(link.getDownloadURL().replace("datpiff.com/mixtapes-detail.php?id=", "datpiff.com/pop-mixtape-download.php?id="));
        } else if (!link.getDownloadURL().contains(".php?id=")) {
            final Browser br2 = new Browser();
            br2.getPage(link.getDownloadURL());
            String downID = br2.getRegex("openMixtape\\( \\'(.*?)\\'").getMatch(0);
            if (downID == null) {
                downID = br2.getRegex("mixtapePlayer(Tall)?\\.swf\\?mid=(.*?)\"").getMatch(0);
            }
            if (downID != null) {
                link.setUrlDownload("http://www.datpiff.com/pop-mixtape-download.php?id=" + downID);
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(ONLYREGISTEREDUSERTEXT)) {
            link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.datpiffcom.only4premium", ONLYREGISTEREDUSERTEXT));
            return AvailableStatus.TRUE;
        }
        if (br.containsHTML("(>Download Unavailable<|>A zip file has not yet been generated<|>Mixtape Not Found<|has been removed<)")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = null;
        if (br.containsHTML(CURRENTLYUNAVAILABLE)) {
            filename = br.getRegex("<title>([^<>\"]*?) \\- Mixtapes @ DatPiff\\.com</title>").getMatch(0);
            if (filename == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setName(Encoding.htmlDecode(filename.trim()));
            link.getLinkStatus().setStatusText(CURRENTLYUNAVAILABLETEXT);
            return AvailableStatus.TRUE;
        }
        filename = br.getRegex("<title>Download Mixtape \\&quot;(.*?)\\&quot;</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<span>Download Mixtape<em>(.*?)</em></span>").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("<span>Download Track<em>([^<>\"]*?)</em>").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("name=\"title\" content=\"([^<>\"]*?)\"").getMatch(0);
        }
        if (filename == null) {
            final String downloadButton = br.getRegex("(?-s)iframe src=\"(https?://.*?embed/.*?/\\?downloadbutton=\\d+)").getMatch(0);
            if (downloadButton != null) {
                filename = br.getRegex("property=\"og:url\" content=\".*?\\.com/(.*?)\\.\\d+\\.html?").getMatch(0);
                if (filename != null) {
                    filename = filename + ".zip";
                }
                link.setName(Encoding.htmlDecode(filename.trim()));
                return AvailableStatus.TRUE;
            }
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename.trim()));
        return AvailableStatus.TRUE;
    }

    public void doFree(final DownloadLink downloadLink) throws Exception, PluginException {
        // Untested
        // final String timeToRelease =
        // br.getRegex("\\'dateTarget\\': (\\d+),").getMatch(0);
        // if (timeToRelease != null) throw new
        // PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE,
        // "Not yet released", Long.parseLong(timeToRelease) -
        // System.currentTimeMillis());
        String dllink = checkDirectLink(downloadLink, "directlink");
        if (dllink == null) {
            if (br.containsHTML(CURRENTLYUNAVAILABLE)) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, CURRENTLYUNAVAILABLETEXT, 3 * 60 * 60 * 1000l);
            }
            if (!downloadLink.getDownloadURL().contains(".php?id=")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Not (yet) downloadable");
            }
            String downloadButton = br.getRegex("(?-s)iframe src=\"(https?://.*?embed/.*?/\\?downloadbutton=\\d+)").getMatch(0);
            if (downloadButton != null) {
                br.getPage(downloadButton);
                downloadButton = br.getRegex("(?-s)downloadbutton\" href=\"(https?://.*?download\\?id=\\d+)").getMatch(0);
                if (downloadButton != null) {
                    br.getPage(downloadButton);
                    final Form form = br.getForm(0);
                    if (form != null) {
                        br.setFollowRedirects(false);
                        br.submitForm(form);
                        br.setFollowRedirects(true);
                        dllink = br.getRedirectLocation();
                    }
                }
            }
            if (dllink == null) {
                // whole mixtape
                String action = br.getRegex("id=\"loginform\" action=\"(/[^<>\"]*?)\"").getMatch(0);
                if (action == null) {
                    action = br.getRegex("<form action=\"(/[^<>\"]*?)\"").getMatch(0);
                }
                if (action == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                for (int i = 1; i <= 3; i++) {
                    String id = br.getRegex("name=\"id\" value=\"([^<>\"]*?)\"").getMatch(0);
                    if (id == null) {
                        id = new Regex(downloadLink.getDownloadURL(), "\\.php\\?id=(.+)").getMatch(0);
                    }
                    if (id == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    String postData = "id=" + id + "&x=" + new Random().nextInt(100) + "&y=" + new Random().nextInt(100);
                    if (br.containsHTML("solvemedia\\.com/papi/")) {
                        final PluginForDecrypt solveplug = JDUtilities.getPluginForDecrypt("linkcrypt.ws");
                        final org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia sm = new org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia(br);
                        File cf = null;
                        try {
                            cf = sm.downloadCaptcha(getLocalCaptchaFile());
                        } catch (final Exception e) {
                            if (org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia.FAIL_CAUSE_CKEY_MISSING.equals(e.getMessage())) {
                                throw new PluginException(LinkStatus.ERROR_FATAL, "Host side solvemedia.com captcha error - please contact the " + this.getHost() + " support");
                            }
                            throw e;
                        }
                        final String code = getCaptchaCode("solvemedia", cf, downloadLink);
                        final String chid = sm.getChallenge(code);
                        postData += "&cmd=downloadsolve&adcopy_response=" + Encoding.urlEncode(code) + "&adcopy_challenge=" + Encoding.urlEncode(chid);
                    }
                    br.postPage(action, postData);
                    dllink = br.getRedirectLocation();
                    if (dllink == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    if (dllink.matches("https?://(www\\.)?datpiff\\.com/pop\\-mixtape\\-download\\.php\\?id=[A-Za-z0-9]+")) {
                        br.getPage(dllink);
                        continue;
                    }
                    break;
                }
            }
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (dllink.matches("https?://(www\\.)?datpiff\\.com/pop\\-mixtape\\-download\\.php\\?id=[A-Za-z0-9]+")) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // Server doesn't send the correct filename directly, filename fix also
        // doesn't work so we have to do it this way
        downloadLink.setFinalFileName(getFileNameFromHeader(dl.getConnection()));
        downloadLink.setProperty("directlink", dllink);
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        final String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    return null;
                }
                return dllink;
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return null;
    }

    private void login(Account account) throws Exception {
        setBrowserExclusive();
        br.postPage("http://www.datpiff.com/login", "cmd=login&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
        if (br.getRedirectLocation() == null) { // Should be /account if OK, or /login if failed.
            for (int i = 0; i < 3; i++) { // Sometimes retry is needed, login page is displayed without redirect.
                Thread.sleep(3 * 1000);
                br.postPage("http://www.datpiff.com/login", "cmd=login&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                if (br.getRedirectLocation() != null) {
                    break;
                }
            }
        }
        if (br.getCookie(MAINPAGE, "mcim") == null || br.getCookie(MAINPAGE, "lastuser") == null) {
            final String lang = System.getProperty("user.language");
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (final PluginException e) {
            account.setValid(false);
            throw e;
        }
        account.setValid(true);
        ai.setUnlimitedTraffic();
        ai.setStatus("Registered User");
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://www.datpiff.com/terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML(PREMIUMONLY)) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) {
                    throw (PluginException) e;
                }
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.datpiffcom.only4premium", ONLYREGISTEREDUSERTEXT));
        }
        doFree(downloadLink);
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        doFree(link);
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
}
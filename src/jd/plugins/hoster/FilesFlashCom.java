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

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filesflash.com" }, urls = { "http://(www\\.)?(filesflash\\.(com|net)|173\\.231\\.61\\.130)(:\\d+)?/[a-z0-9]+" }, flags = { 2 })
public class FilesFlashCom extends PluginForHost {

    private static final String IPBLOCKED = "(>Your IP address is already downloading another link|Please wait for that download to finish\\.|Free users may only download one file at a time\\.)";
    private static String       MAINPAGE  = "http://filesflash.com/";

    public FilesFlashCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://filesflash.com/premium.php");
    }

    /**
     * Leave most of the urls unchanged as for some countries, only urls with
     * the port in it are accessable
     */
    // public void correctDownloadLink(DownloadLink link) {
    // link.setUrlDownload("http://filesflash.com/" + new
    // Regex(link.getDownloadURL(), "([a-z0-9]+)$").getMatch(0));
    // }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        // Link offline
        if (br.containsHTML("(>That is not a valid url\\.<|>That file is not available for download\\.<|>That file has been banned from this website|>That file was deleted due to inactivity<|>That file has been deleted)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // Invalid link
        if (br.containsHTML(">403 Forbidden<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex(">Filename: (.*?)<br").getMatch(0);
        String filesize = br.getRegex("Size: (.*?)</td>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (final PluginException e) {
            account.setValid(false);
            throw e;
        }
        br.getPage("http://filesflash.com/index.php");
        ai.setUnlimitedTraffic();
        String expire = br.getRegex("Premium: (\\d{4}\\-\\d{2}\\-\\d{2} \\d{2}:\\d{2}:\\d{2}) UTC").getMatch(0);
        if (expire == null) {
            final String lang = System.getProperty("user.language");
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Accounttyp!\r\nDieses Plugin unterstützt keine Free-Accounts, da diese bei diesem Hoster keine Vorteile bringen.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid accounttype!\r\nThis plugin doesn't support free accounts for this host because they don't bring any advantages.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd hh:mm:ss", null));
        }
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://filesflash.com/tos.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(DownloadLink link, Account acc) {

        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }

        return false;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (downloadLink.getDownloadURL().contains("filesflash.net")) MAINPAGE = "http://filesflash.net/";
        String MAINIP = br.getRegex("(http://filesflash\\.com:8001/)").getMatch(0);
        if (MAINIP == null) MAINIP = br.getRegex("(http://173\\.231\\.61\\.130:8001/)").getMatch(0);
        if (MAINIP == null) MAINIP = br.getRegex("(http://173\\.231\\.61\\.130/)").getMatch(0);
        if (MAINIP != null) MAINPAGE = MAINIP;
        final String token = br.getRegex("<input type=\"hidden\" name=\"token\" value=\"(.*?)\"/>").getMatch(0);
        if (token == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.postPage(MAINPAGE + "freedownload.php", "token=" + token + "&freedl=+Start+free+download+");
        if (br.containsHTML(IPBLOCKED)) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "IP already downloading", 10 * 60 * 1000l);
        if (br.containsHTML("(>That file is too big for free downloading.| Max allowed size for free downloads is)")) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.filesflashcom.only4premium", "Only downloadable for premium users"));
        final String rcID = br.getRegex("google\\.com/recaptcha/api/challenge\\?\\?rand=\\d+\\&amp;k=([^<>\"]*?)\"").getMatch(0);
        if (rcID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        rc.setId(rcID);
        rc.load();
        final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
        final String c = getCaptchaCode(cf, downloadLink);
        br.postPage(MAINPAGE + "freedownload.php", "token=" + token + "&submit=Submit&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c));
        if (br.containsHTML("google.com/recaptcha")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        // Should never happen
        if (br.containsHTML(">Your link has expired")) throw new PluginException(LinkStatus.ERROR_FATAL, "Server error (link expired)");
        String dllink = br.getRegex("(\"|\\')(http://[a-z0-9]+\\.filesflash\\.com/[a-z0-9]+/[a-z0-9]+/.*?)(\"|\\')").getMatch(1);
        if (dllink == null) dllink = br.getRegex("href=\'([^<>\"]*?)\'><big><b>Click here to start free download").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String wait = br.getRegex("count=(\\d+);").getMatch(0);
        int waittime = 45;
        if (wait != null) waittime = Integer.parseInt(wait);
        // Normal waittime is 45 seconds, if waittime > 10 Minutes reconnect
        if (waittime > 600) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waittime * 1001l);
        sleep(waittime * 1001l, downloadLink);
        br.setFollowRedirects(false);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML(IPBLOCKED)) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "IP already downloading", 10 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        String dllink = br.getRedirectLocation();
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.postPage("http://filesflash.com/login.php", "email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&submit=Login");
        if (br.getCookie(MAINPAGE, "userid") == null || br.getCookie(MAINPAGE, "password") == null || br.containsHTML(">Invalid email address or password")) {
            final String lang = System.getProperty("user.language");
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
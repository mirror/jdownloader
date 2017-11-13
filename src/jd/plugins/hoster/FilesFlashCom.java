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
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
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
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filesflash.com" }, urls = { "http://(www\\.)?(filesflash\\.(com|net)|173\\.231\\.61\\.130)(:8001)?/[a-z0-9]+" })
public class FilesFlashCom extends PluginForHost {
    private final String html_ipBlocked       = "(>Your IP address is already downloading another link|Please wait for that download to finish\\.|Free users may only download one file at a time\\.)";
    private final String html_tempunavailable = ">The server which has this file is currently not available";
    private final String mainDomain           = "http://filesflash.com/";
    private String       userDomain           = "filesflash.com";

    public FilesFlashCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
        this.enablePremium(mainDomain + "/premium.php");
    }

    @Override
    public String rewriteHost(String host) {
        if (host == null || "filesflash.com".equals(host) || "filesflash.net".equals(host)) {
            return "filesflash.com";
        }
        return super.rewriteHost(host);
    }

    /**
     * Leave most of the urls unchanged as for some countries, only urls with the port in it are accessible.-notRaztoki<br />
     * <br />
     * WRONG: bad because multihosters most likely will not associate IP based address with this hoster, base links there for need to be
     * changed to mainDomain. -raztoki 20150119
     *
     * @throws PluginException
     *
     *
     */
    @SuppressWarnings("deprecation")
    public void correctDownloadLink(DownloadLink link) throws PluginException {
        // find fuid
        final String fuid = new Regex(link.getDownloadURL(), "([a-z0-9]+)$").getMatch(0);
        if (fuid == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // set link dupe stuff
        try {
            link.setLinkID(getHost() + "://" + fuid);
        } catch (final Throwable e) {
            link.setProperty("LINKDUPEID", getHost() + "://" + fuid);
        }
        // set primary based on user settings
        setConfiguredDomain();
        // record userPreference link!
        final String userEndURL = "http://" + userDomain + "/" + fuid;
        link.setProperty("userEndURL", userEndURL);
        link.setContentUrl(userEndURL);
        link.setUrlDownload(mainDomain + fuid);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        correctDownloadLink(link);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getStringProperty("userEndURL", link.getDownloadURL()));
        // Link offline
        if (br.containsHTML("(>That is not a valid url\\.<|>That file has been banned|>That file has been deleted|>That file is not available|>That file was deleted|>That file was lost|>That file was removed)")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // Invalid link
        if (br.containsHTML(">403 Forbidden<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML(html_tempunavailable)) {
            link.getLinkStatus().setStatusText("The server on which this file is is currently unavailable");
            return AvailableStatus.TRUE;
        }
        final String filename = br.getRegex(">Filename:\\s*(.*?)\\s*<br").getMatch(0);
        final String filesize = br.getRegex("Size:\\s*(.*?)\\s*</td>").getMatch(0);
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            setConfiguredDomain();
            login(account);
        } catch (final PluginException e) {
            account.setValid(false);
            throw e;
        }
        br.getPage("/index.php");
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
        return mainDomain + "/tos.php";
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
        handleGeneralErrors();
        final String token = br.getRegex("<input type=\"hidden\" name=\"token\" value=\"(.*?)\"/>").getMatch(0);
        if (token == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.postPage("/freedownload.php", "token=" + token + "&freedl=+Start+free+download+");
        if (br.containsHTML(html_ipBlocked)) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "IP already downloading", 10 * 60 * 1000l);
        }
        if (br.containsHTML("(>That file is too big for free downloading.| Max allowed size for free downloads is)")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.filesflashcom.only4premium", "Only downloadable for premium users"));
        }
        final String rcID = br.getRegex("google\\.com/recaptcha/api/challenge\\?\\?rand=\\d+\\&amp;k=([^<>\"]*?)\"").getMatch(0);
        if (rcID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Recaptcha rc = new Recaptcha(br, this);
        rc.setId(rcID);
        rc.load();
        final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
        final String c = getCaptchaCode("recaptcha", cf, downloadLink);
        br.postPage("/freedownload.php", "token=" + token + "&submit=Submit&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c));
        if (br.containsHTML("google.com/recaptcha")) {
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        // Should never happen
        if (br.containsHTML(">Your link has expired")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Server error (link expired)");
        }
        String dllink = br.getRegex("(\"|\\')(http://[a-z0-9]+\\.filesflash\\.com/[a-z0-9]+/[a-z0-9]+/.*?)\\1").getMatch(1);
        if (dllink == null) {
            dllink = br.getRegex("href=\'([^<>\"]*?)\'><big><b>Click here to start free download").getMatch(0);
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String wait = br.getRegex("count=(\\d+);").getMatch(0);
        int waittime = 45;
        if (wait != null) {
            waittime = Integer.parseInt(wait);
        }
        // Normal waittime is 45 seconds, if waittime > 10 Minutes reconnect
        if (waittime > 600) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waittime * 1001l);
        }
        sleep(waittime * 1001l, downloadLink);
        br.setFollowRedirects(false);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML(html_ipBlocked)) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "IP already downloading", 10 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        handleGeneralErrors();
        login(account);
        br.setFollowRedirects(false);
        br.getPage(link.getStringProperty("userEndURL", link.getDownloadURL()));
        final String dllink = br.getRedirectLocation();
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

    private void handleGeneralErrors() throws PluginException {
        if (br.containsHTML(html_tempunavailable)) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "The server on which this file is is currently unavailable");
        }
    }

    private void login(final Account account) throws Exception {
        this.setBrowserExclusive();
        br.postPage("http://" + userDomain + "/login.php", "email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&submit=Login");
        if (br.getCookie("http://" + userDomain, "userid") == null || br.getCookie("http://" + userDomain, "password") == null || br.containsHTML(">Invalid email address or password")) {
            final String lang = System.getProperty("user.language");
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
    }

    private void setConfiguredDomain() {
        final int chosenDomain = getPluginConfig().getIntegerProperty(domain, 0);
        userDomain = this.allDomains[chosenDomain];
    }

    private final String   domain     = "domain";
    /** The list of server values displayed to the user */
    private final String[] allDomains = new String[] { "filesflash.com", "filesflash.net", "173.231.61.130" };

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), domain, allDomains, JDL.L("plugins.host.FilesFlashCom.preferredDomain", "Use this domain for download and login:")).setDefaultValue(0));
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
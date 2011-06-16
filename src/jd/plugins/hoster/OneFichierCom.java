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

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "1fichier.com" }, urls = { "http://[a-z0-9]+\\.(dl4free\\.com|alterupload\\.com|cjoint\\.net|desfichiers\\.com|dfichiers\\.com|megadl\\.fr|mesfichiers\\.org|piecejointe\\.net|pjointe\\.com|tenvoi\\.com|1fichier\\.com)/" }, flags = { 2 })
public class OneFichierCom extends PluginForHost {

    public OneFichierCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.1fichier.com/en/register.pl");
    }

    @Override
    public String getAGBLink() {
        return "http://www.1fichier.com/en/cgu.html";
    }

    private static AtomicInteger maxPrem = new AtomicInteger(1);

    public void correctDownloadLink(DownloadLink link) {
        // Note: We cannot replace all domains with "1fichier.com" because the
        // downloadlinks are always bind to a domains
        // Prefer english language
        if (!link.getDownloadURL().contains("/en/")) {
            // /en/index.html
            Regex idhostandName = new Regex(link.getDownloadURL(), "http://(.*?)\\.(.*?)/");
            link.setUrlDownload("http://" + idhostandName.getMatch(0) + "." + idhostandName.getMatch(1) + "/en/index.html");
        }
    }

    private static final String PASSWORDTEXT = "(Accessing this file is protected by password|Please put it on the box bellow)";
    private static final String PREMIUMPAGE  = "https://www.1fichier.com/en/login.pl";
    private static final String MAINPAGE     = "www.1fichier.com";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        prepareBrowser(br);
        br.setFollowRedirects(false);
        br.setCustomCharset("utf-8");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(The requested file could not be found|The file may has been deleted by its owner|Le fichier demandé n\\'existe pas\\.|Il a pu être supprimé par son propriétaire\\.)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>Téléchargement du fichier : (.*?)</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("content=\"Téléchargement du fichier (.*?)\">").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("(>Cliquez ici pour télécharger|>Click here to download) (.*?)</a>").getMatch(1);
                if (filename == null) {
                    filename = br.getRegex("\">(Nom du fichier :|File name :)</th>[\t\r\n ]+<td>(.*?)</td>").getMatch(1);
                    if (filename == null) filename = br.getRegex("<title>Download of (.*?)</title>").getMatch(0);
                }
            }
        }
        String filesize = br.getRegex("<th>(Taille :|File size :)</th>[\t\n\r ]+<td>(.*?)</td>").getMatch(1);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        if (filesize != null) {
            filesize = filesize.replace("Go", "Gb").replace("Mo", "Mb").replace("Ko", "Kb");
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        if (br.containsHTML(PASSWORDTEXT)) link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.onefichiercom.passwordprotected", "This link is password protected"));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    public void doFree(DownloadLink downloadLink) throws Exception, PluginException {
        String passCode = null;
        // Their limit is just very short so a 30 second waittime for all
        // downloads will remove the limit
        String dllink = br.getRedirectLocation();
        if (dllink != null && br.containsHTML("(/>Téléchargements en cours|>veuillez patienter avant de télécharger un autre fichier|>You already downloading some files|>Please wait a few seconds before downloading new ones)")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many simultan downloads", 45 * 1000l);

        if (br.containsHTML(PASSWORDTEXT)) {
            passCode = handlePassword(downloadLink, passCode);
            dllink = br.getRedirectLocation();
        } else {
            if (dllink == null) {
                dllink = br.getRegex("<br/>\\&nbsp;<br/>\\&nbsp;<br/>\\&nbsp;[\t\n\r ]+<a href=\"(http://.*?)\"").getMatch(0);
            }
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        /* server has issues with range requests ->result in broken file */
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        dl.startDownload();
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        prepareBrowser(br);
        br.postPage(PREMIUMPAGE, "mail=" + Encoding.urlEncode(account.getUser()) + " &pass=" + Encoding.urlEncode(account.getPass()) + "&secure=on&Login=Login");
        if (br.getCookie(MAINPAGE, "SID") == null || br.getCookie(MAINPAGE, "SID").equals("")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        /* reset maxPrem workaround on every fetchaccount info */
        maxPrem.set(1);
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        account.setValid(true);
        br.getPage("http://www.1fichier.com/en/console/abo.pl");
        String premUntil = br.getRegex("subscribed to our advanced services to (\\d+/\\d+/\\d+)").getMatch(0);
        if (premUntil != null) {
            account.setValid(true);
            ai.setStatus("Premium User");
            ai.setValidUntil(TimeFormatter.getMilliSeconds(premUntil, "dd/MM/yyyy", null) + (24 * 60 * 60 * 1000l));
            ai.setUnlimitedTraffic();
            try {
                maxPrem.set(20);
                account.setMaxSimultanDownloads(-1);
            } catch (final Throwable e) {
            }
            ai.setStatus("Premium User");
        } else {
            ai.setUnlimitedTraffic();
            try {
                maxPrem.set(5);
                account.setMaxSimultanDownloads(5);
            } catch (final Throwable e) {
            }
            ai.setStatus("Registered (free) User");
        }
        return ai;
    }

    private void prepareBrowser(final Browser br) {
        try {
            if (br == null) { return; }
            br.getHeaders().put("User-Agent", "Mozilla/5.0 (X11; U; Linux x86_64; en-US; rv:1.9.2.16) Gecko/20110323 Ubuntu/10.10 (maverick) Firefox/3.6.16");
            br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            br.getHeaders().put("Accept-Language", "en-us,en;q=0.5");
            br.getHeaders().put("Pragma", null);
            br.getHeaders().put("Cache-Control", null);
        } catch (Throwable e) {
            /* setCookie throws exception in 09580 */
        }
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        String passCode = null;
        requestFileInformation(link);
        login(account);
        Browser brold = br;
        br = new Browser();
        prepareBrowser(br);
        br.setCookie("https://www.1fichier.com", "SID", brold.getCookie("https://www.1fichier.com", "SID"));
        br.setFollowRedirects(false);
        sleep(10 * 1000l, link);
        br.getPage(link.getDownloadURL().replace("en/index.html", ""));
        if (maxPrem.get() == 5) {
            doFree(link);
        } else {
            if (br.containsHTML(PASSWORDTEXT)) passCode = handlePassword(link, passCode);
            String dllink = br.getRedirectLocation();
            if (dllink != null && br.containsHTML("(/>Téléchargements en cours|>veuillez patienter avant de télécharger un autre fichier|>You already downloading some files|>Please wait a few seconds before downloading new ones)")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many simultan downloads", 45 * 1000l);
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
            if (passCode != null) {
                link.setProperty("pass", passCode);
            }
            dl.startDownload();
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /* workaround for free/premium issue on stable 09581 */
        return maxPrem.get();
    }

    private String handlePassword(DownloadLink downloadLink, String passCode) throws IOException, PluginException {
        logger.info("This link seems to be password protected, continuing...");
        if (downloadLink.getStringProperty("pass", null) == null) {
            passCode = Plugin.getUserInput("Password?", downloadLink);
        } else {
            /* gespeicherten PassCode holen */
            passCode = downloadLink.getStringProperty("pass", null);
        }
        br.postPage(br.getURL(), "pass=" + passCode);
        if (br.containsHTML(PASSWORDTEXT)) throw new PluginException(LinkStatus.ERROR_RETRY, JDL.L("plugins.hoster.onefichiercom.wrongpassword", "Password wrong!"));
        return passCode;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
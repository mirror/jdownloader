//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.IOException;
import java.util.Locale;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filefactory.com" }, urls = { "http://[\\w\\.]*?filefactory\\.com(/|//)file/[\\w]+/?" }, flags = { 2 })
public class FileFactory extends PluginForHost {

    private static Pattern baseLink = Pattern.compile("action=\"(\\/dlf.*).\\ ", Pattern.CASE_INSENSITIVE);

    private static final String FILESIZE = "<span>(.*? (B|KB|MB)) file";

    private static final String NO_SLOT = "no free download slots";
    private static final String NOT_AVAILABLE = "class=\"box error\"";

    private static final String LOGIN_ERROR = "The email or password you have entered is incorrect";

    private static Pattern patternForDownloadlink = Pattern.compile("downloadLink.*href..(.*)..Click", Pattern.DOTALL);

    private static final String SERVER_DOWN = "server hosting the file you are requesting is currently down";

    public FileFactory(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.filefactory.com/info/premium.php");
    }

    public int getTimegapBetweenConnections() {
        return 200;
    }

    public void handleFree(DownloadLink parameter) throws Exception {
        requestFileInformation(parameter);
        try {
            handleFree0(parameter);
        } catch (InterruptedException e2) {
            return;
        } catch (IOException e) {
            logger.log(java.util.logging.Level.SEVERE, "Exception occurred", e);
            if (e.getMessage() != null && e.getMessage().contains("502")) {
                logger.severe("Filefactory returned Bad gateway.");
                Thread.sleep(1000);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            } else if (e.getMessage() != null && e.getMessage().contains("503")) {
                logger.severe("Filefactory returned Bad gateway.");
                Thread.sleep(1000);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            } else {
                throw e;
            }
        }
    }

    public void handleFree0(DownloadLink parameter) throws Exception {
        br.setFollowRedirects(true);
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("there are currently no free download slots")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 3 * 60 * 1000l); }
        if (br.containsHTML(NOT_AVAILABLE)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML(SERVER_DOWN) || br.containsHTML(NO_SLOT)) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 20 * 60 * 1000l); }

        // URL von zweiter Seite ermitteln
        String urlWithFilename = Encoding.htmlDecode("http://www.filefactory.com" + br.getRegex(baseLink).getMatch(0));

        // Zweite Seite laden
        br.getPage(urlWithFilename);

        // Datei Downloadlink filtern
        String downloadUrl = Encoding.htmlDecode(br.getRegex(patternForDownloadlink).getMatch(0));
        long waittime = 31000l;

        try {
            waittime = Long.parseLong(br.getRegex("<p id=\"countdown\">(\\d+?)</p>").getMatch(0)) * 1000l;
        } catch (Exception e) {
        }
        if (waittime > 60000l) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waittime); }
        waittime += 1000;
        sleep(waittime, parameter);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, parameter, downloadUrl);

        // Prüft ob content disposition header da sind
        if (dl.getConnection().isContentDisposition()) {
            dl.startDownload();
        } else {
            br.followConnection();
            if (br.containsHTML("have exceeded the download limit")) {
                waittime = 0;
                try {
                    waittime = Long.parseLong(br.getRegex("Please wait (\\d+) minutes to download more files").getMatch(0)) * 1000l;
                } catch (Exception e) {
                }
                if (waittime > 0) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waittime);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        }

    }

    public int getMaxRetries() {
        return 20;
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage("http://filefactory.com");

        Form login = br.getForm(0);
        login.put("email", account.getUser());
        login.put("password", account.getPass());
        br.submitForm(login);

        if (br.containsHTML(LOGIN_ERROR)) throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
    }

    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);

        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }

        br.getPage("http://www.filefactory.com/member/");
        String expire = br.getMatch("Your account is valid until the <strong>(.*?)</strong>");
        if (expire == null) {
            account.setValid(false);
            return ai;
        }
        expire = expire.replace("th", "");
        ai.setValidUntil(Regex.getMilliSeconds(expire, "dd MMMM, yyyy", Locale.UK));

        br.getPage("http://www.filefactory.com/reward/summary.php");
        String points = br.getMatch("Available reward points.*?class=\"amount\">(.*?) points").replaceAll("\\,", "");
        ai.setPremiumPoints(Long.parseLong(points.trim()));

        return ai;
    }

    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, br.getRedirectLocation(), true, 0);
        if (dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (br.containsHTML(NOT_AVAILABLE)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.containsHTML(SERVER_DOWN)) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 20 * 60 * 1000l);
            } else {
                String red = br.getRegex(Pattern.compile("10px 0;\">.*<a href=\"(.*?)\">Download with FileFactory Premium", Pattern.DOTALL)).getMatch(0);
                logger.finer("Indirect download");
                br.setFollowRedirects(true);
                dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, red, true, 0);
            }
        } else {
            logger.finer("DIRECT download");
        }
        dl.startDownload();
    }

    public String getAGBLink() {
        return "http://www.filefactory.com/info/terms.php";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll(".com//", ".com/"));
        link.setUrlDownload(link.getDownloadURL().replaceAll("http://filefactory", "http://www.filefactory"));
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        for (int i = 0; i < 4; i++) {
            try {
                Thread.sleep(200);
            } catch (Exception e) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            try {
                br.getPage(downloadLink.getDownloadURL());
                break;
            } catch (Exception e) {
                if (i == 3) throw e;
            }
        }
        if (br.containsHTML(NOT_AVAILABLE) && !br.containsHTML("there are currently no free download slots")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML(SERVER_DOWN)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else {
            if (br.containsHTML("there are currently no free download slots")) {
                downloadLink.getLinkStatus().setErrorMessage(JDL.L("plugins.hoster.filefactorycom.errors.nofreeslots", "No slots free available"));
                downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.filefactorycom.errors.nofreeslots", "No slots free available"));
            } else {
                if (br.containsHTML("File Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                String fileName = br.getRegex("<title>(.*?) - FileFactory</title>").getMatch(0);
                String fileSize = br.getRegex(FILESIZE).getMatch(0);
                if (fileName == null || fileSize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                downloadLink.setName(fileName.trim());
                downloadLink.setDownloadSize(Regex.getSize(fileSize));
            }

        }
        br.setFollowRedirects(false);
        return AvailableStatus.TRUE;
    }

    public void init() {
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public void reset() {
    }

    public void resetPluginGlobals() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }

}

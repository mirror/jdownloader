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

package jd.plugins.host;

import java.io.IOException;
import java.util.Locale;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.reconnect.Reconnecter;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDLocale;

public class FileFactory extends PluginForHost {

    private static Pattern baseLink = Pattern.compile("action=\"(\\/dlf.*).\\ ", Pattern.CASE_INSENSITIVE);
    private static final String DOWNLOAD_LIMIT = "(Thank you for waiting|exceeded the download limit)";

    private static final String FILESIZE = "<span>(.*? (B|KB|MB)) file";

    private static final String NO_SLOT = "no free download slots";
    private static final String NOT_AVAILABLE = "class=\"box error\"";
    private static final String PATTERN_DOWNLOADING_TOO_MANY_FILES = "currently downloading too many files at once";
    private static final String WAIT_TIME = "have exceeded the download limit for free users.*Please wait ([0-9]+).*to download more files";

    private static final String LOGIN_ERROR = "The email or password you have entered is incorrect";

    private static Pattern patternForDownloadlink = Pattern.compile("downloadLink.*href..(.*)..Click", Pattern.DOTALL);

    private static final String SERVER_DOWN = "server hosting the file you are requesting is currently down";

    public FileFactory(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.filefactory.com/info/premium.php");
    }

    // @Override
    public int getTimegapBetweenConnections() {
        return 200;
    }

    // @Override
    public void handleFree(DownloadLink parameter) throws Exception {
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

        sleep(31000, parameter);
        dl = br.openDownload(parameter, downloadUrl);

        // Prüft ob content disposition header da sind
        if (dl.getConnection().isContentDisposition()) {
            long cu = parameter.getDownloadCurrent();
            dl.startDownload();
            long loaded = parameter.getDownloadCurrent() - cu;
            if (loaded > 30 * 1024 * 1024l) {
                Reconnecter.requestReconnect();
            }
        } else {
            // Falls nicht wird die html seite geladen
            br.followConnection();
            if (br.containsHTML(DOWNLOAD_LIMIT)) {
                logger.info("Traffic Limit for Free User reached");
                if (br.containsHTML("seconds")) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(br.getRegex(WAIT_TIME).getMatch(0)) * 60 * 1000l);
                } else if (br.containsHTML("minutes")) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(br.getRegex(WAIT_TIME).getMatch(0)) * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
                }
            } else if (br.containsHTML(PATTERN_DOWNLOADING_TOO_MANY_FILES)) {
                logger.info("You are downloading too many files at the same time. Wait 10 seconds(or reconnect) and retry afterwards");
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 1000l);
            }
        }

    }

    // @Override
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

    // @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);

        try {
            login(account);
        } catch (PluginException e) {
            ai.setValid(false);
            return ai;
        }

        br.getPage("http://www.filefactory.com/member/");
        String expire = br.getMatch("Your account is valid until the <strong>(.*?)</strong>");
        if (expire == null) {
            ai.setValid(false);
            return ai;
        }
        expire = expire.replace("th", "");
        ai.setValidUntil(Regex.getMilliSeconds(expire, "dd MMMM, yyyy", Locale.UK));

        br.getPage("http://www.filefactory.com/reward/summary.php");
        String points = br.getMatch("Available reward points.*?class=\"amount\">(.*?) points").replaceAll("\\,", "");
        ai.setPremiumPoints(Long.parseLong(points.trim()));

        return ai;
    }

    // @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);

        login(account);

        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());

        dl = br.openDownload(downloadLink, br.getRedirectLocation(), true, 0);

        if (dl.getConnection().getHeaderField("Content-Disposition") == null) {
            br.followConnection();

            if (br.containsHTML(NOT_AVAILABLE)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.containsHTML(SERVER_DOWN)) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 20 * 60 * 1000l);
            } else {
                String red = br.getRegex(Pattern.compile("10px 0;\">.*<a href=\"(.*?)\">Download with FileFactory Premium", Pattern.DOTALL)).getMatch(0);
                logger.finer("Indirect download");
                dl = br.openDownload(downloadLink, red, true, 0);
            }
        } else {
            logger.finer("DIRECT download");
        }
        dl.startDownload();
    }

    // @Override
    public String getAGBLink() {
        return "http://www.filefactory.com/info/terms.php";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll(".com//", ".com/"));
        link.setUrlDownload(link.getDownloadURL().replaceAll("http://filefactory", "http://www.filefactory"));
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        br.setFollowRedirects(true);
        for (int i = 0; i < 4; i++) {
            try {
                Thread.sleep(200);
            } catch (Exception e) {
                return AvailableStatus.FALSE;
            }
            try {
                br.getPage(downloadLink.getDownloadURL());
                break;
            } catch (Exception e) {
                if (i == 3) throw e;
            }
        }
        if (br.containsHTML(NOT_AVAILABLE) && !br.containsHTML("there are currently no free download slots")) {
            br.setFollowRedirects(false);
            return AvailableStatus.FALSE;
        } else if (br.containsHTML(SERVER_DOWN)) {
            br.setFollowRedirects(false);
            return AvailableStatus.FALSE;
        } else {
            if (br.containsHTML("there are currently no free download slots")) {
                downloadLink.getLinkStatus().setErrorMessage(JDLocale.L("plugins.hoster.filefactorycom.errors.nofreeslots", "No slots free available"));
                downloadLink.getLinkStatus().setStatusText(JDLocale.L("plugins.hoster.filefactorycom.errors.nofreeslots", "No slots free available"));
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

    // @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    // @Override
    public void init() {
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    // @Override
    public void reset() {
    }

    // @Override
    public void resetPluginGlobals() {
    }

    // @Override
    public void reset_downloadlink(DownloadLink link) {
    }

}

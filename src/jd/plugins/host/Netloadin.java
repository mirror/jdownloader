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

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Encoding;
import jd.http.URLConnectionAdapter;
import jd.http.requests.Request;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;
import jd.utils.JDLocale;

public class Netloadin extends PluginForHost {
    static private final String AGB_LINK = "http://netload.in/index.php?id=13";

    static private final String CAPTCHA_WRONG = "Sicherheitsnummer nicht eingegeben";

    static private final String DOWNLOAD_CAPTCHA = "download_captcha.tpl";
    static private final String DOWNLOAD_LIMIT = "download_limit.tpl";
    static private final String DOWNLOAD_START = "download_load.tpl";
    private String LINK_PASS = null;

    static private final Pattern DOWNLOAD_WAIT_TIME = Pattern.compile("countdown\\(([0-9]*),'change", Pattern.CASE_INSENSITIVE);

    static private final String FILE_DAMAGED = "(Die Datei wurde Opfer einer defekten Festplatte|Diese Datei liegt auf einem Server mit einem technischen Defekt. Wir konnten diese Datei leider nicht wieder herstellen)";

    static private final String FILE_NOT_FOUND = "Die Datei konnte leider nicht gefunden werden";

    static private final String LIMIT_REACHED = "share/images/download_limit_go_on.gif";
    static private final String NEW_HOST_URL = "<a class=\"Orange_Link\" href=\"(.*?)\" >Alternativ klicke hier\\.<\\/a>";

    private static String getID(String link) {
        String id = new Regex(link, "\\/datei([a-zA-Z0-9]+)").getMatch(0);
        if (id == null) id = new Regex(link, "file_id=([a-zA-Z0-9]+)").getMatch(0);
        return id;
    }

    private String fileStatusText;

    public Netloadin(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(1000l);
        this.enablePremium("http://netload.in/index.php?refer_id=134847&id=39");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        if (downloadLink.getDownloadURL().matches("sjdp://.*")) {
            ((PluginForHost) PluginWrapper.getNewInstance("jd.plugins.host.Serienjunkies")).handleFree(downloadLink);
            return;
        }
        br.setDebug(true);

        LinkStatus linkStatus = downloadLink.getLinkStatus();
        downloadLink.setUrlDownload("http://netload.in/datei" + Netloadin.getID(downloadLink.getDownloadURL()) + ".htm");
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        br.getPage(downloadLink.getDownloadURL());
        checkPassword(downloadLink);
        if (linkStatus.isFailed()) return;

        String url = br.getRegex(Pattern.compile("<div class=\"Free_dl\">.*?<a href=\"(.*?)\"", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (br.containsHTML(FILE_NOT_FOUND)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }
        if (br.containsHTML(FILE_DAMAGED)) {
            linkStatus.setErrorMessage("File is on a damaged server");
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setValue(20 * 60 * 1000l);
            return;
        }

        if (!br.containsHTML(DOWNLOAD_START) || url == null) {
            linkStatus.setErrorMessage("Download link not found");
            logger.severe(br.toString());
            linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFEKT);
            return;
        }
        url = url.replaceAll("\\&amp\\;", "&");
        br.getPage(url);
        if (br.containsHTML(FILE_DAMAGED)) {
            linkStatus.setErrorMessage("File is on a damaged server");
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setValue(20 * 60 * 1000l);
            return;
        }

        if (!br.containsHTML(DOWNLOAD_CAPTCHA)) {
            linkStatus.setErrorMessage("Captcha not found");
            linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFEKT);
            return;
        }

        String captchaURL = br.getRegex("<img style=\".*?\" src=\"(.*?)\" alt=\"Sicherheitsbild\" \\/>").getMatch(0);
        Form[] forms = br.getForms();
        Form captchaPost = forms[0];
        captchaPost.setAction("index.php?id=10");
        if (captchaURL == null) {
            if (br.containsHTML("download_load.tpl")) {
                linkStatus.addStatus(LinkStatus.ERROR_RETRY);
                return;
            }
            linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFEKT);
            return;
        }
        File file = this.getLocalCaptchaFile(this);
        Browser c = br.cloneBrowser();
        Browser.download(file, c.openGetConnection(captchaURL));
        captchaPost.put("captcha_check", Plugin.getCaptchaCode(file, this, downloadLink));
        br.submitForm(captchaPost);
        if (br.containsHTML(FILE_NOT_FOUND)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }
        if (br.containsHTML(FILE_DAMAGED)) {
            logger.warning("File is on a damaged server");
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setValue(20 * 60 * 1000l);
            return;
        }
        if (br.containsHTML("Datenbank Fehler")) {
            logger.warning("Database Error");
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setValue(20 * 60 * 1000l);
            return;
        }

        if (br.containsHTML(LIMIT_REACHED) || br.containsHTML(DOWNLOAD_LIMIT)) {
            String wait = new Regex(br.getRequest().getHtmlCode(), DOWNLOAD_WAIT_TIME).getMatch(0);
            long waitTime = 0;
            if (wait != null) {
                waitTime = Long.parseLong(wait);
                waitTime = waitTime * 10L;
            }
            if (waitTime == 0) {
                logger.finest("Waittime was 0");
                sleep(30000l, downloadLink);
                linkStatus.addStatus(LinkStatus.ERROR_RETRY);
                return;
            }
            linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
            linkStatus.setValue(waitTime);
            return;
        }
        if (br.containsHTML(CAPTCHA_WRONG)) {
            linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
            return;
        }
        String finalURL = br.getRegex(NEW_HOST_URL).getMatch(0);
        sleep(20000, downloadLink);
        dl = RAFDownload.download(downloadLink, br.createRequest(finalURL));
        dl.startDownload();
    }

    private void checkPassword(DownloadLink downloadLink) throws Exception {
        if (!br.containsHTML("download_password")) return;
        String pass = downloadLink.getStringProperty("pass", LINK_PASS);

        // falls das pw schon gesetzt und gespeichert wurde.. versucht er es
        // damit
        if (pass != null && br.containsHTML("download_password")) {
            Form[] forms = br.getForms();
            Form pw = forms[forms.length - 1];
            pw.put("password", pass);
            br.submitForm(pw);
        }
        // ansonsten 3 abfrageversuche
        int maxretries = 3;
        while (br.containsHTML("download_password") && maxretries-- >= 0) {
            Form[] forms = br.getForms();
            Form pw = forms[forms.length - 1];
            pw.put("password", pass = Plugin.getUserInput(JDLocale.LF("plugins.netload.downloadPassword_question", "Password protected. Enter Password for %s", downloadLink.getName()), downloadLink));
            br.submitForm(pw);
        }
        // falls falsch abbruch
        if (br.containsHTML("download_password")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDLocale.L("plugins.netload.downloadPassword_wrong", "Linkpassword is wrong"), 20 * 60 * 1000l);

        }
        // richtiges pw... wird gesoeichert
        if (pass != null) {
            downloadLink.setProperty("pass", pass);
            LINK_PASS = pass;
        }
    }

    private void login(Account account) throws IOException, PluginException {
        setBrowserExclusive();
        br.getPage("http://netload.in/index.php");
        br.postPage("http://netload.in/index.php", "txtuser=" + Encoding.urlEncode(account.getUser()) + "&txtpass=" + Encoding.urlEncode(account.getPass()) + "&txtcheck=login&txtlogin=");
        String cookie = br.getCookie("http://netload.in/", "cookie_user");
        if (cookie == null) {
            account.setEnabled(false);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        }
        if (br.getRedirectLocation() == null || !br.getRedirectLocation().trim().equalsIgnoreCase("http://netload.in/index.php")) {
            account.setEnabled(false);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    private void isExpired(Account account) throws IOException, PluginException {
        br.getPage("http://netload.in/index.php?id=2");
        String validUntil = br.getRegex("Verbleibender Zeitraum</div>.*?<div style=.*?><span style=.*?>(.*?)</span></div>").getMatch(0).trim();
        if (validUntil != null && new Regex(validUntil.trim(), "kein").matches()) {
            account.setEnabled(false);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    public AccountInfo getAccountInformation(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        try {
            login(account);
        } catch (PluginException e) {
            ai.setValid(false);
            return ai;
        }
        try {
            isExpired(account);
        } catch (PluginException e) {
            ai.setExpired(true);
            return ai;
        }
        String validUntil = br.getRegex("Verbleibender Zeitraum</div>.*?<div style=.*?><span style=.*?>(.*?)</span></div>").getMatch(0).trim();
        String days = new Regex(validUntil, "([\\d]+) ?Tage").getMatch(0);
        String hours = new Regex(validUntil, "([\\d]+) ?Stunde").getMatch(0);
        long res = 0;
        if (days != null) res += Long.parseLong(days.trim()) * 24 * 60 * 60 * 1000;
        if (hours != null) res += Long.parseLong(hours.trim()) * 60 * 60 * 1000;
        res += new Date().getTime();
        ai.setValidUntil(res);
        return ai;
    }

    public int getTimegapBetweenConnections() {
        return 800;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        if (downloadLink.getDownloadURL().matches("sjdp://.*")) {
            ((PluginForHost) PluginWrapper.getNewInstance("jd.plugins.host.Serienjunkies")).handleFree(downloadLink);
            return;
        }
        getFileInformation(downloadLink);
        login(account);
        isExpired(account);
        downloadLink.setUrlDownload("http://netload.in/datei" + Netloadin.getID(downloadLink.getDownloadURL()) + ".htm");
        br.setFollowRedirects(false);
        br.setDebug(true);
        br.openGetConnection(downloadLink.getDownloadURL());
        Request con;
        if (br.getRedirectLocation() == null) {
            Thread.sleep(1000);
            br.followConnection();
            checkPassword(downloadLink);
            checkErrors();
            String url = br.getRedirectLocation();
            if (url == null) url = br.getRegex("<a class=\"Orange_Link\" href=\"(.*?)\" >Alternativ klicke hier.<\\/a>").getMatch(0);
            if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT, "Download link not found");

            con = br.createRequest(url);

            dl = RAFDownload.download(downloadLink, con, true, 0);
            // dl.headFake(null);
            dl.setFirstChunkRangeless(true);
            URLConnectionAdapter connection = dl.connect(br);
            for (int i = 0; i < 10 && (!connection.isOK()); i++) {
                try {
                    con = br.createRequest(url);
                    dl = RAFDownload.download(downloadLink, con, true, 0);
                    connection = dl.connect(br);
                } catch (Exception e) {
                    try {
                        Thread.sleep(150);
                    } catch (InterruptedException e2) {
                        e2.printStackTrace();
                    }
                }
            }
        } else {
            con = br.createGetRequest(null);
            dl = RAFDownload.download(downloadLink, con, true, 0);
            // dl.headFake(null);
            dl.setFirstChunkRangeless(true);
            dl.connect(br);
        }
        if (!dl.getConnection().isContentDisposition() && dl.getConnection().getResponseCode() != 206 && dl.getConnection().getResponseCode() != 416) {
            // Serverfehler
            if (br.followConnection() == null) throw new PluginException(LinkStatus.ERROR_RETRY, "Server:Could not follow Link");
            checkPassword(downloadLink);
            checkErrors();
        }
        dl.startDownload();
    }

    private void checkErrors() throws PluginException {
        if (br.containsHTML(FILE_NOT_FOUND)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML(FILE_DAMAGED)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File is on a damaged server", 20 * 60 * 1000l);
    }

    @Override
    public String getAGBLink() {
        return AGB_LINK;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws PluginException {
        if (downloadLink.getDownloadURL().matches("sjdp://.*")) return false;
        try {
            this.setBrowserExclusive();
            br.setConnectTimeout(15000);

            String id = Netloadin.getID(downloadLink.getDownloadURL());
            String page = br.getPage("http://netload.in/share/fileinfos2.php?bz=1&file_id=" + id);
            for (int i = 0; i < 3 && page == null; i++) {
                try {
                    Thread.sleep(150);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                page = br.getPage("http://netload.in/share/fileinfos2.php?bz=1&file_id=" + id);
            }

            if (page == null || Regex.matches(page, "unknown file_data")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

            String[] entries = br.getRegex("(.*?);(.*?);(.*?);(.*?);(.*)").getRow(0);

            if (entries == null) {
                entries = br.getRegex(";(.*?);(.*?);(.*?)").getRow(0);
                if (entries == null || entries.length < 3) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                downloadLink.setDownloadSize((int) Regex.getSize(entries[1] + " bytes"));
                downloadLink.setName(entries[0]);
                downloadLink.setDupecheckAllowed(true);
                fileStatusText = "Might be offline";
                return true;
            }

            if (entries == null || entries.length < 3) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

            downloadLink.setName(entries[1]);
            downloadLink.setDupecheckAllowed(true);
            fileStatusText = entries[2];
            downloadLink.setDownloadSize((int) Regex.getSize(entries[2] + " bytes"));

            downloadLink.setMD5Hash(entries[4].trim());
            if (entries[3].equalsIgnoreCase("online")) { return true; }
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } catch (PluginException e2) {
            throw e2;
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public String getFileInformationString(DownloadLink downloadLink) {
        if (downloadLink.getDownloadURL().matches("sjdp://.*")) return "";
        return downloadLink.getName() + " (" + fileStatusText + ")";
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {

    }
}
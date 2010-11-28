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
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.download.RAFDownload;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "netload.in" }, urls = { "http://[\\w\\.]*?netload\\.in/[^(http://)].+" }, flags = { 2 })
public class Netloadin extends PluginForHost {
    static private final String  AGB_LINK            = "http://netload.in/index.php?id=13";

    static private final String  CAPTCHA_WRONG       = "Sicherheitsnummer nicht eingegeben";

    static private final String  DOWNLOAD_CAPTCHA    = "download_captcha.tpl";
    static private final String  DOWNLOAD_LIMIT      = "download_limit.tpl";
    static private final String  DOWNLOAD_START      = "download_load.tpl";
    static private final String  DOWNLOAD_STARTXMAS  = "download_load_xmas.tpl";
    static private final String  DOWNLOAD_STARTXMAS2 = "download_load_xmas2.tpl";
    private String               LINK_PASS           = null;

    static private final Pattern DOWNLOAD_WAIT_TIME  = Pattern.compile("countdown\\(([0-9]*),'change", Pattern.CASE_INSENSITIVE);

    static private final String  FILE_DAMAGED        = "(Die Datei wurde Opfer einer defekten Festplatte|Diese Datei liegt auf einem Server mit einem technischen Defekt|This Server is currently in maintenance work)";

    static private final String  FILE_NOT_FOUND      = "Die Datei konnte leider nicht gefunden werden";

    static private final String  LIMIT_REACHED       = "share/images/download_limit_go_on.gif";
    static private final String  NEW_HOST_URL        = "<a class=\"Orange_Link\" href=\"(.*?)\" >Alternativ klicke hier\\.<\\/a>";

    private static String getID(String link) {
        String id = new Regex(link, "\\/datei([a-zA-Z0-9]+)").getMatch(0);
        if (id == null) id = new Regex(link, "file_id=([a-zA-Z0-9]+)").getMatch(0);
        if (id == null) id = new Regex(link, "netload\\.in\\/([a-zA-Z0-9]+)\\/.+").getMatch(0);
        return id;
    }

    public Netloadin(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(1000l);
        this.enablePremium("http://netload.in/index.php?refer_id=134847&id=39");
    }

    /* TODO: remove me after 0.9xx public */
    private void workAroundTimeOut(Browser br) {
        try {
            if (br != null) {
                br.setConnectTimeout(30000);
                br.setReadTimeout(30000);
            }
        } catch (Throwable e) {
        }
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload("http://netload.in/datei" + Netloadin.getID(link.getDownloadURL()) + ".htm");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        try {
            workAroundTimeOut(br);
            requestFileInformation(downloadLink);
            br.setDebug(true);
            LinkStatus linkStatus = downloadLink.getLinkStatus();
            this.setBrowserExclusive();
            br.getPage("http://netload.in/index.php?lang=de");
            br.setFollowRedirects(true);
            br.getPage(downloadLink.getDownloadURL());
            br.setFollowRedirects(false);
            checkPassword(downloadLink);

            if (linkStatus.isFailed()) return;
            if (br.containsHTML("download_fast_link")) {
                handleFastLink(downloadLink);
                return;
            }
            String url = br.getRegex(Pattern.compile("<div class=\"Free_dl\">.*?<a href=\"(.*?)\"", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
            if (br.containsHTML(FILE_NOT_FOUND)) {
                linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
                return;
            }
            if (br.containsHTML("We occurred an unexpected error")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 30 * 60 * 1000l);
            if (br.containsHTML("Im Link ist ein Schreibfehler")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (br.containsHTML(FILE_DAMAGED)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.netloadin.errors.fileondmgserver", "File on damaged server"), 20 * 60 * 1000l);
            if ((!br.containsHTML(DOWNLOAD_START) && !br.containsHTML(DOWNLOAD_STARTXMAS) && !br.containsHTML(DOWNLOAD_STARTXMAS2)) || url == null) {
                linkStatus.setErrorMessage(JDL.L("plugins.hoster.netloadin.errors.dlnotfound", "Download link not found"));
                logger.severe(br.toString());
                linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFECT);
                return;
            }
            url = url.replaceAll("\\&amp\\;", "&");
            br.getPage(url);
            if (br.containsHTML(FILE_DAMAGED)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.netloadin.errors.fileondmgserver", "File on damaged server"), 20 * 60 * 1000l);

            if (!br.containsHTML(DOWNLOAD_CAPTCHA)) {
                linkStatus.setErrorMessage(JDL.L("plugins.hoster.netloadin.errors.captchanotfound", "Captcha not found"));
                linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFECT);
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
                linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFECT);
                return;
            }

            captchaPost.put("captcha_check", getCaptchaCode(captchaURL, downloadLink));
            br.submitForm(captchaPost);
            handleErrors(downloadLink);

            String finalURL = br.getRegex(NEW_HOST_URL).getMatch(0);
            sleep(20000, downloadLink);
            logger.info("used serverurl: " + finalURL);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finalURL);
            dl.startDownload();
        } catch (IOException e) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 5 * 60 * 1000l);
        }
    }

    private void handleErrors(DownloadLink downloadLink) throws Exception {
        if (br.containsHTML(FILE_NOT_FOUND)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("Im Link ist ein Schreibfehler")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML(FILE_DAMAGED)) {
            logger.warning("File is on a damaged server");
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.netloadin.errors.fileondmgserver", "File on damaged server"), 20 * 60 * 1000l);

        }
        if (br.containsHTML("Datenbank Fehler")) {
            logger.warning("Database Error");
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 20 * 60 * 1000l);

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

                throw new PluginException(LinkStatus.ERROR_RETRY);

            }

            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waitTime);

        }
        if (br.containsHTML(CAPTCHA_WRONG)) {

        throw new PluginException(LinkStatus.ERROR_CAPTCHA); }
        if (br.containsHTML("download_unknown_server_data")) {
            logger.info("File is not uploaded completly");
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 60 * 2000l);

        }
        if (br.containsHTML("unknown_file_data")) {
            logger.info("unknown_file_data");
            throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.netloadin.errors.damagedfile", "Damaged file"));

        }

    }

    private void handleFastLink(DownloadLink downloadLink) throws Exception {
        br.forceDebug(true);
        String url = br.getRegex("<a class=\"download_fast_link\" href=\"(.*?)\">Start Free Download</a>").getMatch(0);

        url = Encoding.htmlDecode(url);
        url = "http://netload.in/" + url;

        sleep(10000, downloadLink);
        br.setFollowRedirects(false);
        br.getPage(url);
        if (br.getRedirectLocation() != null) {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, br.getRedirectLocation());
            dl.startDownload();
        } else {
            handleErrors(downloadLink);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

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
            pw.put("password", pass = Plugin.getUserInput(JDL.LF("plugins.hoster.netload.downloadPassword_question", "Password protected. Enter Password for %s", downloadLink.getName()), downloadLink));
            br.submitForm(pw);
        }
        // falls falsch abbruch
        if (br.containsHTML("download_password")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.netload.errors.downloadPassword_wrong", "Link password is wrong"), 20 * 60 * 1000l);

        }
        // richtiges pw... wird gesoeichert
        if (pass != null) {
            downloadLink.setProperty("pass", pass);
            LINK_PASS = pass;
        }
    }

    private void login(Account account) throws IOException, PluginException {
        setBrowserExclusive();
        // br.setDebug(true);
        workAroundTimeOut(br);
        br.getPage("http://netload.in/index.php?lang=de");
        br.getPage("http://netload.in/index.php");
        br.postPage("http://netload.in/index.php", "txtuser=" + Encoding.urlEncode(account.getUser()) + "&txtpass=" + Encoding.urlEncode(account.getPass()) + "&txtcheck=login&txtlogin=");
        String cookie = br.getCookie("http://netload.in/", "cookie_user");
        if (cookie == null) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
        if (br.getRedirectLocation() == null || !br.getRedirectLocation().trim().equalsIgnoreCase("http://netload.in/index.php")) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
    }

    private void isExpired(Account account) throws IOException, PluginException {
        br.getPage("http://netload.in/index.php?id=2");
        String validUntil = br.getRegex("Verbleibender Zeitraum</div>.*?<div style=.*?><span style=.*?>(.*?)</span></div>").getMatch(0);
        if (validUntil != null && new Regex(validUntil.trim(), "kein").matches()) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        try {
            isExpired(account);

        } catch (PluginException e) {
            ai.setExpired(true);
            return ai;
        }
        String validUntil = br.getRegex("Verbleibender Zeitraum</div>.*?<div style=.*?><span style=.*?>(.*?)</span></div>").getMatch(0);
        if (validUntil == null) {
            account.setValid(true);
            account.setTempDisabled(true);
            ai.setStatus("Temp Server Error, will retry later");
            /* only for debugging */
            try {
                logger.finest(br.getRequest().getHttpConnection() + "");
            } catch (Throwable e) {
            }
            try {
                logger.finest(br + "");
            } catch (Throwable e) {
            }
            return ai;
        }
        validUntil = validUntil.trim();
        String days = new Regex(validUntil, "([\\d]+) ?Tage?").getMatch(0);
        String hours = new Regex(validUntil, "([\\d]+) ?Stunde").getMatch(0);
        long res = 0;
        if (days != null) res += Long.parseLong(days.trim()) * 24 * 60 * 60 * 1000;
        if (hours != null) res += Long.parseLong(hours.trim()) * 60 * 60 * 1000;
        res += System.currentTimeMillis();
        ai.setValidUntil(res);
        return ai;
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 800;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        workAroundTimeOut(br);
        requestFileInformation(downloadLink);
        login(account);
        isExpired(account);
        boolean resume = true;
        int chunks = 0;
        if (downloadLink.getBooleanProperty("nochunk", false) == true) {
            resume = false;
            chunks = 1;
        }
        String url = null;
        workAroundTimeOut(br);
        br.openGetConnection(downloadLink.getDownloadURL());
        Request con;
        try {
            if (br.getRedirectLocation() == null) {
                Thread.sleep(1000);
                br.followConnection();
                checkPassword(downloadLink);
                handleErrors(downloadLink);
                if (br.containsHTML("We occurred an unexpected error")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 30 * 60 * 1000l);
                url = br.getRedirectLocation();
                if (url == null) url = br.getRegex("<a class=\"Orange_Link\" href=\"(.*?)\" >Alternativ klicke hier.<\\/a>").getMatch(0);
                if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, JDL.L("plugins.hoster.netloadin.errors.dlnotfound", "Download link not found"));

                con = br.createRequest(url);
                try {
                    /* remove next major update */
                    /* workaround for broken timeout in 0.9xx public */
                    con.setConnectTimeout(30000);
                    con.setReadTimeout(60000);
                } catch (Throwable e) {
                }
                /**
                 * TODO: Umbauen auf
                 * jd.plugins.BrowserAdapter.openDownload(br,...)
                 **/
                dl = RAFDownload.download(downloadLink, con, resume, chunks);
                // dl.headFake(null);
                dl.setFirstChunkRangeless(true);
                URLConnectionAdapter connection = dl.connect(br);
                for (int i = 0; i < 10 && (!connection.isOK()); i++) {
                    try {
                        con = br.createRequest(url);
                        try {
                            /* remove next major update */
                            /* workaround for broken timeout in 0.9xx public */
                            con.setConnectTimeout(30000);
                            con.setReadTimeout(60000);
                        } catch (Throwable e) {
                        }
                        dl = RAFDownload.download(downloadLink, con, resume, chunks);
                        connection = dl.connect(br);
                    } catch (Exception e) {
                        try {
                            Thread.sleep(150);
                        } catch (InterruptedException e2) {
                        }
                    }
                }
            } else {
                url = br.getRedirectLocation();
                con = br.createGetRequest(null);
                try {
                    /* remove next major update */
                    /* workaround for broken timeout in 0.9xx public */
                    con.setConnectTimeout(30000);
                    con.setReadTimeout(60000);
                } catch (Throwable e) {
                }
                dl = RAFDownload.download(downloadLink, con, resume, chunks);
                // dl.headFake(null);
                dl.setFirstChunkRangeless(true);
                dl.connect(br);
            }
            if (!dl.getConnection().isContentDisposition() && dl.getConnection().getResponseCode() != 206 && dl.getConnection().getResponseCode() != 416) {
                // Serverfehler
                if (br.followConnection() == null) throw new PluginException(LinkStatus.ERROR_RETRY, JDL.L("plugins.hoster.netloadin.errors.couldnotfollow", "Server: could not follow the link"));
                checkPassword(downloadLink);
                handleErrors(downloadLink);
            }
            if (!dl.startDownload()) {
                if (downloadLink.getLinkStatus().getErrorMessage() != null && downloadLink.getLinkStatus().getErrorMessage().startsWith(JDL.L("download.error.message.rangeheaderparseerror", "Unexpected rangeheader format:"))) {
                    logger.severe("Workaround for Netload Server-Problem! Setting Resume to false and Chunks to 1!");
                    downloadLink.setProperty("nochunk", true);
                    downloadLink.setChunksProgress(null);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } finally {
            logger.info("used serverurl: " + url);
        }
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        link.setProperty("nochunk", false);
    }

    @Override
    public String getAGBLink() {
        return AGB_LINK;
    }

    public AvailableStatus websiteFileCheck(DownloadLink downloadLink) throws PluginException {
        this.setBrowserExclusive();
        logger.info("FileCheckAPI error, try website check!");
        workAroundTimeOut(br);
        IOException ex = null;
        String id = Netloadin.getID(downloadLink.getDownloadURL());
        for (int i = 0; i < 5; i++) {
            try {
                Thread.sleep(500 + (i * 200));
            } catch (InterruptedException e) {
                return AvailableStatus.UNCHECKABLE;
            }
            ex = null;
            try {
                br.getPage("http://netload.in/index.php?id=10&file_id=" + id + "&lang=de");
                break;
            } catch (IOException e2) {
                ex = e2;
            }
        }
        if (ex != null) return AvailableStatus.UNCHECKABLE;
        String filename = br.getRegex("<div class=\"dl_first_filename\">(.*?)<").getMatch(0);
        String filesize = br.getRegex("<div class=\"dl_first_filename\">.*?style=.*?>.*?(\\d+.*?)<").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize.trim()));
        return AvailableStatus.TRUE;
    }

    @Override
    public boolean checkLinks(DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            Browser br = new Browser();
            workAroundTimeOut(br);
            br.setCookiesExclusive(true);
            StringBuilder sb = new StringBuilder();
            ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    /* we test 80 links at once */
                    if (index == urls.length || links.size() > 80) break;
                    links.add(urls[index]);
                    index++;
                }
                sb.delete(0, sb.capacity());
                sb.append("auth=BVm96BWDSoB4WkfbEhn42HgnjIe1ilMt&bz=1&md5=1&file_id=");
                int c = 0;
                for (DownloadLink dl : links) {
                    if (c > 0) sb.append(";");
                    sb.append(Netloadin.getID(dl.getDownloadURL()));
                    c++;
                }
                br.postPage("http://api.netload.in/info.php", sb.toString());
                String infos[][] = br.getRegex(Pattern.compile("(.*?);(.*?);(\\d+);(.*?);([0-9a-fA-F]+)")).getMatches();
                for (DownloadLink dl : links) {
                    String id = Netloadin.getID(dl.getDownloadURL());
                    int hit = -1;
                    for (int i = 0; i < infos.length; i++) {
                        if (infos[i][0].equalsIgnoreCase(id)) {
                            hit = i;
                            break;
                        }
                    }
                    if (hit == -1) {
                        /* id not in response, so its offline */
                        dl.setAvailable(false);
                    } else {
                        dl.setFinalFileName(infos[hit][1].trim());
                        dl.setDownloadSize(Regex.getSize(infos[hit][2]));
                        if (infos[hit][3].trim().equalsIgnoreCase("online")) {
                            dl.setAvailable(true);
                            dl.setMD5Hash(infos[hit][4].trim());
                        } else {
                            dl.setAvailable(false);
                        }
                    }
                }
                if (index == urls.length) break;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws PluginException {
        try {
            DownloadLink urls[] = new DownloadLink[1];
            urls[0] = downloadLink;
            checkLinks(urls);
            if (!downloadLink.isAvailabilityStatusChecked()) return AvailableStatus.UNCHECKED;
            if (downloadLink.isAvailable()) return AvailableStatus.TRUE;
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } catch (PluginException e) {
            /* workaround for buggy api */
            /* workaround for stable */
            DownloadLink tmpLink = new DownloadLink(null, "temp", "temp", "temp", false);
            LinkStatus linkState = new LinkStatus(tmpLink);
            e.fillLinkStatus(linkState);
            if (linkState.hasStatus(LinkStatus.ERROR_FILE_NOT_FOUND)) {
                return websiteFileCheck(downloadLink);
            } else {
                throw e;
            }
        }
    }

    @Override
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

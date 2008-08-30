//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

import jd.config.Configuration;
import jd.http.Browser;
import jd.http.HTTPConnection;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class Netloadin extends PluginForHost {
    static private final String AGB_LINK = "http://netload.in/index.php?id=12";

    static private final String CAPTCHA_WRONG = "Sicherheitsnummer nicht eingegeben";

    static private final String CODER = "JD-Team";
    static private final String DOWNLOAD_CAPTCHA = "download_captcha.tpl";
    static private final String DOWNLOAD_LIMIT = "download_limit.tpl";
    static private final String DOWNLOAD_START = "download_load.tpl";
    private String LINK_PASS = null;

    static private final Pattern DOWNLOAD_WAIT_TIME = Pattern.compile("countdown\\(([0-9]*),'change", Pattern.CASE_INSENSITIVE);

    static private final String FILE_DAMAGED = "(Die Datei wurde Opfer einer defekten Festplatte|Diese Datei liegt auf einem Server mit einem technischen Defekt. Wir konnten diese Datei leider nicht wieder herstellen)";

    static private final String FILE_NOT_FOUND = "Die Datei konnte leider nicht gefunden werden";
    static private final String HOST = "netload.in";

    static private final String LIMIT_REACHED = "share/images/download_limit_go_on.gif";
    static private final String NEW_HOST_URL = "<a class=\"Orange_Link\" href=\"(.*?)\" >Alternativ klicke hier\\.<\\/a>";
    static private final Pattern PAT_SUPPORTED = Pattern.compile("sjdp://[\\w\\.]*?netload\\.in.*|(http://[\\w\\.]*?netload\\.in/(?!index\\.php).*)", Pattern.CASE_INSENSITIVE);

    private static String getID(String link) {

        return new Regex(link, "\\/datei([a-zA-Z0-9]+)").getMatch(0);

    }

    private String fileStatusText;

    public Netloadin() {
        setConfigElements();
        this.enablePremium();
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        if (downloadLink.getDownloadURL().matches("sjdp://.*")) {
            new Serienjunkies().handleFree(downloadLink);
            return;
        }
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        downloadLink.setUrlDownload("http://netload.in/datei" + Netloadin.getID(downloadLink.getDownloadURL()) + ".htm");

        br.setCookiesExclusive(true);
        br.clearCookies(HOST);

        br.getPage(downloadLink.getDownloadURL());
        checkPassword(downloadLink, linkStatus);
        if (linkStatus.isFailed()) return;
        String url = br.getRegex("<div class=\"Free_dl\"><a href=\"(.*?)\">").getMatch(0);
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
        captchaPost.action = "index.php?id=10";
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
        if (!Browser.download(file, c.openGetConnection(captchaURL)) || !file.exists()) {
            logger.severe("Captcha download failed: " + captchaURL);

            linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
            return;
        }
        captchaPost.getVars().put("captcha_check", this.getCaptchaCode(file, downloadLink));
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

        if (br.containsHTML(LIMIT_REACHED) || br.containsHTML(DOWNLOAD_LIMIT)) {

            linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);

            long waitTime = Long.parseLong(new Regex(br.getRequest().getHtmlCode(), DOWNLOAD_WAIT_TIME).getMatch(0));
            waitTime = waitTime * 10L;

            linkStatus.setValue(waitTime);
            return;
        }
        if (br.containsHTML(CAPTCHA_WRONG)) {

            linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
            return;
        }
        String finalURL = br.getRegex(NEW_HOST_URL).getMatch(0);

        sleep(20000, downloadLink);

        dl = new RAFDownload(this, downloadLink, br.openGetConnection(finalURL));
        dl.startDownload();

    }

    private void checkPassword(DownloadLink downloadLink, LinkStatus linkStatus) throws IOException {
        if (!br.containsHTML("download_password")) return;
        String pass = downloadLink.getStringProperty("LINK_PASSWORD", LINK_PASS);

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
            pw.put("password", pass = JDUtilities.getGUI().showUserInputDialog(String.format(JDLocale.L("plugins.netload.downloadPassword_question", "Password protected. Enter Password for %s"), downloadLink.getName())));
            br.submitForm(pw);
        }
        // falls falsch abbruch
        if (br.containsHTML("download_password")) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setValue(20 * 60 * 1000l);
            linkStatus.setErrorMessage(JDLocale.L("plugins.netload.downloadPassword_wrong", "Linkpassword is wrong"));
            return;
        }
        // richtiges pw... wird gesoeichert
        if (pass != null) {
            downloadLink.setProperty("LINK_PASSWORD", pass);
            LINK_PASS = pass;
        }

    }

    public AccountInfo getAccountInformation(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        Browser br = new Browser();

        br.postPage("http://" + HOST + "/index.php", "txtuser=" + account.getUser() + "&txtpass=" + account.getPass() + "&txtcheck=login&txtlogin=");
        if (br.getRedirectLocation() == null) {
            ai.setValid(false);
            return ai;
        }
        br.getPage("http://netload.in/index.php?id=2");

        // String login =
        // ri.getRegexp("<td>Login:</td><td.*?><b>(.*?)</b></td>").getFirstMatch(
        // 1).trim();
        String validUntil = br.getRegex("Verbleibender Zeitraum</div>.*?<div style=.*?><span style=.*?>(.*?)</span></div>").getMatch(0).trim();

        String days = new Regex(validUntil, "([\\d]+) ?Tage").getMatch(0);
        String hours = new Regex(validUntil, "([\\d]+) ?Stunde").getMatch(0);
        long res = 0;
        if (days != null) res += Long.parseLong(days.trim()) * 24 * 60 * 60 * 1000;
        if (hours != null) res += Long.parseLong(hours.trim()) * 60 * 60 * 1000;
        res += new Date().getTime();

        logger.info(new Date(res) + "");
        ai.setValidUntil(res);

        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        if (downloadLink.getDownloadURL().matches("sjdp://.*")) {
            new Serienjunkies().handleFree(downloadLink);
            return;
        }
        String user = account.getUser();
        String pass = account.getPass();
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        downloadLink.setUrlDownload("http://netload.in/datei" + Netloadin.getID(downloadLink.getDownloadURL()) + ".htm");

        br.setFollowRedirects(false);
        br.getPage("http://" + HOST);
        br.postPage("http://" + HOST + "/index.php", "txtuser=" + user + "&txtpass=" + pass + "&txtcheck=login&txtlogin=");
        if (br.getRedirectLocation() == null) {
            linkStatus.addStatus(LinkStatus.ERROR_PREMIUM);

            return;
        }

        br.getPage(downloadLink.getDownloadURL());
        HTTPConnection con;
        if (br.getRedirectLocation() == null) {

            checkPassword(downloadLink, linkStatus);
            if (linkStatus.isFailed()) return;
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

            String url = br.getRedirectLocation();
            if (url == null) url = br.getRegex("<a class=\"Orange_Link\" href=\"(.*?)\" >Alternativ klicke hier.<\\/a>").getMatch(0);
            if (url == null) {
                logger.severe("Download link not found");

                linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFEKT);
                return;
            }

            con = br.openGetConnection(url);
            for (int i = 0; i < 10 && (!con.isOK()); i++) {
                try {
                    con = br.openGetConnection(url);

                } catch (Exception e) {
                    try {
                        Thread.sleep(150);
                    } catch (InterruptedException e2) {
                        e2.printStackTrace();
                    }
                }
            }

        } else {
            con = br.openGetConnection(null);
        }

        sleep(100, downloadLink);

        dl = new RAFDownload(this, downloadLink, con);
        dl.setResume(true);
        dl.setChunkNum(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2));

        dl.setLoadPreBytes(1);
        dl.startDownload();

    }

    @Override
    public String getAGBLink() {
        return AGB_LINK;
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        if (downloadLink.getDownloadURL().matches("sjdp://.*")) return true;
        try {
            LinkStatus linkStatus = downloadLink.getLinkStatus();

            br.setCookiesExclusive(true);
            br.clearCookies(HOST);

            br.setConnectTimeout(15000);
            String id = Netloadin.getID(downloadLink.getDownloadURL());
            String page = br.getPage("http://netload.in/share/fileinfos2.php?file_id=" + id);
            for (int i = 0; i < 3 && page == null; i++) {
                try {
                    Thread.sleep(150);
                } catch (InterruptedException e) {

                    e.printStackTrace();
                }
                page = br.getPage("http://netload.in/share/fileinfos2.php?file_id=" + id);

            }

            if (page == null) { return false; }

            if (Regex.matches(page, "unknown file_data")) {
                linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
                return false;
            }

            String[] entries = Regex.getLines(page);

            if (entries.length < 3) { return false; }

            downloadLink.setName(entries[0]);
            fileStatusText = entries[2];
            downloadLink.setDownloadSize((int) Regex.getSize(entries[1]));

            if (entries[2].equalsIgnoreCase("online")) { return true; }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;

    }

    @Override
    public String getFileInformationString(DownloadLink downloadLink) {
        if (downloadLink.getDownloadURL().matches("sjdp://.*")) return "";
        return downloadLink.getName() + " (" + fileStatusText + ")";
    }

    @Override
    public String getHost() {
        return HOST;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jd.plugins.PluginForHost#reset()
     */

    @Override
    public String getPluginName() {
        return HOST;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jd.plugins.PluginForHost#checkAvailability(jd.plugins.DownloadLink)
     */

    @Override
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
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

    private void setConfigElements() {

    }
}